package com.transist.data.model

data class DialogState(
    val isValidLanguage: Boolean? = null,
    val isValidSpelling: Boolean? = null,
    val isValidMeaning: Boolean? = null,
    val isValidCommonness: Boolean? = null,
    val allDone: Boolean = false
)