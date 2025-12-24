package com.noteability.mynote.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noteability.mynote.BuildConfig
import com.noteability.mynote.data.entity.Note
import com.noteability.mynote.data.entity.Tag
import com.noteability.mynote.data.repository.NoteRepository
import com.noteability.mynote.data.repository.TagRepository
import com.noteability.mynote.model.ChatRequest
import com.noteability.mynote.model.ChatResponse
import com.noteability.mynote.model.Message
import com.noteability.mynote.model.NetworkModule
import com.noteability.mynote.util.ImageStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * UI State for the Compose Note Edit Screen
 */
data class NoteEditUiState(
    val isLoading: Boolean = false,
    val noteId: Long? = null,
    val title: String = "",
    val content: String = "",
    val currentTag: Tag? = null,
    val allTags: List<Tag> = emptyList(),
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null,
    // AI Summary State
    val isAiSummaryVisible: Boolean = false,
    val aiSummaryContent: String = "",
    val isAiGenerating: Boolean = false,
    // AI Tagging State
    val suggestedTags: List<String> = emptyList(),
    val showAiTagsDialog: Boolean = false,
    // Local image insertion state (path, description)
    val pendingLocalImage: Pair<String, String>? = null
)

/**
 * ViewModel for the Compose Note Edit Screen.
 * Handles logic for loading, saving, and updating notes.
 */
