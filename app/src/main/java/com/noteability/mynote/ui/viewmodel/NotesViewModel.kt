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
    
    init {
        loadNotes()
    }
    
    // 加载所有笔记
    fun loadNotes() {
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
    fun searchNotes(query: String) {
        currentSearchQuery = query
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                noteRepository.searchNotes(query).collect { searchResults ->
                    // 如果有选择的标签，还要根据标签过滤
                    val filteredResults = if (currentTagId != null) {
                        searchResults.filter { it.tagId == currentTagId }
                    } else {
                        searchResults
                    }
                    _notes.value = filteredResults
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "搜索笔记失败"
                _isLoading.value = false
            }
        }
    }
    
    // 重置筛选和搜索条件
    fun resetFilters() {
        currentTagId = null
        currentSearchQuery = ""
        loadNotes()
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