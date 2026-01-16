package com.transist.ui

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.transist.util.LocaleHelper
import java.util.Locale

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(base: Context?) {
        val deviceLanguage = Locale.getDefault().language
        val newContext = LocaleHelper.applyLocale(base!!, deviceLanguage)
        super.attachBaseContext(newContext)
    }
}
