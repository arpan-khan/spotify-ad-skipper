package com.spotify.adskipper

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var hasRequestedNotificationPermission = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            updateUIStatus()
        }
    }

    private val shizukuPermissionListener = rikka.shizuku.Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d(TAG, "Shizuku permission granted")
                updateUIStatus()
            } else {
                android.util.Log.w(TAG, "Shizuku permission denied")
                Toast.makeText(
                    this,
                    "Shizuku permission denied",
                    Toast.LENGTH_SHORT
                ).show()
                updateUIStatus()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AdSkipperNotificationManager.createNotificationChannel(this)

        rikka.shizuku.Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)

        findViewById<Button>(R.id.btnNotificationAccess).setOnClickListener {
            requestNotificationAccess()
        }

        findViewById<Button>(R.id.btnShizukuPermission).setOnClickListener {
            requestShizukuPermission()
        }

        findViewById<Button>(R.id.btnBatteryOptimization).setOnClickListener {
            requestBatteryOptimizationExemption()
        }

        findViewById<SwitchCompat>(R.id.switchAdSkipEnabled).setOnCheckedChangeListener { _, isChecked ->
            AdSkipPreferences.setServiceEnabled(this, isChecked)
            updateUIStatus()
        }

        findViewById<LinearLayout>(R.id.rowManageExclusions).setOnClickListener {
            startActivity(Intent(this, ExclusionListActivity::class.java))
        }

        updateUIStatus()
    }

    override fun onResume() {
        super.onResume()

        updateUIStatus()
    }

    override fun onDestroy() {

        rikka.shizuku.Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        super.onDestroy()
    }

    fun checkNotificationAccess(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        val packageName = packageName
        return enabledListeners?.contains(packageName) == true
    }

    fun checkShizukuStatus(): ShizukuStatus {
        return when {
            !ShizukuController.isShizukuAvailable() -> {
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

    fun checkBatteryOptimization(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(packageName)
        }
        return true
    }

    fun updateUIStatus() {
        val notificationGranted = checkNotificationAccess()
        val shizukuStatus = checkShizukuStatus()
        val batteryOptimized = checkBatteryOptimization()
        val shizukuGranted = shizukuStatus == ShizukuStatus.RUNNING_AND_GRANTED
        val allGranted = notificationGranted && shizukuGranted && batteryOptimized
        val serviceEnabled = AdSkipPreferences.isServiceEnabled(this)
        val prerequisitesMet = notificationGranted && shizukuGranted
        val serviceActive = serviceEnabled && prerequisitesMet

        val btnNotif = findViewById<Button>(R.id.btnNotificationAccess)
        val badgeNotif = findViewById<TextView>(R.id.badgeNotificationStatus)
        if (notificationGranted) {
            btnNotif.visibility = View.GONE
            badgeNotif.text = "✓ Granted"
            badgeNotif.setBackgroundResource(R.drawable.bg_badge_granted)
            badgeNotif.setTextColor(ContextCompat.getColor(this, R.color.spotify_green_bright))
        } else {
            btnNotif.visibility = View.VISIBLE
            btnNotif.isEnabled = true
            badgeNotif.text = "Required"
            badgeNotif.setBackgroundResource(R.drawable.bg_badge_pending)
            badgeNotif.setTextColor(ContextCompat.getColor(this, R.color.status_warning))
        }

        val btnShizuku = findViewById<Button>(R.id.btnShizukuPermission)
        val badgeShizuku = findViewById<TextView>(R.id.badgeShizukuStatus)
        val tvShizukuStatus = findViewById<TextView>(R.id.tvShizukuStatus)

        tvShizukuStatus.text = when (shizukuStatus) {
            ShizukuStatus.RUNNING_AND_GRANTED -> getString(R.string.shizuku_running_and_granted)
            ShizukuStatus.RUNNING_NOT_GRANTED -> getString(R.string.shizuku_running_not_granted)
            ShizukuStatus.NOT_RUNNING -> getString(R.string.shizuku_not_running)
            ShizukuStatus.NOT_INSTALLED -> getString(R.string.shizuku_not_installed)
        }

        if (shizukuGranted) {
            btnShizuku.visibility = View.GONE
            badgeShizuku.text = "✓ Authorized"
            badgeShizuku.setBackgroundResource(R.drawable.bg_badge_granted)
            badgeShizuku.setTextColor(ContextCompat.getColor(this, R.color.spotify_green_bright))
        } else {
            btnShizuku.visibility = View.VISIBLE
            badgeShizuku.text = "Required"
            badgeShizuku.setBackgroundResource(R.drawable.bg_badge_pending)
            badgeShizuku.setTextColor(ContextCompat.getColor(this, R.color.status_warning))

            when (shizukuStatus) {
                ShizukuStatus.NOT_INSTALLED -> {
                    btnShizuku.isEnabled = true
                    btnShizuku.text = getString(R.string.install_shizuku_app)
                }
                ShizukuStatus.NOT_RUNNING -> {
                    btnShizuku.isEnabled = true
                    btnShizuku.text = getString(R.string.open_shizuku_app)
                }
                ShizukuStatus.RUNNING_NOT_GRANTED -> {
                    btnShizuku.isEnabled = true
                    btnShizuku.text = getString(R.string.grant_shizuku_permission)
                }
                else -> {
                    btnShizuku.isEnabled = false
                }
            }
        }

        val btnBattery = findViewById<Button>(R.id.btnBatteryOptimization)
        val badgeBattery = findViewById<TextView>(R.id.badgeBatteryStatus)
        if (batteryOptimized) {
            btnBattery.visibility = View.GONE
            badgeBattery.text = "✓ Unrestricted"
            badgeBattery.setBackgroundResource(R.drawable.bg_badge_granted)
            badgeBattery.setTextColor(ContextCompat.getColor(this, R.color.spotify_green_bright))
        } else {
            btnBattery.visibility = View.VISIBLE
            btnBattery.isEnabled = true
            badgeBattery.text = "Recommended"
            badgeBattery.setBackgroundResource(R.drawable.bg_badge_pending)
            badgeBattery.setTextColor(ContextCompat.getColor(this, R.color.status_warning))
        }

        val adSkipSwitch = findViewById<SwitchCompat>(R.id.switchAdSkipEnabled)
        if (adSkipSwitch.isChecked != serviceEnabled) {
            adSkipSwitch.isChecked = serviceEnabled
        }
        adSkipSwitch.isEnabled = prerequisitesMet

        findViewById<ImageView>(R.id.ivServiceStatus).setImageResource(
            if (serviceActive) R.drawable.ic_check else R.drawable.ic_cross
        )
        findViewById<TextView>(R.id.tvServiceStatus).text = when {
            !prerequisitesMet -> getString(R.string.service_status_waiting_permissions)
            serviceEnabled -> getString(R.string.service_status_active)
            else -> getString(R.string.service_status_paused)
        }

        val tvHeroBadge = findViewById<TextView>(R.id.tvHeroBadge)
        val tvHeroTitle = findViewById<TextView>(R.id.tvHeroTitle)
        val tvHeroDesc = findViewById<TextView>(R.id.tvHeroDesc)

        when {
            !prerequisitesMet -> {
                tvHeroBadge.text = getString(R.string.hero_badge_setup)
                tvHeroBadge.setBackgroundResource(R.drawable.bg_badge_pending)
                tvHeroBadge.setTextColor(ContextCompat.getColor(this, R.color.status_warning))
                tvHeroTitle.text = getString(R.string.hero_title_setup)
                tvHeroDesc.text = getString(R.string.hero_desc_setup)
            }
            !serviceEnabled -> {
                tvHeroBadge.text = getString(R.string.hero_badge_paused)
                tvHeroBadge.setBackgroundResource(R.drawable.bg_badge_pending)
                tvHeroBadge.setTextColor(ContextCompat.getColor(this, R.color.status_warning))
                tvHeroTitle.text = getString(R.string.hero_title_paused)
                tvHeroDesc.text = getString(R.string.hero_desc_paused)
            }
            else -> {
                tvHeroBadge.text = getString(R.string.hero_badge_active)
                tvHeroBadge.setBackgroundResource(R.drawable.bg_badge_granted)
                tvHeroBadge.setTextColor(ContextCompat.getColor(this, R.color.spotify_green_bright))
                tvHeroTitle.text = getString(R.string.hero_title_active)
                tvHeroDesc.text = if (batteryOptimized) {
                    getString(R.string.hero_desc_active)
                } else {
                    getString(R.string.hero_desc_active_battery_warn)
                }
            }
        }

        if (serviceActive) {
            checkAndRequestNotificationPermission()
            AdSkipperNotificationManager.showEngineActiveNotification(this)
        } else {
            AdSkipperNotificationManager.cancelNotification(this)
        }

        val legacyStatus = findViewById<TextView>(R.id.tvStatusMessage)
        if (legacyStatus != null) {
            legacyStatus.text = if (allGranted) getString(R.string.status_complete) else getString(R.string.status_incomplete)
        }

        val ivNotifStatus = findViewById<ImageView>(R.id.ivNotificationStatus)
        if (ivNotifStatus != null) {
            ivNotifStatus.setImageResource(if (notificationGranted) R.drawable.ic_check else R.drawable.ic_cross)
        }
        val ivShizuku = findViewById<ImageView>(R.id.ivShizukuStatus)
        if (ivShizuku != null) {
            ivShizuku.setImageResource(if (shizukuGranted) R.drawable.ic_check else R.drawable.ic_cross)
        }
        val ivBattery = findViewById<ImageView>(R.id.ivBatteryStatus)
        if (ivBattery != null) {
            ivBattery.setImageResource(if (batteryOptimized) R.drawable.ic_check else R.drawable.ic_cross)
        }

        val excludedCount = ExclusionPreferences.getExcludedPackages(this).size
        findViewById<TextView>(R.id.tvExclusionCount).text = when (excludedCount) {
            0 -> getString(R.string.exclusion_none_active)
            1 -> getString(R.string.exclusion_count_one)
            else -> getString(R.string.exclusion_count_format, excludedCount)
        }

        findViewById<TextView>(R.id.tvStatTotal).text = AdSkipStats.getTotalSkips(this).toString()
        findViewById<TextView>(R.id.tvStatToday).text = AdSkipStats.getSkipsToday(this).toString()
        findViewById<TextView>(R.id.tvStatWeek).text = AdSkipStats.getSkipsThisWeek(this).toString()
        findViewById<TextView>(R.id.tvStatTimeSaved).text =
            getString(R.string.stats_time_saved_format, AdSkipStats.getFormattedTimeSaved(this))
    }

    fun requestNotificationAccess() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }

    fun requestShizukuPermission() {
        when (checkShizukuStatus()) {
            ShizukuStatus.RUNNING_NOT_GRANTED -> {
                rikka.shizuku.Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
            }
            ShizukuStatus.NOT_RUNNING -> {
                val launchIntent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                if (launchIntent != null) {
                    startActivity(launchIntent)
                } else {
                    openShizukuWebsite()
                }
            }
            ShizukuStatus.NOT_INSTALLED -> {
                openShizukuWebsite()
            }
            ShizukuStatus.RUNNING_AND_GRANTED -> {

            }
        }
    }

    private fun openShizukuWebsite() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app"))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open browser", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (!hasRequestedNotificationPermission) {
                    hasRequestedNotificationPermission = true
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
    }
}
