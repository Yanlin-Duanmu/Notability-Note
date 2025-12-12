package com.noteability.mynote.util

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class TextDiffUtils {
    private val TAG = "TextDiffUtils"
    private val LONG_TEXT_THRESHOLD = 5000
    
    /**
     * 检查文本是否需要使用差分存储
     */
    fun shouldUseDiffStorage(text: String): Boolean {
        return text.length > LONG_TEXT_THRESHOLD
    }
    
    /**
     * 生成两个文本之间的差异
     */
    fun generateDiff(oldText: String, newText: String): String {
        try {
            val diffs = computeDiffs(oldText, newText)
            return diffsToJson(diffs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate diff: ${e.message}")
            return ""
        }
    }
    
    /**
     * 应用差异到原文本
     */
    fun applyDiff(originalText: String, diffData: String): String {
        try {
            val diffs = jsonToDiffs(diffData)
            return applyDiffs(originalText, diffs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply diff: ${e.message}")
            return originalText
        }
    }
    
    /**
     * 使用Myers差分算法计算两个字符串之间的差异（优化版）
     */
    private fun computeDiffs(oldText: String, newText: String): List<Diff>
    {
        val n = oldText.length
        val m = newText.length
        val max = n + m
        val trace = mutableListOf<IntArray>()
        
        // 初始数组，只需要创建一次
        val initialV = IntArray(2 * max + 1)
        trace.add(initialV)
        
        for (d in 1..max) {
            // 只复制必要的数组元素，而不是整个数组
            val prevV = trace[d - 1]
            val currentV = IntArray(2 * max + 1)
            
            // 只复制可能被访问的元素，而不是整个数组
            // k的范围是-d到d，所以只需要复制对应的索引范围
            val start = max - d
            val end = max + d + 1
            System.arraycopy(prevV, start, currentV, start, end - start)
            
            trace.add(currentV)
            
            for (k in -d..d step 2) {
                var x: Int
                
                val kIndex = k + max
                if (k == -d || (k != d && prevV[kIndex - 1] < prevV[kIndex + 1])) {
                    x = prevV[kIndex + 1]
                } else {
                    x = prevV[kIndex - 1] + 1
                }
                
                var y = x - k
                
                // 优化：跳过连续相同的字符
                while (x < n && y < m && oldText[x] == newText[y]) {
                    x++
                    y++
                }
                
                currentV[kIndex] = x
                
                if (x >= n && y >= m) {
                    return buildDiffList(oldText, newText, trace)
                }
            }
        }
        
        return emptyList()
    }
    
    /**
     * 构建差异列表（优化版）
     */
    private fun buildDiffList(oldText: String, newText: String, trace: List<IntArray>): List<Diff>
    {
        val diffs = mutableListOf<Diff>()
        var x = oldText.length
        var y = newText.length
        val max = oldText.length + newText.length
        
        for (d in trace.size - 1 downTo 1) {
            val v = trace[d]
            val vPrev = trace[d - 1]
            val k = x - y
            
            val kPrev = if (k == -d || (k != d && vPrev[k - 1 + max] < vPrev[k + 1 + max])) {
                k + 1
            } else {
                k - 1
            }
            
            val xPrev = vPrev[kPrev + max]
            val yPrev = xPrev - kPrev
            
            // 跳过相同的部分
            while (x > xPrev && y > yPrev && oldText[x - 1] == newText[y - 1]) {
                x--
                y--
            }
            
            if (x > xPrev) {
                // 删除操作
                if (xPrev < x) {
                    // 直接使用字符串切片，避免额外的字符串操作
                    diffs.add(Diff(DiffType.DELETE, xPrev, oldText.substring(xPrev, x)))
                }
                x = xPrev
            } else if (y > yPrev) {
                // 插入操作
                if (yPrev < y) {
                    // 直接使用字符串切片，避免额外的字符串操作
                    diffs.add(Diff(DiffType.INSERT, x, newText.substring(yPrev, y)))
                }
                y = yPrev
            }
        }
        
        // 反转差异列表并返回
        return diffs.reversed()
    }
    
    /**
     * 将差异列表转换为JSON格式
     */
    private fun diffsToJson(diffs: List<Diff>): String {
        val jsonArray = JSONArray()
        
        for (diff in diffs) {
            val jsonObject = JSONObject()
            jsonObject.put("type", diff.type.name)
            jsonObject.put("position", diff.position)
            jsonObject.put("text", diff.text)
            jsonArray.put(jsonObject)
        }
        
        return jsonArray.toString()
    }
    
    /**
     * 将JSON格式的差异数据转换为差异列表
     */
    private fun jsonToDiffs(json: String): List<Diff> {
        val diffs = mutableListOf<Diff>()
        val jsonArray = JSONArray(json)
        
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val type = DiffType.valueOf(jsonObject.getString("type"))
            val position = jsonObject.getInt("position")
            val text = jsonObject.getString("text")
            diffs.add(Diff(type, position, text))
        }
        
        return diffs
    }
    
    /**
     * 应用差异列表到原文本
     */
    private fun applyDiffs(original: String, diffs: List<Diff>): String {
        val sb = StringBuilder(original)
        var offset = 0
        
        for (diff in diffs) {
            val pos = diff.position + offset
            
            when (diff.type) {
                DiffType.INSERT -> {
                    sb.insert(pos, diff.text)
                    offset += diff.text.length
                }
                DiffType.DELETE -> {
                    sb.delete(pos, pos + diff.text.length)
                    offset -= diff.text.length
                }
            }
        }
        
        return sb.toString()
    }
    
    /**
     * 差异类型枚举
     */
    enum class DiffType {
        INSERT, DELETE
    }
    
    /**
     * 差异数据类
     */
    data class Diff(val type: DiffType, val position: Int, val text: String)
}