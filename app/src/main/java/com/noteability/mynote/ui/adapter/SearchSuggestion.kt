package com.noteability.mynote.ui.adapter

data class SearchSuggestion(
    val text: String,
    val type: SearchSuggestionType
)

enum class SearchSuggestionType {
    HISTORY,
    SUGGESTION,
    NO_MATCH // <-- Add this line
}
