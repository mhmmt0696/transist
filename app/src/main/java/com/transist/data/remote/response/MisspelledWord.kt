package com.transist.data.remote.response

data class MisspelledWord(
    val original: String,
    val correction: String
)