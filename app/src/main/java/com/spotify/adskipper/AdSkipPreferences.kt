package com.spotify.adskipper

import android.content.Context

object AdSkipPreferences {

    private const val PREFS_NAME = "ad_skip_prefs"
    private const val KEY_SERVICE_ENABLED = "service_enabled"

    fun isServiceEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SERVICE_ENABLED, true)
    }

    fun setServiceEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
    }
}
