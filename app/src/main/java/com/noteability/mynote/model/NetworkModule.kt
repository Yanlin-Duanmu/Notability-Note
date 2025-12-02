package com.noteability.mynote.model

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val BASE_URL = "https://api.openai.com/"

    private val jsonConfig = Json { ignoreUnknownKeys = true }

    val api: OpenAiApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(jsonConfig.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenAiApi::class.java)
    }
}
