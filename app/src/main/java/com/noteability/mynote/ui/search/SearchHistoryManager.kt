package com.noteability.mynote.ui.search

import android.content.Context
import android.content.SharedPreferences

class SearchHistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    private val historyKey = "history"

    fun getSearchHistory(): List<String> {
        val historySet = prefs.getStringSet(historyKey, emptySet()) ?: emptySet()
        return historySet.toList().reversed()
    }

    fun saveSearchQuery(query: String) {
        if (query.isNotBlank()) {
            val historySet = prefs.getStringSet(historyKey, LinkedHashSet()) ?: LinkedHashSet()
            val newHistorySet = LinkedHashSet(historySet)
            newHistorySet.add(query)
            prefs.edit().putStringSet(historyKey, newHistorySet).apply()
        }
    }
    fun removeSearchQuery(query: String) {
        if (query.isNotBlank()) {
            val historySet = prefs.getStringSet(historyKey, LinkedHashSet()) ?: return
            // 创建一个可变集合的副本
            val newHistorySet = LinkedHashSet(historySet)
            // 从副本中移除指定的查询
            if (newHistorySet.remove(query)) {
                // 如果成功移除了元素，则将新的集合存回SharedPreferences
                prefs.edit().putStringSet(historyKey, newHistorySet).apply()
            }
        }
    }
}