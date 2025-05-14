package com.transist.data.model

data class FolderData(
    val id: Int,
    val name: String,
    val targetLanguage: String,
    val statusAdd: Int,
    val statusStudy: Int,
    val date_created: Int,
    val itemCount: Int
)