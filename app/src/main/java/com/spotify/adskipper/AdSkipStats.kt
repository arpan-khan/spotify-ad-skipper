package com.spotify.adskipper

import android.content.Context
import java.util.Calendar

object AdSkipStats {

    private const val PREFS_NAME = "ad_skip_stats"
    private const val KEY_TIMESTAMPS = "skip_timestamps"
    private const val AVERAGE_AD_LENGTH_SECONDS = 25L

    fun recordSkip(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updated = HashSet(prefs.getStringSet(KEY_TIMESTAMPS, emptySet()) ?: emptySet())
        updated.add(System.currentTimeMillis().toString())
        prefs.edit().putStringSet(KEY_TIMESTAMPS, updated).apply()
    }

    fun getTotalSkips(context: Context): Int {
        return readTimestamps(context).size
    }

    fun getSkipsToday(context: Context): Int {
        val startOfDay = startOfTodayMillis()
        return readTimestamps(context).count { it >= startOfDay }
    }

    fun getSkipsThisWeek(context: Context): Int {
        val startOfWeek = startOfWeekMillis()
        return readTimestamps(context).count { it >= startOfWeek }
    }

    fun getEstimatedTimeSavedSeconds(context: Context): Long {
        return getTotalSkips(context) * AVERAGE_AD_LENGTH_SECONDS
    }

    fun getFormattedTimeSaved(context: Context): String {
        val totalSeconds = getEstimatedTimeSavedSeconds(context)
        val minutes = totalSeconds / 60
        val hours = minutes / 60
        return when {
            hours > 0 -> {
                val remainingMinutes = minutes % 60
                if (remainingMinutes == 0L) "~${hours}h" else "~${hours}h ${remainingMinutes}m"
            }
            minutes > 0 -> "~${minutes} min"
            else -> "~${totalSeconds} sec"
        }
    }

    private fun readTimestamps(context: Context): List<Long> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getStringSet(KEY_TIMESTAMPS, emptySet()) ?: emptySet()
        return raw.mapNotNull { it.toLongOrNull() }
    }

    private fun startOfTodayMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun startOfWeekMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
