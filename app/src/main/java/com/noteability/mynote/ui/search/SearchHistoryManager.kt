package com.noteability.mynote.ui.search // 请确认包名是否正确

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 管理搜索历史记录，确保记录按时间倒序排列（最新的在最前面）。
 * 使用 Gson 将 List<String> 序列化为 JSON 字符串进行存储。
 */
class SearchHistoryManager(context: Context) {

    private val prefs = context.getSharedPreferences("search_history_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val historyKey = "search_history"

    /**
     * 添加一个新的搜索词。
     * 如果该词已存在，会先移除旧的，再将新的添加到列表头部，以确保它是最新的。
     * @param query 要添加的搜索词，如果为空或仅有空白，则不执行任何操作。
     */
    fun addSearchQuery(query: String) {
        if (query.isBlank()) {
            return
        }

        val history = getSearchHistory().toMutableList()
        // 先移除已存在的相同记录，以更新其位置
        history.remove(query)
        // 将新纪录添加到列表的最前面 (索引为 0 的位置)
        history.add(0, query)
        // 保存更新后的列表
        saveHistory(history)
    }

    /**
     * 获取所有搜索历史记录。
     * @return 一个按时间倒序排列的字符串列表 (List<String>)，最新的记录在最前面。
     */
    fun getSearchHistory(): List<String> {
        val json = prefs.getString(historyKey, null)
        return if (json != null) {
            // 使用 TypeToken 来帮助 Gson 解析泛型列表
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } else {
            // 如果没有历史记录，返回一个空列表
            emptyList()
        }
    }

    /**
     * 从历史记录中移除一个指定的搜索词。
     * @param query 要移除的搜索词。
     */
    fun removeSearchQuery(query: String) {
        val history = getSearchHistory().toMutableList()
        if (history.remove(query)) {
            // 只有在确实发生了移除操作后，才重新保存
            saveHistory(history)
        }
    }

    /**
     * 清除所有的搜索历史记录。
     */
    fun clearHistory() {
        prefs.edit().remove(historyKey).apply()
    }

    /**
     * 将历史记录列表转换为 JSON 字符串并保存到 SharedPreferences。
     */
    private fun saveHistory(historyList: List<String>) {
        val json = gson.toJson(historyList)
        prefs.edit().putString(historyKey, json).apply()
    }
}
