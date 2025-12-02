package com.noteability.mynote.model

import kotlinx.serialization.Serializable

// Request payload for the API
@Serializable
data class ChatRequest(
    val model: String = "",
    val messages: List<Message>,
    val temperature: Double = 0.7
)

// Message format
@Serializable
data class Message(
    val role: String, // "system" or "user"
    val content: String
)


