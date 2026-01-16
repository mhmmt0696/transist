package com.transist.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.gms.appset.AppSet
import com.google.android.gms.appset.AppSetIdInfo

class PreferencesRepository(private val context: Context) {

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

    fun getInitalTranslationQuota(): Int = sharedPref.getInt("initial_translation_quota", 3)

    fun setInitalTranslationQuota(count: Int) {
        sharedPref.edit().putInt("initial_translation_quota", count).apply()
    }

    fun decreaseInitalTranslationQuota(): Int {
        val currentQuota = getInitalTranslationQuota()
        val decreasedQuota = currentQuota - 1
        if (currentQuota > 0){
            setInitalTranslationQuota(decreasedQuota)
        }
        return decreasedQuota
    }

}
