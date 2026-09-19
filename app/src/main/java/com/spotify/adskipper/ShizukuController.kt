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

object ShizukuController {

    private const val TAG = "ShizukuController"
    private const val SPOTIFY_PACKAGE = "com.spotify.music"

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Shizuku not available", e)
            false
        }
    }

    fun checkShizukuPermission(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check Shizuku permission", e)
            false
        }
    }

    fun finishAndRemoveSpotifyTask(context: Context): Result<Unit> {

        if (!isShizukuAvailable()) {
            return Result.Error(IllegalStateException("Shizuku service not available"))
        }

        if (!checkShizukuPermission()) {
            return Result.Error(SecurityException("Shizuku permission not granted"))
        }

        return try {

            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager

            val appTasks = activityManager.appTasks

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

    fun forceStopSpotify(): Result<Unit> {

        if (!isShizukuAvailable()) {
            return Result.Error(IllegalStateException("Shizuku service not available"))
        }

        if (!checkShizukuPermission()) {
            return Result.Error(SecurityException("Shizuku permission not granted"))
        }

        return try {

            safeAddHiddenApiExemptions("Landroid/app/IActivityManager")

            val activityService: IBinder = SystemServiceHelper.getSystemService("activity")
            val wrappedBinder = ShizukuBinderWrapper(activityService)

            val stubClass = Class.forName("android.app.IActivityManager\$Stub")
            val asInterfaceMethod = stubClass.getDeclaredMethod("asInterface", IBinder::class.java)
            asInterfaceMethod.isAccessible = true
            val activityManager = asInterfaceMethod.invoke(null, wrappedBinder)

            val forceStopMethod = activityManager!!.javaClass.getDeclaredMethod(
                "forceStopPackage",
                String::class.java,
                Int::class.javaPrimitiveType
            )
            forceStopMethod.isAccessible = true

            val userHandleClass = Class.forName("android.os.UserHandle")
            val myUserIdMethod = userHandleClass.getDeclaredMethod("myUserId")
            myUserIdMethod.isAccessible = true
            val userId = myUserIdMethod.invoke(null) as Int

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

    fun launchActivity(packageName: String, context: Context): Result<Unit> {

        if (!isShizukuAvailable()) {
            return Result.Error(IllegalStateException("Shizuku service not available"))
        }

        if (!checkShizukuPermission()) {
            return Result.Error(SecurityException("Shizuku permission not granted"))
        }

        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return Result.Error(IllegalStateException("Package $packageName not installed or has no launch intent"))

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {

            safeAddHiddenApiExemptions(
                "Landroid/app/IActivityManager",
                "Landroid/content/IIntentReceiver",
                "Landroid/os/UserHandle"
            )

            val activityService: IBinder = SystemServiceHelper.getSystemService("activity")
            val wrappedBinder = ShizukuBinderWrapper(activityService)

            val stubClass = Class.forName("android.app.IActivityManager\$Stub")
            val asInterfaceMethod = stubClass.getDeclaredMethod("asInterface", IBinder::class.java)
            asInterfaceMethod.isAccessible = true
            val activityManager = asInterfaceMethod.invoke(null, wrappedBinder)

            val userHandleClass = Class.forName("android.os.UserHandle")
            val myUserIdMethod = userHandleClass.getDeclaredMethod("myUserId")
            myUserIdMethod.isAccessible = true
            val userId = myUserIdMethod.invoke(null) as Int

            val startActivityMethod = activityManager!!.javaClass.getDeclaredMethod(
                "startActivityAsUser",
                Class.forName("android.app.IApplicationThread"),
                String::class.java,
                Intent::class.java,
                String::class.java,
                IBinder::class.java,
                String::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Class.forName("android.app.ProfilerInfo"),
                android.os.Bundle::class.java,
                Int::class.javaPrimitiveType
            )
            startActivityMethod.isAccessible = true

            val result = startActivityMethod.invoke(
                activityManager,
                null,
                "com.android.shell",
                intent,
                null,
                null,
                null,
                0,
                0,
                null,
                null,
                userId
            ) as Int

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

    fun getForegroundTaskInfo(): Result<ForegroundTaskInfo> {
        if (!isShizukuAvailable()) {
            return Result.Error(IllegalStateException("Shizuku service not available"))
        }

        if (!checkShizukuPermission()) {
            return Result.Error(SecurityException("Shizuku permission not granted"))
        }

        return try {
            safeAddHiddenApiExemptions("Landroid/app/IActivityTaskManager")

            val activityTaskService: IBinder = SystemServiceHelper.getSystemService("activity_task")
            val wrappedBinder = ShizukuBinderWrapper(activityTaskService)

            val stubClass = Class.forName("android.app.IActivityTaskManager\$Stub")
            val asInterfaceMethod = stubClass.getDeclaredMethod("asInterface", IBinder::class.java)
            asInterfaceMethod.isAccessible = true
            val activityTaskManager = asInterfaceMethod.invoke(null, wrappedBinder)
                ?: return Result.Error(IllegalStateException("Failed to obtain IActivityTaskManager interface"))

            val getFocusedRootTaskInfoMethod = activityTaskManager.javaClass.getDeclaredMethod("getFocusedRootTaskInfo")
            getFocusedRootTaskInfoMethod.isAccessible = true
            val focusedTaskInfo = getFocusedRootTaskInfoMethod.invoke(activityTaskManager) as? android.app.TaskInfo
                ?: return Result.Error(IllegalStateException("No focused task info returned"))

            val taskId = focusedTaskInfo.taskId
            val foregroundPackageName = focusedTaskInfo.topActivity?.packageName
            Log.d(TAG, "Captured foreground task id: $taskId, package: $foregroundPackageName")
            Result.Success(ForegroundTaskInfo(taskId, foregroundPackageName))
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception while capturing foreground task", e)
            Result.Error(e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception while capturing foreground task", e)
            Result.Error(e)
        }
    }

    fun restoreForegroundTask(taskId: Int): Result<Unit> {
        if (!isShizukuAvailable()) {
            return Result.Error(IllegalStateException("Shizuku service not available"))
        }

        if (!checkShizukuPermission()) {
            return Result.Error(SecurityException("Shizuku permission not granted"))
        }

        return try {
            safeAddHiddenApiExemptions(
                "Landroid/app/IActivityManager",
                "Landroid/app/IApplicationThread"
            )

            val activityService: IBinder = SystemServiceHelper.getSystemService("activity")
            val wrappedBinder = ShizukuBinderWrapper(activityService)

            val stubClass = Class.forName("android.app.IActivityManager\$Stub")
            val asInterfaceMethod = stubClass.getDeclaredMethod("asInterface", IBinder::class.java)
            asInterfaceMethod.isAccessible = true
            val activityManager = asInterfaceMethod.invoke(null, wrappedBinder)
                ?: return Result.Error(IllegalStateException("Failed to obtain IActivityManager interface"))

            val moveTaskToFrontMethod = activityManager.javaClass.getDeclaredMethod(
                "moveTaskToFront",
                Class.forName("android.app.IApplicationThread"),
                String::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                android.os.Bundle::class.java
            )
            moveTaskToFrontMethod.isAccessible = true
            moveTaskToFrontMethod.invoke(activityManager, null, "com.android.shell", taskId, 0, null)

            Log.d(TAG, "Restored foreground task id: $taskId")
            Result.Success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception while restoring foreground task", e)
            Result.Error(e)
        } catch (e: Exception) {
            Log.w(TAG, "Could not restore previous foreground task (it may no longer exist)", e)
            Result.Error(e)
        }
    }

    private fun safeAddHiddenApiExemptions(vararg signature: String) {
        try {
            HiddenApiBypass.addHiddenApiExemptions(*signature)
        } catch (t: Throwable) {
            Log.w(TAG, "HiddenApiBypass not supported in current environment: ${t.message}")
        }
    }
}
