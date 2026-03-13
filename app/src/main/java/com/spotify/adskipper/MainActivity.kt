package com.spotify.adskipper

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat

/**
 * Main activity for permission setup and status display.
 * Provides a checklist interface guiding users through required setup steps.
 * 
 * This activity manages three critical permissions:
 * 1. Notification Listener Access - Required to detect Spotify advertisements
 * 2. Shizuku Permission - Required to force-stop Spotify process
 * 3. Battery Optimization Exemption - Required to keep service active
 */
class MainActivity : AppCompatActivity() {
    
    private val shizukuPermissionListener = rikka.shizuku.Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d(TAG, "Shizuku permission granted")
                updateUIStatus()
            } else {
                android.util.Log.w(TAG, "Shizuku permission denied")
                android.widget.Toast.makeText(
                    this,
                    "Shizuku permission denied",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                updateUIStatus()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Register Shizuku permission callback
        rikka.shizuku.Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        
        // Set up button click listeners
        findViewById<android.widget.Button>(R.id.btnNotificationAccess).setOnClickListener {
            requestNotificationAccess()
        }
        
        findViewById<android.widget.Button>(R.id.btnShizukuPermission).setOnClickListener {
            requestShizukuPermission()
        }
        
        findViewById<android.widget.Button>(R.id.btnBatteryOptimization).setOnClickListener {
            requestBatteryOptimizationExemption()
        }
        
        // Check and display initial permission status
        updateUIStatus()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh permission status when returning from settings
        updateUIStatus()
    }
    
    override fun onDestroy() {
        // Unregister Shizuku permission callback
        rikka.shizuku.Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        super.onDestroy()
    }
    
    // ========== Permission Checking Methods ==========
    
    /**
     * Checks if notification listener access is granted.
     * 
     * Uses NotificationManagerCompat to check if this app is enabled
     * as a notification listener service.
     * 
     * @return true if notification access is granted, false otherwise
     */
    fun checkNotificationAccess(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        val packageName = packageName
        return enabledListeners?.contains(packageName) == true
    }
    
    /**
     * Checks the current Shizuku service status and permission state.
     * 
     * Returns one of four possible states:
     * - RUNNING_AND_GRANTED: Shizuku is running and permission is granted
     * - RUNNING_NOT_GRANTED: Shizuku is running but permission not granted
     * - NOT_RUNNING: Shizuku service is not running
     * - NOT_INSTALLED: Shizuku app is not installed
     * 
     * @return ShizukuStatus enum indicating current state
     */
    fun checkShizukuStatus(): ShizukuStatus {
        return when {
            !ShizukuController.isShizukuAvailable() -> {
                // Check if Shizuku app is installed
                try {
                    packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
                    ShizukuStatus.NOT_RUNNING
                } catch (e: PackageManager.NameNotFoundException) {
                    ShizukuStatus.NOT_INSTALLED
                }
            }
            !ShizukuController.checkShizukuPermission() -> ShizukuStatus.RUNNING_NOT_GRANTED
            else -> ShizukuStatus.RUNNING_AND_GRANTED
        }
    }
    
    /**
     * Checks if battery optimization is disabled for this app.
     * 
     * Battery optimization exemption is critical for keeping the
     * SpotifyAdListener service active, especially on Samsung devices
     * with aggressive battery management.
     * 
     * @return true if battery optimization is disabled (exempted), false otherwise
     */
    fun checkBatteryOptimization(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(packageName)
        }
        // Battery optimization doesn't exist before Android M
        return true
    }
    
    // ========== UI Update Methods ==========
    
    /**
     * Updates all UI status indicators based on current permission states.
     * 
     * This method:
     * - Updates checkmark/cross icons for each permission
     * - Updates Shizuku status text
     * - Enables/disables buttons based on current status
     * - Updates overall status message
     */
    fun updateUIStatus() {
        // Check all permission states
        val notificationGranted = checkNotificationAccess()
        val shizukuStatus = checkShizukuStatus()
        val batteryOptimized = checkBatteryOptimization()
        
        // Update notification access status
        findViewById<android.widget.ImageView>(R.id.ivNotificationStatus).setImageResource(
            if (notificationGranted) R.drawable.ic_check else R.drawable.ic_cross
        )
        findViewById<android.widget.Button>(R.id.btnNotificationAccess).isEnabled = !notificationGranted
        
        // Update Shizuku status
        val shizukuGranted = shizukuStatus == ShizukuStatus.RUNNING_AND_GRANTED
        findViewById<android.widget.ImageView>(R.id.ivShizukuStatus).setImageResource(
            if (shizukuGranted) R.drawable.ic_check else R.drawable.ic_cross
        )
        
        // Update Shizuku status text
        val shizukuStatusText = when (shizukuStatus) {
            ShizukuStatus.RUNNING_AND_GRANTED -> getString(R.string.shizuku_running_and_granted)
            ShizukuStatus.RUNNING_NOT_GRANTED -> getString(R.string.shizuku_running_not_granted)
            ShizukuStatus.NOT_RUNNING -> getString(R.string.shizuku_not_running)
            ShizukuStatus.NOT_INSTALLED -> getString(R.string.shizuku_not_installed)
        }
        findViewById<android.widget.TextView>(R.id.tvShizukuStatus).text = shizukuStatusText
        
        // Enable/disable Shizuku button based on availability
        findViewById<android.widget.Button>(R.id.btnShizukuPermission).isEnabled = 
            shizukuStatus == ShizukuStatus.RUNNING_NOT_GRANTED
        
        // Update battery optimization status
        findViewById<android.widget.ImageView>(R.id.ivBatteryStatus).setImageResource(
            if (batteryOptimized) R.drawable.ic_check else R.drawable.ic_cross
        )
        findViewById<android.widget.Button>(R.id.btnBatteryOptimization).isEnabled = !batteryOptimized
        
        // Update overall status message
        val allGranted = notificationGranted && shizukuGranted && batteryOptimized
        findViewById<android.widget.TextView>(R.id.tvStatusMessage).text = 
            if (allGranted) getString(R.string.status_complete) else getString(R.string.status_incomplete)
    }
    
    // ========== Permission Request Methods ==========
    
    /**
     * Opens system settings to allow user to grant notification listener access.
     * 
     * Navigates to ACTION_NOTIFICATION_LISTENER_SETTINGS where the user
     * can enable SpotifyAdListener in the notification access list.
     */
    fun requestNotificationAccess() {
        val intent = android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }
    
    /**
     * Requests Shizuku API permission from the user.
     * 
     * Shows a system dialog requesting permission to use Shizuku API.
     * The result will be delivered to the registered permission listener.
     * 
     * Note: Shizuku service must be running for this to work.
     */
    fun requestShizukuPermission() {
        if (ShizukuController.isShizukuAvailable()) {
            rikka.shizuku.Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
        }
    }
    
    /**
     * Opens system settings to request battery optimization exemption.
     * 
     * Navigates to ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS where
     * the user can disable battery optimization for this app.
     * 
     * Only available on Android M (API 23) and above.
     */
    fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = android.content.Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                android.net.Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
    
    companion object {
        private const val TAG = "MainActivity"
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
    }
}
