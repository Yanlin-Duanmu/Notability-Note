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

