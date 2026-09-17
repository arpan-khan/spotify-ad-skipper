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

class SpotifyAdListener : NotificationListenerService() {

    private companion object {
        const val TAG = "SpotifyAdListener"
        const val SPOTIFY_PACKAGE = "com.spotify.music"
        const val AD_TITLE_KEYWORD = "Advertisement"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var isAdSkipInProgress = false

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        if (sbn.packageName != SPOTIFY_PACKAGE) return

        if (!AdSkipPreferences.isServiceEnabled(applicationContext)) return

        val notification = sbn.notification ?: return

        val title = notification.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = notification.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val ticker = notification.tickerText?.toString()
        Log.d(TAG, "Spotify notification - Title: '$title', Text: '$text', Ticker: '$ticker'")

        if (isAdvertisement(notification)) {

            if (isAdSkipInProgress) {
                Log.d(TAG, "Ad skip already in progress, skipping duplicate")
                return
            }

            Log.d(TAG, "Advertisement detected, executing skip sequence")

            scope.launch {
                executeAdSkipSequence()
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {

    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "Notification listener disconnected")
    }

    private fun isAdvertisement(notification: Notification): Boolean {
        val extras = notification.extras ?: return false

        val ticker = notification.tickerText?.toString()
        if (ticker?.contains(AD_TITLE_KEYWORD, ignoreCase = true) == true) {
            return true
        }

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        if (title?.contains(AD_TITLE_KEYWORD, ignoreCase = true) == true) {
            return true
        }

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        if (text?.contains(AD_TITLE_KEYWORD, ignoreCase = true) == true) {
            return true
        }

        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        if (subText?.contains(AD_TITLE_KEYWORD, ignoreCase = true) == true) {
            return true
        }

        val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()
        if (infoText?.contains(AD_TITLE_KEYWORD, ignoreCase = true) == true) {
            return true
        }

        return false
    }

    private suspend fun executeAdSkipSequence() {
        isAdSkipInProgress = true

        try {

            val foregroundTaskId = when (val result = ShizukuController.getForegroundTaskId()) {
                is Result.Success -> result.value
                is Result.Error -> {
                    Log.w(TAG, "Could not capture foreground task, focus will not be restored", result.exception)
                    null
                }
            }

            Log.d(TAG, "Waiting for Spotify to update queue state...")
            delay(3500)

            when (val result = ShizukuController.finishAndRemoveSpotifyTask(applicationContext)) {
                is Result.Error -> {
                    Log.e(TAG, "Finish and remove task failed", result.exception)
                    return
                }
                is Result.Success -> Log.d(TAG, "Spotify task finished and removed")
            }

            delay(1000)

            when (val result = SpotifyController.relaunchSpotify(applicationContext)) {
                is Result.Error -> {
                    Log.e(TAG, "Relaunch failed", result.exception)
                    return
                }
                is Result.Success -> Log.d(TAG, "Spotify relaunched")
            }

            delay(3000)

            Log.d(TAG, "Sending play intent...")
            when (val result = SpotifyController.play(applicationContext)) {
                is Result.Error -> Log.e(TAG, "Play intent failed", result.exception)
                is Result.Success -> Log.d(TAG, "Play intent sent")
            }

            if (foregroundTaskId != null) {
                when (val result = ShizukuController.restoreForegroundTask(foregroundTaskId)) {
                    is Result.Error -> Log.w(TAG, "Could not restore previous foreground app", result.exception)
                    is Result.Success -> Log.d(TAG, "Restored previous foreground app")
                }
            }

            Log.d(TAG, "Ad skip sequence complete")
        } finally {
            isAdSkipInProgress = false
            Log.d(TAG, "Ad skip flag reset, ready for next ad")
        }
    }

    override fun onDestroy() {

        scope.cancel()
        super.onDestroy()
        Log.d(TAG, "Service destroyed, coroutines cancelled")
    }
}
