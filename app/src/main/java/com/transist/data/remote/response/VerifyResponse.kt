package com.transist.data.remote.response

data class VerifyResponse(
    val success: Boolean,
    val message: String?,
    val productId: String,
    val expiryTime: String,
    val subscriptionState: String
)