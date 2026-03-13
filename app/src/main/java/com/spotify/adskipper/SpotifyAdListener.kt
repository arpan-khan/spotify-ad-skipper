package com.spotify.adskipper

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Background NotificationListenerService that monitors Spotify notifications
 * and triggers the ad skip sequence when advertisements are detected.
 * 
 * This service passively listens for Spotify notifications, detects advertisements
 * by checking the notification title, and executes an asynchronous sequence to
 * force-stop, relaunch, and skip to the next track.
 */
class SpotifyAdListener : NotificationListenerService() {
    
    private companion object {
        const val TAG = "SpotifyAdListener"
        const val SPOTIFY_PACKAGE = "com.spotify.music"
        const val AD_TITLE_KEYWORD = "Advertisement"
    }
    
    // Coroutine scope with SupervisorJob to prevent child failures from cancelling the scope
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Filter for Spotify notifications only (most efficient check first)
        if (sbn.packageName != SPOTIFY_PACKAGE) return
        
        // Extract notification
        val notification = sbn.notification ?: return
        
        // DEBUG: Log all notification details
        val title = notification.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = notification.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val ticker = notification.tickerText?.toString()
        Log.d(TAG, "Spotify notification - Title: '$title', Text: '$text', Ticker: '$ticker'")
        
        // Check if this is an advertisement
        if (isAdvertisement(notification)) {
            Log.d(TAG, "Advertisement detected, executing skip sequence")
            
            // Execute ad skip sequence asynchronously (DO NOT block main thread)
            scope.launch {
                executeAdSkipSequence()
            }
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Optional: Handle notification removal if needed in future
    }
    
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
    }
    
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "Notification listener disconnected")
    }
    
    /**
     * Checks if a notification is an advertisement.
     * 
     * Checks multiple fields to ensure no ads slip through:
     * - Ticker text (most reliable indicator)
     * - Title field
     * - Text/description field
     * - Sub-text field
     * 
     * @param notification The notification to check
     * @return true if the notification is an advertisement, false otherwise
     */
    private fun isAdvertisement(notification: Notification): Boolean {
        val extras = notification.extras ?: return false
        
        // Check ticker text (most reliable)
        val ticker = notification.tickerText?.toString()
        if (ticker?.contains(AD_TITLE_KEYWORD, ignoreCase = true) == true) {
            return true
        }
        
        // Check title
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        if (title?.contains(AD_TITLE_KEYWORD, ignoreCase = true) == true) {
            return true
        }
        
        // Check text/description
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        if (text?.contains(AD_TITLE_KEYWORD, ignoreCase = true) == true) {
            return true
        }
        
        // Check sub-text
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        if (subText?.contains(AD_TITLE_KEYWORD, ignoreCase = true) == true) {
            return true
        }
        
        // Check info text
        val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()
        if (infoText?.contains(AD_TITLE_KEYWORD, ignoreCase = true) == true) {
            return true
        }
        
        return false
    }
    
    /**
     * Executes the ad skip sequence asynchronously.
     * 
     * Sequence:
     * 1. Force-stop Spotify via Shizuku
     * 2. Wait 1000ms for process termination
     * 3. Relaunch Spotify
     * 4. Wait 2000ms for Spotify to initialize
     * 5. Send skip to next track intent
     * 
     * The sequence terminates early if force-stop or relaunch fails.
     * Skip failures are logged but don't prevent sequence completion.
     */
    private suspend fun executeAdSkipSequence() {
        // Step 1: Force stop Spotify
        when (val result = ShizukuController.forceStopSpotify()) {
            is Result.Error -> {
                Log.e(TAG, "Force stop failed", result.exception)
                return // Terminate sequence early
            }
            is Result.Success -> Log.d(TAG, "Spotify stopped")
        }
        
        // Step 2: Wait for process termination to complete
        delay(1000)
        
        // Step 3: Relaunch Spotify
        when (val result = SpotifyController.relaunchSpotify(applicationContext)) {
            is Result.Error -> {
                Log.e(TAG, "Relaunch failed", result.exception)
                return // Terminate sequence early
            }
            is Result.Success -> Log.d(TAG, "Spotify relaunched")
        }
        
        // Step 4: Wait for Spotify to initialize and load state
        delay(2000)
        
        // Step 5: Send skip to next track intent
        when (val result = SpotifyController.skipToNext(applicationContext)) {
            is Result.Error -> Log.e(TAG, "Skip failed", result.exception)
            is Result.Success -> Log.d(TAG, "Track skipped successfully")
        }
        
        // Sequence complete - service remains active for next advertisement
    }
    
    override fun onDestroy() {
        // Cancel all pending coroutines to prevent resource leaks
        scope.cancel()
        super.onDestroy()
        Log.d(TAG, "Service destroyed, coroutines cancelled")
    }
}
