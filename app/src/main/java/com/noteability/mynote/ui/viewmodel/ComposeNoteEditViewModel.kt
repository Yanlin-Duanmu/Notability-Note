package com.noteability.mynote.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noteability.mynote.data.entity.Note
import com.noteability.mynote.data.entity.Tag
import com.noteability.mynote.data.repository.NoteRepository
import com.noteability.mynote.data.repository.TagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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
    val isAiGenerating: Boolean = false
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
        
        // Open panel first
        _uiState.update {
            it.copy(
                isAiSummaryVisible = true,
                aiSummaryContent = "",
                isAiGenerating = true
            )
        }
        
        // Mock streaming generation
        viewModelScope.launch {
            val mockSummary = "这是一段关于笔记的智能摘要。它展示了笔记的主要内容，帮助用户快速回顾。\n\n" +
                    "1. 关键点一：使用 Compose 构建现代化 UI。\n" +
                    "2. 关键点二：所见即所得的 Markdown 编辑体验。\n" +
                    "3. 关键点三：集成 AI 辅助功能，提升效率。\n\n" +
                    "总体来说，这个应用旨在提供优雅且强大的笔记记录体验。随着内容的增加，摘要也会相应变长，以测试滚动效果。"
            
            val chunks = mockSummary.split("") // Split by char for smooth streaming
            
            for (char in chunks) {
                if (!_uiState.value.isAiSummaryVisible) break // Stop if closed
                delay(30) // Simulate network delay
                _uiState.update { it.copy(aiSummaryContent = it.aiSummaryContent + char) }
            }
            
            _uiState.update { it.copy(isAiGenerating = false) }
        }
    }

    fun closeAiSummary() {
        _uiState.update { it.copy(isAiSummaryVisible = false, isAiGenerating = false) }
    }
}