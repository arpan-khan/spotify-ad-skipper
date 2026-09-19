package com.spotify.adskipper

import android.content.Context

object ExclusionPreferences {

    private const val PREFS_NAME = "ad_skip_exclusions"
    private const val KEY_EXCLUDED_PACKAGES = "excluded_packages"

    fun getExcludedPackages(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_EXCLUDED_PACKAGES, emptySet()) ?: emptySet()
    }

    fun setExcludedPackages(context: Context, packages: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_EXCLUDED_PACKAGES, HashSet(packages)).apply()
    }

    fun isExcluded(context: Context, packageName: String): Boolean {
        return getExcludedPackages(context).contains(packageName)
    }
}
