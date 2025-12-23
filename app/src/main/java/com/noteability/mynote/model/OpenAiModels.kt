package com.noteability.mynote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Request payload for the API
@Serializable
data class ChatRequest(
    val model: String = "",
    val messages: List<Message>,
    val temperature: Double = 0.7,
    val stream: Boolean = false
)

// Message format
@Serializable
data class Message(
    val role: String, // "system" or "user"
    val content: String
)

// API response payload (non-streaming)
@Serializable
data class ChatResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: Message? = null,
    val delta: Delta? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

// Streaming delta
@Serializable
data class Delta(
    val content: String? = null,
    val role: String? = null
)
