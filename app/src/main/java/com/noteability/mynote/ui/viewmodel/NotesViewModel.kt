package com.noteability.mynote.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noteability.mynote.data.entity.Note
import com.noteability.mynote.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotesViewModel(private val noteRepository: NoteRepository) : ViewModel() {
    
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    private var currentTagId: Long? = null
    private var currentSearchQuery: String = ""
    private var loggedInUserId: Long = 1L // 默认ID，在实际登录后会被更新
    
    // 设置当前登录用户ID
    fun setLoggedInUserId(userId: Long) {
        loggedInUserId = userId
        // 更新仓库中的用户ID
        if (noteRepository is com.noteability.mynote.data.repository.impl.NoteRepositoryImpl) {
            noteRepository.updateCurrentUserId(userId)
        }
        // 重新加载笔记，确保只显示当前用户的笔记
        loadNotes()
    }
    
    init {
        loadNotes()
    }
    
    // 加载所有笔记
    fun loadNotes() {
        currentTagId = null // 重置标签筛选
        currentSearchQuery = ""
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                noteRepository.getAllNotes().collect { notes ->
                    _notes.value = notes
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "加载笔记失败"
                _isLoading.value = false
            }
        }
    }
    
    // 根据标签加载笔记
    fun loadNotesByTag(tagId: Long) {
        currentTagId = tagId
        currentSearchQuery = ""
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                noteRepository.getNotesByTagId(tagId).collect { notes ->
                    _notes.value = notes
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "加载笔记失败"
                _isLoading.value = false
            }
        }
    }
    
    // 搜索笔记
    fun searchNotes(query: String, tagId: Long) {
        currentSearchQuery = query
        val searchTagId = if (tagId == 0L) null else tagId
        currentTagId = searchTagId

        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                noteRepository.searchNotes(query, searchTagId).collect { searchResults ->
                    _notes.value = searchResults
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "搜索笔记失败"
                _isLoading.value = false
            }
        }
    }
    
    // 保存笔记
    fun saveNote(note: Note) {
        viewModelScope.launch {
            try {
                noteRepository.saveNote(note)
                // 保存成功后重新加载笔记
                if (currentTagId != null) {
                    loadNotesByTag(currentTagId!!)
                } else {
                    loadNotes()
                }
            } catch (e: Exception) {
                _error.value = "保存笔记失败"
            }
        }
    }
    
    // 删除笔记
    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            try {
                noteRepository.deleteNote(noteId)
                // 删除成功后重新加载笔记
                if (currentTagId != null) {
                    loadNotesByTag(currentTagId!!)
                } else {
                    loadNotes()
                }
            } catch (e: Exception) {
                _error.value = "删除笔记失败"
            }
        }
    }
}