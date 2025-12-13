package com.noteability.mynote.util

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

class TextDiffUtils {
    private val TAG = "TextDiffUtils"
    private val LONG_TEXT_THRESHOLD = 5000
    private val gson: Gson = GsonBuilder().create()
    
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
            // 快速检查：如果文本完全相同，直接返回空差异
            if (oldText == newText) {
                return "[]"
            }
            
            // 快速检查：如果差异很小，使用简化算法
            if (Math.abs(oldText.length - newText.length) < 100 && oldText.length > 1000) {
                return generateFastDiff(oldText, newText)
            }
            
            val diffs = computeDiffs(oldText, newText)
            return gson.toJson(diffs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate diff: ${e.message}")
            return ""
        }
    }
    
    /**
     * 快速差异生成算法，适用于差异很小的情况
     */
    private fun generateFastDiff(oldText: String, newText: String): String {
        val minLength = Math.min(oldText.length, newText.length)
        var start = 0
        var end = 0
        
        // 找到前缀相同的部分
        while (start < minLength && oldText[start] == newText[start]) {
            start++
        }
        
        // 找到后缀相同的部分
        while (end < minLength - start && oldText[oldText.length - 1 - end] == newText[newText.length - 1 - end]) {
            end++
        }
        
        val diffs = mutableListOf<Diff>()
        
        // 处理中间不同的部分
        if (start < oldText.length - end) {
            diffs.add(Diff(DiffType.DELETE, start, oldText.substring(start, oldText.length - end)))
        }
        
        if (start < newText.length - end) {
            diffs.add(Diff(DiffType.INSERT, start, newText.substring(start, newText.length - end)))
        }
        
        return gson.toJson(diffs)
    }
    
    /**
     * 应用差异到原文本
     */
    fun applyDiff(originalText: String, diffData: String): String {
        try {
            if (diffData.isEmpty() || diffData == "[]") {
                return originalText
            }
            
            val diffsType = object : TypeToken<List<Diff>>() {}.type
            val diffs: List<Diff> = gson.fromJson(diffData, diffsType)
            return applyDiffs(originalText, diffs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply diff: ${e.message}")
            return originalText
        }
    }
    
    /**
     * 使用Myers差分算法计算两个字符串之间的差异（线性空间优化版）
     */
    private fun computeDiffs(oldText: String, newText: String): List<Diff>
    {
        val n = oldText.length
        val m = newText.length
        val max = n + m
        
        // 前向搜索
        val forwardV = IntArray(2 * max + 1)
        val forwardTrace = mutableListOf<IntArray>()
        
        for (d in 0..max) {
            val currentV = IntArray(2 * max + 1)
            
            for (k in -d..d step 2) {
                var x: Int
                val kIndex = k + max
                
                if (k == -d || (k != d && forwardV[kIndex - 1] < forwardV[kIndex + 1])) {
                    x = forwardV[kIndex + 1]
                } else {
                    x = forwardV[kIndex - 1] + 1
                }
                
                var y = x - k
                
                // 跳过连续相同的字符
                while (x < n && y < m && oldText[x] == newText[y]) {
                    x++
                    y++
                }
                
                currentV[kIndex] = x
                
                if (x >= n && y >= m) {
                    return buildDiffListLinear(oldText, newText, forwardTrace + listOf(currentV))
                }
            }
            
            forwardV.forEachIndexed { index, value -> currentV[index] = value }
            forwardTrace.add(currentV)
        }
        
        return emptyList()
    }
    
    /**
     * 构建差异列表（线性空间优化版）
     */
    private fun buildDiffListLinear(oldText: String, newText: String, trace: List<IntArray>): List<Diff>
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
     * 应用差异列表到原文本（优化版）
     */
    private fun applyDiffs(original: String, diffs: List<Diff>): String {
        if (diffs.isEmpty()) {
            return original
        }
        
        val sb = StringBuilder(original)
        var offset = 0
        
        // 预分配足够的容量以减少扩容次数
        val totalLength = original.length + diffs.sumBy { 
            when (it.type) {
                DiffType.INSERT -> it.text.length
                DiffType.DELETE -> -it.text.length
                else -> 0
            }
        }
        sb.ensureCapacity(totalLength)
        
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