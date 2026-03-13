package com.spotify.adskipper

import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/**
 * Singleton wrapper around the Shizuku API for privileged process management operations.
 * Uses Shizuku's Remote Binder Call approach with reflection to access IActivityManager APIs.
 * 
 * This controller provides reliable, invisible, and instant process termination using
 * Shizuku's privileged API access via ShizukuBinderWrapper and reflection to access
 * hidden Android APIs, avoiding the limitations of Accessibility Services and deprecated
 * shell command approaches.
 * 
 * Implementation follows Shizuku best practices:
 * - Uses ShizukuBinderWrapper for binder forwarding
 * - Calls IActivityManager.forceStopPackage() via reflection
 * - No reliance on deprecated newProcess() method
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
     * Forces Spotify to stop using Shizuku API via IActivityManager.
     * 
     * Uses Shizuku's Remote Binder Call approach with reflection to directly invoke
     * IActivityManager.forceStopPackage() with elevated privileges.
     * This is more reliable and faster than shell command execution.
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
            // Get IActivityManager via Shizuku binder wrapper using reflection
            val activityService: IBinder = SystemServiceHelper.getSystemService("activity")
            val wrappedBinder = ShizukuBinderWrapper(activityService)
            
            // Use reflection to access IActivityManager.Stub.asInterface()
            val iActivityManagerStub = Class.forName("android.app.IActivityManager\$Stub")
            val asInterfaceMethod = iActivityManagerStub.getMethod("asInterface", IBinder::class.java)
            val activityManager = asInterfaceMethod.invoke(null, wrappedBinder)
            
            // Use reflection to call forceStopPackage(String, int)
            val forceStopMethod = activityManager!!.javaClass.getMethod(
                "forceStopPackage",
                String::class.java,
                Int::class.javaPrimitiveType
            )
            
            // Get current user ID (typically 0)
            val userHandleClass = Class.forName("android.os.UserHandle")
            val myUserIdMethod = userHandleClass.getMethod("myUserId")
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
}
