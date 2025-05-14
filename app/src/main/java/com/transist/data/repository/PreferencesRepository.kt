package com.transist.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import org.threeten.bp.LocalDate

class PreferencesRepository(context: Context) {
    private val sharedPref: SharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

    fun isFirstLaunch(): Boolean = sharedPref.getBoolean("isFirstLaunch", true)

    fun getThemeMode(): Int = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    fun setThemeMode(mode: Int) {
        sharedPref.edit().putInt("theme_mode", mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun setFirstLaunchDone() {
        sharedPref.edit().putBoolean("isFirstLaunch", false).apply()
    }

    fun setTranslationCount(count: Int) {
        sharedPref.edit().putInt("translation_count", count).apply()
    }

    fun checkAndResetDailyQuota() {
        val today = LocalDate.now()
        val lastDayString = sharedPref.getString("last_day", today.toString())
        val lastDay = LocalDate.parse(lastDayString)

        if (lastDay.isBefore(today)) {
            // Yeni gün başlamış, kotayı sıfırla
            sharedPref.edit().putInt("translation_count", 20)
                .putString("last_day", today.toString())
                .apply()
        }
    }

    fun getTranslationQuota(): Int = sharedPref.getInt("translation_count", 20)

    fun decreaseTranslationCount() {
        checkAndResetDailyQuota()
        val count = getTranslationQuota()
        if (count != 0) {
            sharedPref.edit().putInt("translation_count", count - 1).apply()
        }
    }
}
