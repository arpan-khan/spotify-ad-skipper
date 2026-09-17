package com.spotify.adskipper

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

object SpotifyController {

    private const val TAG = "SpotifyController"
    private const val SPOTIFY_PACKAGE = "com.spotify.music"

    fun isSpotifyInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SPOTIFY_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun relaunchSpotify(context: Context): Result<Unit> {
        return try {

            when (val result = ShizukuController.launchActivity(SPOTIFY_PACKAGE, context)) {
                is Result.Success -> {
                    Log.d(TAG, "Spotify relaunched via Shizuku")
                    Result.Success(Unit)
                }
                is Result.Error -> {

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

    fun play(context: Context): Result<Unit> {
        return try {

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
