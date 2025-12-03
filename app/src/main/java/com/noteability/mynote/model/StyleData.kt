// StyleData.kt
data class TextStyle(
    val start: Int,
    val end: Int,
    val styleType: String,  // "bold", "italic", "underline"
    val styleValue: Int = 0
)

data class NoteStyleData(
    val textStyles: List<TextStyle> = emptyList()
)