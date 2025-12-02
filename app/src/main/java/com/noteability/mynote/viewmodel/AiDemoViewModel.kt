package com.noteability.mynote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noteability.mynote.model.ChatRequest
import com.noteability.mynote.model.Message
import com.noteability.mynote.model.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiUiState(
    val sourceText: String = "",       // original text input
    val existingTags: String = "",     // existing tag library input
    val summaryResult: String = "",    // summary result
    val tagResult: List<String> = emptyList(), // tag result
    val isLoading: Boolean = false,    // loading flag
    val error: String? = null          // error message
)

class AiDemoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())

    // Read-only state exposed to UI
    val uiState = _uiState.asStateFlow()

    private val apiKey = "Bearer sk-" // TODO: insert your key

    // Update input fields
    fun onSourceTextChanged(text: String) {
        _uiState.update { it.copy(sourceText = text) }
    }

    fun onTagsInputChanged(text: String) {
        _uiState.update { it.copy(existingTags = text) }
    }

    // Make AI request
    private fun callAi(systemPrompt: String, userPrompt: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            // Set loading state
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Make network request
                val response = NetworkModule.api.chat(
                    token = apiKey,
                    request = ChatRequest(
                        messages = listOf(
                            Message("system", systemPrompt),
                            Message("user", userPrompt)
                        )
                    )
                )
                // Get result
                val content = response.choices.firstOrNull()?.message?.content ?: ""
                onSuccess(content)
            } catch (e: Exception) {
                // Handle error
                _uiState.update { it.copy(error = "请求失败: ${e.message}") }
            } finally {
                // Clear loading state
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // Fetch summary
    fun fetchSummary() {
        val text = _uiState.value.sourceText
        if (text.isBlank()) return

        callAi(
            systemPrompt = "你是一个专业的摘要助手。",
            userPrompt = "请对以下文本进行精简摘要，控制在200字以内：\n\n$text"
        ) { result ->
            _uiState.update { it.copy(summaryResult = result) }
        }
    }

}
