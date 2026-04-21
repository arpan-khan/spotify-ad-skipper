package com.spotify.adskipper

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/**
 * Singleton for privileged process management operations.
 * 
 * This controller provides:
 * 1. Activity launch via Shizuku (bypasses Android 15 background restrictions)
 * 2. App close via finishAndRemoveTask() (emulates swipe to close)
 * 3. Force stop as fallback (via Shizuku reflection)
 * 
 * Implementation follows Shizuku best practices:
 * - Uses ShizukuBinderWrapper for binder forwarding
 * - Calls IActivityManager methods via reflection
 * - No custom android.jar required (uses reflection)
 */
object ShizukuController {
    
    private const val TAG = "ShizukuController"
    private const val SPOTIFY_PACKAGE = "com.spotify.music"
    
    /**
     * Checks if Shizuku service is running and accessible.
     * 
     * @return true if Shizuku binder is reachable, false otherwise
     */
    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Shizuku not available", e)
            false
        }
    }
    
    /**
     * Checks if Shizuku permission is granted.
     * 
     * @return true if API_V23 permission is granted, false otherwise
     */
    fun checkShizukuPermission(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check Shizuku permission", e)
            false
        }
    }
    
    /**
     * Closes Spotify by finishing all its activities and removing from recent apps.
     * 
     * This emulates the manual "swipe to close" behavior from recent apps by using
     * ActivityManager.getAppTasks() and finishAndRemoveTask(). This triggers the
     * normal app lifecycle: onPause → onStop → onDestroy, giving Spotify time to
     * save its queue state before termination.
     * 
     * @param context Android context for accessing ActivityManager
     * @return Result.Success if task finished, Result.Error if failed
     */
    fun finishAndRemoveSpotifyTask(context: Context): Result<Unit> {
        // Precondition checks
        if (!isShizukuAvailable()) {
            return Result.Error(IllegalStateException("Shizuku service not available"))
        }
        
        if (!checkShizukuPermission()) {
            return Result.Error(SecurityException("Shizuku permission not granted"))
        }
        
        return try {
            // Get ActivityManager
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            
            // Get all app tasks
            val appTasks = activityManager.appTasks
            
            // Find Spotify's task
            var spotifyTaskFound = false
            for (appTask in appTasks) {
                val taskInfo = appTask.taskInfo
                if (taskInfo.baseActivity?.packageName == SPOTIFY_PACKAGE) {
                    Log.d(TAG, "Found Spotify task, calling finishAndRemoveTask()")
                    appTask.finishAndRemoveTask()
                    spotifyTaskFound = true
                    break
                }
            }
            
            if (spotifyTaskFound) {
                Log.d(TAG, "Successfully finished Spotify task via finishAndRemoveTask()")
                Result.Success(Unit)
            } else {
                Log.w(TAG, "No Spotify task found in app tasks, falling back to forceStop")
                forceStopSpotify()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during finishAndRemoveTask", e)
            Result.Error(e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during finishAndRemoveTask", e)
            Result.Error(e)
        }
    }
    
    /**
     * Forces Spotify to stop using Shizuku API via IActivityManager.
     * 
     * Uses Shizuku's Remote Binder Call approach with reflection to directly invoke
     * IActivityManager.forceStopPackage() with elevated privileges.
     * 
     * Note: This method kills the process instantly WITHOUT lifecycle callbacks.
     * Use finishAndRemoveSpotifyTask() instead to preserve Spotify's queue state.
     * 
     * @return Result.Success if process terminated, Result.Error if failed
     */
    fun forceStopSpotify(): Result<Unit> {
        // Precondition checks
        if (!isShizukuAvailable()) {
            return Result.Error(IllegalStateException("Shizuku service not available"))
        }
        
        if (!checkShizukuPermission()) {
            return Result.Error(SecurityException("Shizuku permission not granted"))
        }
        
        return try {
            // Bypass hidden API restrictions (Android 15+)
            HiddenApiBypass.addHiddenApiExemptions("Landroid/app/IActivityManager")
            
            // Get IActivityManager via Shizuku binder wrapper using reflection
            val activityService: IBinder = SystemServiceHelper.getSystemService("activity")
            val wrappedBinder = ShizukuBinderWrapper(activityService)
            
            // Use reflection to access IActivityManager.Stub.asInterface()
            val stubClass = Class.forName("android.app.IActivityManager\$Stub")
            val asInterfaceMethod = stubClass.getDeclaredMethod("asInterface", IBinder::class.java)
            asInterfaceMethod.isAccessible = true
            val activityManager = asInterfaceMethod.invoke(null, wrappedBinder)
            
            // Use reflection to call forceStopPackage(String, int)
            val forceStopMethod = activityManager!!.javaClass.getDeclaredMethod(
                "forceStopPackage",
                String::class.java,
                Int::class.javaPrimitiveType
            )
            forceStopMethod.isAccessible = true
            
            // Get current user ID (typically 0)
            val userHandleClass = Class.forName("android.os.UserHandle")
            val myUserIdMethod = userHandleClass.getDeclaredMethod("myUserId")
            myUserIdMethod.isAccessible = true
            val userId = myUserIdMethod.invoke(null) as Int
            
            // Invoke forceStopPackage
            forceStopMethod.invoke(activityManager, SPOTIFY_PACKAGE, userId)
            
            Log.d(TAG, "Successfully force-stopped Spotify via IActivityManager (reflection)")
            Result.Success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during force stop", e)
            Result.Error(e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during force stop", e)
            Result.Error(e)
        }
    }
    
    /**
     * Launches an activity using Shizuku API via IActivityManager.
     * 
     * Uses Shizuku's Remote Binder Call approach with reflection to directly invoke
     * IActivityManager.startActivity() with elevated privileges, bypassing Android 15's
     * background activity launch restrictions.
     * 
     * This method allows launching activities from background services (like NotificationListenerService)
     * which would normally be blocked by Android 15's security restrictions.
     * 
     * @param packageName The package name of the app to launch
     * @param context Android context for intent resolution
     * @return Result.Success if activity launched, Result.Error if failed
     */
    fun launchActivity(packageName: String, context: Context): Result<Unit> {
        // Precondition checks
        if (!isShizukuAvailable()) {
            return Result.Error(IllegalStateException("Shizuku service not available"))
        }
        
        if (!checkShizukuPermission()) {
            return Result.Error(SecurityException("Shizuku permission not granted"))
        }
        
        // Get launch intent for the package
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return Result.Error(IllegalStateException("Package $packageName not installed or has no launch intent"))
        
        // Add FLAG_ACTIVITY_NEW_TASK (required for launching from non-activity context)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        return try {
            // Bypass hidden API restrictions (Android 15+)
            HiddenApiBypass.addHiddenApiExemptions("Landroid/app/IActivityManager")
            HiddenApiBypass.addHiddenApiExemptions("Landroid/content/IIntentReceiver")
            HiddenApiBypass.addHiddenApiExemptions("Landroid/os/UserHandle")
            
            // Get IActivityManager via Shizuku binder wrapper using reflection
            val activityService: IBinder = SystemServiceHelper.getSystemService("activity")
            val wrappedBinder = ShizukuBinderWrapper(activityService)
            
            // Use reflection to access IActivityManager.Stub.asInterface()
            val stubClass = Class.forName("android.app.IActivityManager\$Stub")
            val asInterfaceMethod = stubClass.getDeclaredMethod("asInterface", IBinder::class.java)
            asInterfaceMethod.isAccessible = true
            val activityManager = asInterfaceMethod.invoke(null, wrappedBinder)
            
            // Get current user ID (typically 0)
            val userHandleClass = Class.forName("android.os.UserHandle")
            val myUserIdMethod = userHandleClass.getDeclaredMethod("myUserId")
            myUserIdMethod.isAccessible = true
            val userId = myUserIdMethod.invoke(null) as Int
            
            // Use reflection to call startActivity
            // IActivityManager.startActivity signature (simplified for Android 15):
            // int startActivity(IApplicationThread caller, String callingPackage, Intent intent,
            //                   String resolvedType, IBinder resultTo, String resultWho, int requestCode,
            //                   int flags, ProfilerInfo profilerInfo, Bundle options)
            //
            // We use a simpler overload that exists in Android framework:
            // int startActivityAsUser(IApplicationThread caller, String callingPackage, Intent intent,
            //                         String resolvedType, IBinder resultTo, String resultWho, int requestCode,
            //                         int startFlags, ProfilerInfo profilerInfo, Bundle options, int userId)
            
            val startActivityMethod = activityManager!!.javaClass.getDeclaredMethod(
                "startActivityAsUser",
                Class.forName("android.app.IApplicationThread"), // caller (null for system)
                String::class.java,                              // callingPackage
                Intent::class.java,                              // intent
                String::class.java,                              // resolvedType (null)
                IBinder::class.java,                             // resultTo (null)
                String::class.java,                              // resultWho (null)
                Int::class.javaPrimitiveType,                    // requestCode (0)
                Int::class.javaPrimitiveType,                    // startFlags (0)
                Class.forName("android.app.ProfilerInfo"),       // profilerInfo (null)
                android.os.Bundle::class.java,                   // options (null)
                Int::class.javaPrimitiveType                     // userId
            )
            startActivityMethod.isAccessible = true
            
            // Invoke startActivityAsUser with elevated privileges
            // Use "com.android.shell" as callingPackage since we're calling through Shizuku (uid=2000)
            val result = startActivityMethod.invoke(
                activityManager,
                null,                    // caller (null = system)
                "com.android.shell",     // callingPackage (must match Shizuku's uid=2000)
                intent,                  // intent
                null,                    // resolvedType
                null,                    // resultTo
                null,                    // resultWho
                0,                       // requestCode
                0,                       // startFlags
                null,                    // profilerInfo
                null,                    // options
                userId                   // userId
            ) as Int
            
            // Check result code (0 = success, negative = error)
            if (result >= 0) {
                Log.d(TAG, "Successfully launched $packageName via IActivityManager (reflection)")
                Result.Success(Unit)
            } else {
                Log.e(TAG, "Failed to launch $packageName, result code: $result")
                Result.Error(IllegalStateException("startActivityAsUser returned error code: $result"))
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during activity launch", e)
            Result.Error(e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during activity launch", e)
            Result.Error(e)
        }
    }
}