class ComposeNoteEditViewModel(
    private val noteRepository: NoteRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditUiState())
    val uiState: StateFlow<NoteEditUiState> = _uiState.asStateFlow()

    // Cached original note for change detection
    private var originalNote: Note? = null
    private var loggedInUserId: Long = 1L
    private val apiKey = "Bearer ${BuildConfig.OPENAI_API_KEY}"

    fun setLoggedInUserId(id: Long) {
        loggedInUserId = id
        noteRepository.setCurrentUserId(id)
        tagRepository.setCurrentUserId(id)
    }

    fun loadData(noteId: Long) {
        _uiState.update { it.copy(isLoading = true, noteId = if (noteId == -1L) null else noteId) }
        
        viewModelScope.launch {
            try {
                // Load Tags
                tagRepository.getAllTags().collect { tags ->
                     // Sort: "Uncategorized" (未归档) first
                    val sortedTags = tags.sortedWith(compareBy {
                        if (it.name == "未归档" || it.name == "未分类") "0" else it.name
                    })
                    
                    _uiState.update { it.copy(allTags = sortedTags) }
                    
                    // Load Note if ID exists
                    if (noteId != -1L) {
                         noteRepository.getNoteById(noteId).collect { note ->
                            note?.let { n ->
                                originalNote = n
                                // Find the tag object from the loaded tags list
                                val tag = sortedTags.find { it.tagId == n.tagId } 
                                    ?: sortedTags.firstOrNull() // Fallback
                                
                                _uiState.update { state ->
                                    state.copy(
                                        title = n.title,
                                        content = n.content,
                                        currentTag = tag,
                                        isLoading = false
                                    )
                                }
                            }
                        }
                    } else {
                        // New Note
                        val defaultTag = sortedTags.find { it.name == "未归档" || it.name == "未分类" } 
                            ?: sortedTags.firstOrNull()
                            
                        _uiState.update { it.copy(isLoading = false, currentTag = defaultTag) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isLoading = false, error = "加载数据失败: ${e.message}") 
                }
            }
        }
    }

    fun updateTitle(newTitle: String) {
        if (_uiState.value.title == newTitle) return
        _uiState.update { it.copy(title = newTitle) }
    }

    fun updateContent(newContent: String) {
        if (_uiState.value.content == newContent) return
        _uiState.update { it.copy(content = newContent) }
    }

    fun updateTag(newTag: Tag) {
        _uiState.update { it.copy(currentTag = newTag) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun saveNote() {
        val currentState = _uiState.value
        val title = currentState.title.trim()
        val content = currentState.content.trim()

        if (title.isEmpty()) {
            _uiState.update { it.copy(error = "标题为空，请添加一个标题！") }
            return
        }
        
        if (title.isEmpty() && content.isEmpty()) {
             // Nothing to save
             return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                var tagIdToUse = currentState.currentTag?.tagId

                // Logic to handle "Uncategorized" tag creation if missing
                if (tagIdToUse == null) {
                    var uncategorizedTag = tagRepository.getTagByName(loggedInUserId, "未分类")
                    if (uncategorizedTag == null) {
                        uncategorizedTag = tagRepository.getTagByName(loggedInUserId, "未归档")
                    }

                    if (uncategorizedTag == null) {
                        val newTag = Tag(
                            tagId = 0,
                            userId = loggedInUserId,
                            name = "未分类",
                            noteCount = 0
                        )
                        tagRepository.saveTag(newTag)
                        uncategorizedTag = tagRepository.getTagByName(loggedInUserId, "未分类")
                    }
                    tagIdToUse = uncategorizedTag?.tagId ?: 1
                }

                val noteId = currentState.noteId

                val cached = originalNote
                if (noteId != null && cached != null) {
                    // Partial update: only changed fields
                    if (cached.title != title) {
                        noteRepository.updateNoteTitle(noteId, title)
                    }
                    if (cached.content != content) {
                        noteRepository.updateNoteContent(noteId, content)
                    }
                    if (cached.tagId != tagIdToUse) {
                        noteRepository.updateNoteTag(noteId, tagIdToUse)
                    }
                } else {
                    // New note creation
                    val note = Note(
                        noteId = noteId ?: 0,
                        userId = loggedInUserId,
                        tagId = tagIdToUse,
                        title = title,
                        content = content,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )

                    if (noteId == null || noteId == 0L) {
                        noteRepository.saveNote(note)
                    } else {
                        noteRepository.updateNote(note)
                    }
                }
                
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isLoading = false, error = "保存笔记失败: ${e.message}") 
                }
            }
        }
    }

    fun deleteNote() {
        val noteId = _uiState.value.noteId
        if (noteId != null) {
             viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                try {
                    noteRepository.deleteNote(noteId)
                    _uiState.update { it.copy(isLoading = false, isDeleted = true) }
                } catch (e: Exception) {
                    _uiState.update { 
                        it.copy(isLoading = false, error = "删除笔记失败: ${e.message}") 
                    }
                }
            }
        }
    }
    
    fun resetSaveState() {
        _uiState.update { it.copy(isSaved = false) }
    }

    fun hasUnsavedChanges(): Boolean {
        val currentState = _uiState.value
        val original = originalNote
            ?: return currentState.title.isNotBlank() || currentState.content.isNotBlank()
        
        // Existing note: compare with original
        return currentState.title != original.title ||
               currentState.content != original.content ||
               currentState.currentTag?.tagId != original.tagId
    }

    fun triggerAiSummary() {
        if (_uiState.value.isAiGenerating) return

        val content = _uiState.value.content
        if (content.isBlank()) {
            _uiState.update { it.copy(error = "笔记内容为空，无法生成摘要") }
            return
        }

        // Open panel first
        _uiState.update {
            it.copy(
                isAiSummaryVisible = true,
                aiSummaryContent = "",
                isAiGenerating = true
            )
        }

        viewModelScope.launch {
            try {
                val responseBody = NetworkModule.api.chatStream(
                    token = apiKey,
                    request = ChatRequest(
                        model = "qwen3-max",
                        messages = listOf(
                            Message("system", "你是一个专业的摘要助手。"),
                            Message("user", """
                                    请对以下文本进行摘要。

                                    【硬性格式要求】：
                                    1. 纯文本排版：禁止使用任何 Markdown 语法（如 **、#、-、> 等）。
                                    2. 数字列表：如需分条，仅使用 "1. "、"2. " 格式。
                                    3. 强制空行：【重点】每段文字结束、每个列表项结束，必须连敲回车，使段落之间完全隔开一个空白行。
                                    4. 字数：200字以内。

                                    【待摘要内容】：
                                    $content
                                    """.trimIndent()),
                        ),
                        stream = true
                    )
                )

                withContext(Dispatchers.IO) {
                    val json = Json { ignoreUnknownKeys = true }
                    
                    responseBody.byteStream().bufferedReader().use { reader ->
                        while (true) {
                            val line = reader.readLine() ?: break
                            
                            if (!_uiState.value.isAiSummaryVisible) break
                            
                            val trimmedLine = line.trim()
                            if (trimmedLine.startsWith("data:")) {
                                val data = trimmedLine.removePrefix("data:").trim()
                                
                                if (data == "[DONE]") break
                                
                                if (data.isNotEmpty()) {
                                    try {
                                        val chunk = json.decodeFromString<ChatResponse>(data)
                                        val delta = chunk.choices.firstOrNull()?.delta?.content
                                        
                                        if (delta != null) {
                                            _uiState.update {
                                                it.copy(aiSummaryContent = it.aiSummaryContent + delta)
                                            }
                                        }
                                    } catch (_: Exception) {
                                        // Skip malformed JSON chunks
                                    }
                                }
                            }
                        }
                    }
                }
                
                _uiState.update { it.copy(isAiGenerating = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        aiSummaryContent = "生成失败: ${e.message}",
                        isAiGenerating = false
                    )
                }
            }
        }
    }

    fun closeAiSummary() {
        _uiState.update { it.copy(isAiSummaryVisible = false, isAiGenerating = false) }
    }

    fun triggerAiTagging() {
        if (_uiState.value.isAiGenerating) return

        val content = _uiState.value.content
        if (content.isBlank()) {
            _uiState.update { it.copy(error = "笔记内容为空，无法生成标签") }
            return
        }

        val existingTags = _uiState.value.allTags.joinToString(", ") { it.name }

        _uiState.update { it.copy(isAiGenerating = true) }

        viewModelScope.launch {
            try {
                val prompt = """
                    文本内容：
                    $content
                    
                    现有标签库：[$existingTags]
                    
                    请从标签库中选择最匹配的标签，如果都不匹配，生成新标签。
                    请仅输出标签，语言和文本内容保持一致，用英文逗号分隔，不要包含任何其他文字，至多 3 个标签。
                    例如：学习资料, Kotlin, 娱乐
                """.trimIndent()

                val response = NetworkModule.api.chat(
                    token = apiKey,
                    request = ChatRequest(
                        model = "qwen3-max",
                        messages = listOf(
                            Message("system", "你是一个分类专家，只输出逗号分隔的标签。"),
                            Message("user", prompt)
                        )
                    )
                )
                val result = response.choices.firstOrNull()?.message?.content ?: ""
                val tagList = result.split(",", "，").map { it.trim() }.filter { it.isNotEmpty() }

                _uiState.update {
                    it.copy(
                        suggestedTags = tagList,
                        isAiGenerating = false,
                        showAiTagsDialog = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "生成标签失败: ${e.message}",
                        isAiGenerating = false
                    )
                }
            }
        }
    }

    fun closeAiTagsDialog() {
        _uiState.update { it.copy(showAiTagsDialog = false) }
    }

    fun applyAiTag(tagName: String) {
        viewModelScope.launch {
            try {
                // Check if tag exists in current list
                var tag = _uiState.value.allTags.find { it.name.equals(tagName, ignoreCase = true) }

                if (tag == null) {
                    // Create new tag
                    val newTag = Tag(
                        tagId = 0,
                        userId = loggedInUserId,
                        name = tagName,
                        noteCount = 0
                    )
                    tagRepository.saveTag(newTag)
                    tag = tagRepository.getTagByName(loggedInUserId, tagName)
                }

                if (tag != null) {
                    updateTag(tag)
                    closeAiTagsDialog()
                } else {
                    _uiState.update { it.copy(error = "无法应用标签: $tagName") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "应用标签失败: ${e.message}") }
            }
        }
    }
    
    fun processLocalImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            ImageStorageManager.saveImageFromUri(context, uri)
                .onSuccess { relativePath ->
                    val absolutePath = ImageStorageManager.getFileUri(context, relativePath)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pendingLocalImage = absolutePath to ""
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "保存图片失败: ${e.message}"
                        )
                    }
                }
        }
    }
    
    fun clearPendingLocalImage() {
        _uiState.update { it.copy(pendingLocalImage = null) }
    }
}