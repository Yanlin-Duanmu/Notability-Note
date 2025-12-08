package com.noteability.mynote.ui.search

import android.content.Context
import android.content.SharedPreferences

class SearchHistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    private val historyKey = "history"
    private val maxHistorySize = 3 // You can adjust this value

    fun getSearchHistory(): List<String> {
        val historySet = prefs.getStringSet(historyKey, emptySet()) ?: emptySet()
        return historySet.toList().reversed().take(maxHistorySize)
    }

    fun saveSearchQuery(query: String) {
        if (query.isNotBlank()) {
            val historyList = (prefs.getStringSet(historyKey, LinkedHashSet()) ?: LinkedHashSet()).toMutableList()

            historyList.remove(query) // Remove if exists to update its position
            historyList.add(0, query)   // Add to the front as the most recent

            val limitedHistory = historyList.take(maxHistorySize)

            prefs.edit().putStringSet(historyKey, LinkedHashSet(limitedHistory)).apply()
        }
    }

    fun removeSearchQuery(query: String) {
        if (query.isNotBlank()) {
            val historySet = prefs.getStringSet(historyKey, LinkedHashSet()) ?: return
            val newHistorySet = LinkedHashSet(historySet)
            if (newHistorySet.remove(query)) {
                prefs.edit().putStringSet(historyKey, newHistorySet).apply()
            }
        }
    }
    fun clearHistory() {
        // This clears the entire search history set from SharedPreferences
        prefs.edit().remove(historyKey).apply()
    }
}
