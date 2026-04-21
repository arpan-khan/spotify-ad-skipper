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
     * Sends play intent to Spotify using media button simulation.
     * 
     * Uses ACTION_MEDIA_BUTTON to simulate pressing a headset play button,
     * which is the most reliable way to trigger playback on Android 15.
     * 
     * @param context Android context for broadcast operations
     * @return Result.Success if sent, Result.Error if failed
     */
    fun play(context: Context): Result<Unit> {
        return try {
            // Send media button event (simulates headset play button)
            val keyDownIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent(
                    android.view.KeyEvent.ACTION_DOWN,
                    android.view.KeyEvent.KEYCODE_MEDIA_PLAY
                ))
                setPackage(SPOTIFY_PACKAGE)
            }
            context.sendBroadcast(keyDownIntent)
            
            val keyUpIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent(
                    android.view.KeyEvent.ACTION_UP,
                    android.view.KeyEvent.KEYCODE_MEDIA_PLAY
                ))
                setPackage(SPOTIFY_PACKAGE)
            }
            context.sendBroadcast(keyUpIntent)
            
            Log.d(TAG, "Media button play intent sent")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send play intent", e)
            Result.Error(e)
        }
    }
}
