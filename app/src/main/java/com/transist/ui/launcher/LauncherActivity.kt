package com.transist.ui.launcher

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.transist.ui.main.ActivityMain
import com.transist.ui.welcome.WelcomeActivity
import com.transist.data.repository.PreferencesRepository

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        // Access the repository directly.
        val prefsRepository = PreferencesRepository(applicationContext)
        val themeMode = prefsRepository.getThemeMode()
        val isFirstLaunch = prefsRepository.isFirstLaunch()

        prefsRepository.checkAndResetDailyQuota()

        prefsRepository.setTranslationCount(10)

        // Set the theme.
        AppCompatDelegate.setDefaultNightMode(themeMode)
        super.onCreate(savedInstanceState)

        // Redirect the user.
        if (isFirstLaunch) {
            startActivity(Intent(this, WelcomeActivity::class.java))
        } else {
            startActivity(Intent(this, ActivityMain::class.java))
        }

        // Close the splash activity.
        finish()

    }
}
