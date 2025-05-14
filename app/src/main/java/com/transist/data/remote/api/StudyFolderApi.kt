package com.transist.data.remote.api

import com.transist.data.remote.request.GenerateContentRequest
import com.transist.data.remote.response.ApiResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// Study Folder Fragment için API arayüzü
interface StudyFolderApi {
    @POST("generate")
    fun getResponse(@Body request: GenerateContentRequest): Call<ApiResponse>
}