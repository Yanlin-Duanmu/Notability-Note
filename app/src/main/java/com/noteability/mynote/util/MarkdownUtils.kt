package com.noteability.mynote.utils

import android.widget.EditText
import android.widget.TextView
import com.noteability.mynote.MyNoteApplication
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin

object MarkdownUtils {

    // 创建 Markwon 实例
    fun createMarkwon(): Markwon {
        return Markwon.builder(MyNoteApplication.context)
            .usePlugin(HtmlPlugin.create())
            .build()

    }

    // 插入 Markdown 格式
    fun insertMarkdownFormat(editText: EditText, format: String) {
        val start = editText.selectionStart
        val end = editText.selectionEnd
        val text = editText.text

        when (format) {
            "bold" -> {
                // **加粗**
                if (start == end) {
                    text.insert(start, "****")
                    editText.setSelection(start + 2)
                } else {
                    val selectedText = text.substring(start, end)
                    text.replace(start, end, "**$selectedText**")
                    editText.setSelection(end + 4)
                }
            }
            "italic" -> {
                // *斜体*
                if (start == end) {
                    text.insert(start, "**")
                    editText.setSelection(start + 1)
                } else {
                    val selectedText = text.substring(start, end)
                    text.replace(start, end, "*$selectedText*")
                    editText.setSelection(end + 2)
                }
            }
            "underline" -> {
                // <u>下划线</u>
                if (start == end) {
                    text.insert(start, "<u></u>")
                    editText.setSelection(start + 3)
                } else {
                    val selectedText = text.substring(start, end)
                    text.replace(start, end, "<u>$selectedText</u>")
                    editText.setSelection(end + 7)
                }
            }
            "bullet" -> {
                // - 列表
                text.insert(start, "- ")
                editText.setSelection(start + 2)
            }
            "numbered" -> {
                // 1. 列表
                text.insert(start, "1. ")
                editText.setSelection(start + 3)
            }
        }
    }

    // 渲染 Markdown
    fun renderMarkdown(textView: TextView, markdownText: String, markwon: Markwon) {
        if (markdownText.isNotEmpty()) {
            markwon.setMarkdown(textView, markdownText)
        } else {
            textView.text = ""
        }
    }
}