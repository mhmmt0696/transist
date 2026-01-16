package com.transist.data.remote.api

import com.transist.data.remote.request.VerifyRequest
import com.transist.data.remote.response.VerifyResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface BackendPurchaseApi {

    @Headers("Content-Type: application/json")
    @POST("verify")
    fun verifyPurchase(@Body body: VerifyRequest): Call<VerifyResponse>
}

