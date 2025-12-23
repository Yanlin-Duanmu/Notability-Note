package com.noteability.mynote.model

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

// Retrofit interface for OpenAI Chat Completions API
interface OpenAiApi {
    @POST("v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") token: String,
        @Body request: ChatRequest
    ): ChatResponse

    @Streaming
    @POST("v1/chat/completions")
    suspend fun chatStream(
        @Header("Authorization") token: String,
        @Body request: ChatRequest
    ): ResponseBody
}
