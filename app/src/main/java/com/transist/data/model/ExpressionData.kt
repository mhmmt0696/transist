package com.transist.data.model

data class ExpressionData(
    val id: Int,
    val expression: String,
    val meaning: String?,
    val note: String?,
    val sentences: List<String>?,
    var nextSentenceIndex: Int,
    val dateCreated: Long,
    val dilKodu: String,
    val folderId: Int,
    val status: Int,
    val folderName: String
)