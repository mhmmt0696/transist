package com.transist.data.local

import android.content.Context

class TxtFileWordReader(context: Context, fileName: String) {
    private var currentIndex = 0
    private val words: List<String>

    init {
        val assetManager = context.assets
        val inputStream = assetManager.open(fileName)
        words = inputStream.bufferedReader().use { it.readLines() }
    }

    fun getNextWordFromTxtFile(): String? {
        return if (currentIndex < words.size) {
            words[currentIndex++]
        } else {
            null // Tüm kelimeler okundu
        }
    }

    fun getNextWordFromAssets(): String? {
        return if (words.isNotEmpty() && currentIndex < words.size) {
            words[currentIndex++].substringBefore(" ").trim()
        } else {
            null // Tüm kelimeler okundu
        }
    }

    fun resetReader() {
        currentIndex = 0 // Okuyucu sıfırlandı
    }
}