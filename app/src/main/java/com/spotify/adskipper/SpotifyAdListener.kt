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
 * force-stop and relaunch Spotify. Playback resumes naturally after relaunch.
 */
class SpotifyAdListener : NotificationListenerService() {
    
    private companion object {
        const val TAG = "SpotifyAdListener"
        const val SPOTIFY_PACKAGE = "com.spotify.music"
        const val AD_TITLE_KEYWORD = "Advertisement"
    }
    
    // Coroutine scope with SupervisorJob to prevent child failures from cancelling the scope
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Flag to prevent concurrent ad skip sequences
    @Volatile
    private var isAdSkipInProgress = false
    
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
            // Prevent concurrent ad skip sequences
            if (isAdSkipInProgress) {
                Log.d(TAG, "Ad skip already in progress, skipping duplicate")
                return
            }
            
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
     * 1. Wait for Spotify to update queue state (ad must start playing first)
     * 2. Send Spotify to background (triggers onPause)
     * 3. Wait for lifecycle callbacks (onPause → onStop)
     * 4. Finish and remove task (emulates swipe to close, triggers onDestroy)
     * 5. Wait for process termination
     * 6. Relaunch Spotify
     * 7. Wait for Spotify to initialize
     * 8. Send play intent
     * 
     * The sequence terminates early if finish or relaunch fails.
     */
    private suspend fun executeAdSkipSequence() {
        isAdSkipInProgress = true
        
        try {
            // Step 1: Wait for Spotify to update queue state after ad starts
            // This delay is critical - without it, Spotify resumes at the wrong track
            Log.d(TAG, "Waiting for Spotify to update queue state...")
            delay(2500)
            
            // Step 2: Send Spotify to background (triggers onPause)
            Log.d(TAG, "Sending Spotify to background...")
            val backgroundIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_HOME)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(backgroundIntent)
            
            // Step 3: Wait for lifecycle callbacks (onPause → onStop)
            delay(1000)
            
            // Step 4: Finish and remove task (emulates swipe to close)
            when (val result = ShizukuController.finishAndRemoveSpotifyTask(applicationContext)) {
                is Result.Error -> {
                    Log.e(TAG, "Finish and remove task failed", result.exception)
                    return
                }
                is Result.Success -> Log.d(TAG, "Spotify task finished and removed")
            }
            
            // Step 5: Wait for process termination
            delay(1000)
            
            // Step 6: Relaunch Spotify
            when (val result = SpotifyController.relaunchSpotify(applicationContext)) {
                is Result.Error -> {
                    Log.e(TAG, "Relaunch failed", result.exception)
                    return
                }
                is Result.Success -> Log.d(TAG, "Spotify relaunched")
            }
            
            // Step 7: Wait for Spotify to initialize
            delay(3000)
            
            // Step 8: Send play intent
            Log.d(TAG, "Sending play intent...")
            when (val result = SpotifyController.play(applicationContext)) {
                is Result.Error -> Log.e(TAG, "Play intent failed", result.exception)
                is Result.Success -> Log.d(TAG, "Play intent sent")
            }
            
            Log.d(TAG, "Ad skip sequence complete")
        } finally {
            isAdSkipInProgress = false
            Log.d(TAG, "Ad skip flag reset, ready for next ad")
        }
    }
    
    override fun onDestroy() {
        // Cancel all pending coroutines to prevent resource leaks
        scope.cancel()
        super.onDestroy()
        Log.d(TAG, "Service destroyed, coroutines cancelled")
    }
}
