package com.spotify.adskipper

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

/**
 * Singleton for managing Spotify application lifecycle through Android intents.
 * Provides methods to relaunch Spotify and send media control commands.
 * 
 * This controller handles intent-based communication with the Spotify app,
 * including launching the app and broadcasting media control intents.
 */
object SpotifyController {
    
    private const val TAG = "SpotifyController"
    private const val SPOTIFY_PACKAGE = "com.spotify.music"
    private const val SPOTIFY_NEXT_ACTION = "com.spotify.mobile.android.ui.widget.NEXT"
    
    /**
     * Checks if Spotify is installed on the device.
     * 
     * @param context Android context for package manager access
     * @return true if Spotify is installed, false otherwise
     */
    fun isSpotifyInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SPOTIFY_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Relaunches the Spotify application.
     * 
     * Uses Shizuku-based activity launch to bypass Android 15 background activity launch restrictions.
     * Falls back to standard startActivity() if Shizuku is unavailable (may fail on Android 15).
     * 
     * @param context Android context for intent operations
     * @return Result.Success if launched, Result.Error if failed
     */
    fun relaunchSpotify(context: Context): Result<Unit> {
        return try {
            // Try Shizuku-based launch first (bypasses Android 15 restrictions)
            when (val result = ShizukuController.launchActivity(SPOTIFY_PACKAGE, context)) {
                is Result.Success -> {
                    Log.d(TAG, "Spotify relaunched via Shizuku")
                    Result.Success(Unit)
                }
                is Result.Error -> {
                    // Fall back to standard launch (may fail on Android 15 from background)
                    Log.w(TAG, "Shizuku launch failed, trying standard launch", result.exception)
                    
                    val intent = context.packageManager.getLaunchIntentForPackage(SPOTIFY_PACKAGE)
                        ?: return Result.Error(IllegalStateException("Spotify not installed"))
                    
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    Log.d(TAG, "Spotify relaunched via standard launch")
                    Result.Success(Unit)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to relaunch Spotify", e)
            Result.Error(e)
        }
    }
    
    /**
     * Sends skip to next track intent to Spotify.
     * 
     * Broadcasts the NEXT action intent scoped to the Spotify package to skip
     * to the next track in the playback queue.
     * 
     * @param context Android context for broadcast operations
     * @return Result.Success if sent, Result.Error if failed
     */
    fun skipToNext(context: Context): Result<Unit> {
        return try {
            val intent = Intent(SPOTIFY_NEXT_ACTION).apply {
                setPackage(SPOTIFY_PACKAGE)
            }
            context.sendBroadcast(intent)
            Log.d(TAG, "Skip intent sent successfully")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send skip intent", e)
            Result.Error(e)
        }
    }
}
