package com.noteability.mynote.ui.viewmodel

import android.util.Log
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
    val showAiTagsDialog: Boolean = false
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
        _uiState.update { it.copy(title = newTitle) }
    }

    fun updateContent(newContent: String) {
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

                if (noteId != null && originalNote != null) {
                    // Update existing note - check for changes
                    if (originalNote?.title != title) {
                        noteRepository.updateNoteTitle(noteId, title)
                    }
                    if (originalNote?.content != content) {
                        noteRepository.updateNoteContent(noteId, content)
                    }
                    if (originalNote?.tagId != tagIdToUse) {
                        noteRepository.updateNoteTag(noteId, tagIdToUse)
                    }
                } else {
                    // Create new note or full update
                    val note = Note(
                        noteId = noteId ?: 0,
                        userId = loggedInUserId,
                        tagId = tagIdToUse,
                        title = title,
                        content = content,
                        createdAt = if (noteId == null) System.currentTimeMillis() else originalNote?.createdAt ?: System.currentTimeMillis(),
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
                            Message("user", "请对以下文本进行精简摘要，控制在200字以内：\n\n$content")
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
                                    } catch (e: Exception) {
                                        // Skip invalid JSON chunks
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

        val existingTags = _uiState.value.allTags.map { it.name }.joinToString(", ")

        _uiState.update { it.copy(isAiGenerating = true) }

        viewModelScope.launch {
            try {
                val prompt = """
                    文本内容：
                    $content
                    
                    现有标签库：[$existingTags]
                    
                    请从标签库中选择最匹配的标签，如果都不匹配，生成新标签。
                    请仅输出标签，用英文逗号分隔，不要包含任何其他文字，至多 3 个标签。
                    例如：Android, AI, Coding
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
}