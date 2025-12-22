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
    val error: String? = null
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
}