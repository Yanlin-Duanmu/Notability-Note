package com.noteability.mynote.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noteability.mynote.data.entity.Note
import com.noteability.mynote.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NoteDetailViewModel(private val noteRepository: NoteRepository) : ViewModel() {
    
    // 当前登录用户ID
    private var loggedInUserId = 0L
    
    private val _note = MutableStateFlow<Note?>(null)
    val note: StateFlow<Note?> = _note
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved
    
    // 加载笔记详情
    fun loadNote(noteId: Long) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                noteRepository.getNoteById(noteId).collect { note ->
                    _note.value = note
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "加载笔记失败"
                _isLoading.value = false
            }
        }
    }
    
    // 保存笔记（新建或更新）
    fun saveNote(note: Note) {
        _isLoading.value = true
        _error.value = null
        _isSaved.value = false
        
        viewModelScope.launch {
            try {
                if (note.noteId == 0L || note.noteId == -1L) {
                    // 新建笔记
                    noteRepository.saveNote(note)
                } else {
                    // 更新笔记
                    noteRepository.updateNote(note)
                }
                _isSaved.value = true
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "保存笔记失败"
                _isLoading.value = false
            }
        }
    }
    
    // 删除笔记
    fun deleteNote(noteId: Long) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                noteRepository.deleteNote(noteId)
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "删除笔记失败"
                _isLoading.value = false
            }
        }
    }
    
    // 重置保存状态
    fun resetSaveState() {
        _isSaved.value = false
    }
    
    // 设置当前登录用户ID
    fun setLoggedInUserId(userId: Long) {
        loggedInUserId = userId
        // 更新仓库中的用户ID
        if (noteRepository is com.noteability.mynote.data.repository.impl.NoteRepositoryImpl) {
            noteRepository.updateCurrentUserId(userId)
        }
    }
    
    // 创建新的空笔记
    fun createNewNote(): Note {
        return Note(noteId = 0, userId = loggedInUserId, tagId = 0, title = "", content = "")
    }
}