package com.transist.data.remote.request

data class VerifyRequest(
    val purchaseToken: String,
    val uid: String,
    val packageName: String
)
