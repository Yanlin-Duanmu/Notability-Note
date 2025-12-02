package com.noteability.mynote.model

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// Retrofit interface for OpenAI Chat Completions API
interface OpenAiApi {
    @POST("v1/chat/completions") // endpoint path
    suspend fun chat(
        @Header("Authorization") token: String, // API key header
        @Body request: ChatRequest
    ): ChatResponse
}
