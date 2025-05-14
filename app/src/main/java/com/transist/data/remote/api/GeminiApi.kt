package com.transist.data.remote.api

import com.transist.data.remote.request.GenerateContentRequest
import com.transist.data.remote.response.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface GeminiApi {
    @POST("generate")
    suspend fun getResponse(@Body request: GenerateContentRequest): Response<ApiResponse>
}

