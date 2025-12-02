package com.noteability.mynote.ui.aiDemo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noteability.mynote.BuildConfig
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

    private val apiKey = "Bearer ${BuildConfig.OPENAI_API_KEY}"

    // Update input fields
    fun onSourceTextChanged(text: String) {
        _uiState.update { it.copy(sourceText = text) }
    }

    fun onTagsInputChanged(text: String) {
        _uiState.update { it.copy(existingTags = text) }
    }

    // Make AI request
    private fun callAi(
        model: String = "qwen3-max",
        systemPrompt: String,
        userPrompt: String,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            // Set loading state
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Make network request
                val response = NetworkModule.api.chat(
                    token = apiKey,
                    request = ChatRequest(
                        model = model,
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
            model = "qwen3-max",
            systemPrompt = "你是一个专业的摘要助手。",
            userPrompt = "请对以下文本进行精简摘要，控制在200字以内：\n\n$text"
        ) { result ->
            _uiState.update { it.copy(summaryResult = result) }
        }
    }

    // Fetch tags
    fun fetchTags() {
        val text = _uiState.value.sourceText
        val tags = _uiState.value.existingTags
        if (text.isBlank()) return

        // Force AI to return a specific format for parsing
        val prompt = """
            文本内容：
            $text
            
            现有标签库：[$tags]
            
            请从标签库中选择最匹配的标签，如果都不匹配，生成新标签。
            请仅输出标签，用英文逗号分隔，不要包含任何其他文字，至多 3 个标签。
            例如：Android, AI, Coding
        """.trimIndent()

        callAi(
            model = "qwen3-max",
            systemPrompt = "你是一个分类专家，只输出逗号分隔的标签。",
            userPrompt = prompt
        ) { result ->
            // Simple parse: split by commas
            val tagList = result.split(",", "，").map { it.trim() }.filter { it.isNotEmpty() }
            _uiState.update { it.copy(tagResult = tagList) }
        }
    }
}
