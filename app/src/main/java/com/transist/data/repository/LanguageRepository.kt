package com.transist.data.repository

import android.content.Context
import com.transist.data.local.DatabaseHelper
import com.transist.data.model.Language
import com.transist.util.getLanguageListInNative

class LanguageRepository (private val context: Context) {

    private val dbHelper = DatabaseHelper(context)

    fun getUserLanguages(): Pair<String, String> {
        val userLangs = dbHelper.readLanguages(dbHelper.writableDatabase) ?: Pair("un", "un")

        val langList = getListOfLanguages()

        val nativeLanguage = langList.find { it.code == userLangs.first }?.name ?: "Unknown"
        val targetLanguage = langList.find { it.code == userLangs.second }?.name ?: "Unknown"

        return Pair(nativeLanguage, targetLanguage)
    }

    fun getUserLanguagesCodes(): Pair<String, String> {
        return dbHelper.readLanguages(dbHelper.writableDatabase) ?: Pair("un", "un")
    }

    fun updateLanguages(nativeCode: String, targetCode: String) {
        dbHelper.updateLanguages(dbHelper.writableDatabase, nativeCode, targetCode)
    }

    fun getLanguageListInNative(languageList: List<Language>): List<String> {
        return getLanguageListInNative(context, languageList)
    }

    fun getListOfLanguages(): List<Language> {
        return try {
            val inputStream = context.assets.open("diller.txt")
            inputStream.bufferedReader().useLines { lines ->
                lines.mapNotNull { line ->
                    val parts = line.split(":").map { it.trim() }
                    if (parts.size == 2) Language(parts[0], parts[1]) else null
                }.toList()
            }
        } catch (e: Exception) {
            listOf(Language("Tanımlanmadı", "unknown"))
        }
    }
}


