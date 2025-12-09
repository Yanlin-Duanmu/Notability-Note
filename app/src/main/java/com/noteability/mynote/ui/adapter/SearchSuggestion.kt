package com.noteability.mynote.ui.adapter

data class SearchSuggestion(
    val text: String,
    val type: SearchSuggestionType,
    val noteId: Long? = null // 用于跳转
)

// 【新增】在文件顶部定义 SearchSuggestionType 枚举
enum class SearchSuggestionType {
    HISTORY, SUGGESTION, NO_MATCH
}
