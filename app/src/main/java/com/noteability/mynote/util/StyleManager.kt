// StyleManager.kt
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.graphics.Typeface
import com.google.gson.Gson

object StyleManager {
    private val gson = Gson()

    /**
     * 从 Spannable 提取样式信息
     */
    fun extractStylesFromSpannable(spannable: Spannable): NoteStyleData {
        val textStyles = mutableListOf<TextStyle>()

        // 提取加粗样式
        val boldSpans = spannable.getSpans(0, spannable.length, StyleSpan::class.java)
        boldSpans.forEach { span ->
            if (span.style == Typeface.BOLD || span.style == Typeface.BOLD_ITALIC) {
                textStyles.add(TextStyle(
                    start = spannable.getSpanStart(span),
                    end = spannable.getSpanEnd(span),
                    styleType = "bold"
                ))
            }
        }

        // 提取斜体样式
        val italicSpans = spannable.getSpans(0, spannable.length, StyleSpan::class.java)
        italicSpans.forEach { span ->
            if (span.style == Typeface.ITALIC || span.style == Typeface.BOLD_ITALIC) {
                textStyles.add(TextStyle(
                    start = spannable.getSpanStart(span),
                    end = spannable.getSpanEnd(span),
                    styleType = "italic"
                ))
            }
        }

        // 提取下划线样式
        val underlineSpans = spannable.getSpans(0, spannable.length, UnderlineSpan::class.java)
        underlineSpans.forEach { span ->
            textStyles.add(TextStyle(
                start = spannable.getSpanStart(span),
                end = spannable.getSpanEnd(span),
                styleType = "underline"
            ))
        }

        return NoteStyleData(textStyles)
    }

    /**
     * 将样式信息转换为 JSON
     */
    fun stylesToJson(styleData: NoteStyleData): String {
        return gson.toJson(styleData)
    }

    /**
     * 从 JSON 解析样式信息
     */
    fun jsonToStyles(json: String): NoteStyleData {
        return if (json.isNotEmpty()) {
            gson.fromJson(json, NoteStyleData::class.java)
        } else {
            NoteStyleData()
        }
    }

    /**
     * 将样式应用到文本
     */
    fun applyStylesToText(text: String, styleData: NoteStyleData): SpannableString {
        val spannable = SpannableString(text)

        styleData.textStyles.forEach { textStyle ->
            when (textStyle.styleType) {
                "bold" -> {
                    spannable.setSpan(
                        StyleSpan(Typeface.BOLD),
                        textStyle.start.coerceAtMost(text.length),
                        textStyle.end.coerceAtMost(text.length),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                "italic" -> {
                    spannable.setSpan(
                        StyleSpan(Typeface.ITALIC),
                        textStyle.start.coerceAtMost(text.length),
                        textStyle.end.coerceAtMost(text.length),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                "underline" -> {
                    spannable.setSpan(
                        UnderlineSpan(),
                        textStyle.start.coerceAtMost(text.length),
                        textStyle.end.coerceAtMost(text.length),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }

        return spannable
    }
}