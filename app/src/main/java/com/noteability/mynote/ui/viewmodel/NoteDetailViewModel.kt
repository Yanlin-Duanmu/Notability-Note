package com.noteability.mynote.ui.viewmodel

import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.graphics.Color
import com.noteability.mynote.data.entity.Note
import com.noteability.mynote.data.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    // --- StateFlows for Search ---
    private val _searchQueryInNote = MutableStateFlow("")
    val searchQueryInNote: StateFlow<String> = _searchQueryInNote.asStateFlow()

    private val _isSearchViewVisible = MutableStateFlow(false)
    val isSearchViewVisible: StateFlow<Boolean> = _isSearchViewVisible.asStateFlow()

    private val _originalContent = MutableStateFlow("")

    private val _matchRanges = MutableStateFlow<List<IntRange>>(emptyList())
    val matchRanges: StateFlow<List<IntRange>> = _matchRanges.asStateFlow()

    private val _currentMatchIndex = MutableStateFlow(-1)
    val currentMatchIndex: StateFlow<Int> = _currentMatchIndex.asStateFlow()
    



    
    
    

    // 加载笔记详情
    fun loadNote(noteId: Long) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                // 使用仓库的getNoteById方法，该方法已经被实现为返回完整内容
                // 包括应用所有差分版本后的长文本内容
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
    
    // 单个字段更新方法
    fun updateNoteTitle(noteId: Long, title: String) {
        _isLoading.value = true
        _error.value = null
        _isSaved.value = false
        
        viewModelScope.launch {
            try {
                noteRepository.updateNoteTitle(noteId, title)
                _isSaved.value = true
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "更新标题失败"
                _isLoading.value = false
            }
        }
    }
    
    fun updateNoteContent(noteId: Long, content: String) {
        _isLoading.value = true
        _error.value = null
        _isSaved.value = false
        
        viewModelScope.launch {
            try {
                noteRepository.updateNoteContent(noteId, content)
                _isSaved.value = true
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "更新内容失败"
                _isLoading.value = false
            }
        }
    }
    
    fun updateNoteTag(noteId: Long, tagId: Long) {
        _isLoading.value = true
        _error.value = null
        _isSaved.value = false
        
        viewModelScope.launch {
            try {
                noteRepository.updateNoteTag(noteId, tagId)
                _isSaved.value = true
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "更新标签失败"
                _isLoading.value = false
            }
        }
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
    val highlightedContent: Flow<Spannable> = combine(
        _originalContent,
        searchQueryInNote,
        matchRanges,
        currentMatchIndex
    ) { content, query, ranges, currentIndex ->
        val spannable = SpannableString(content)
        if (query.isNotBlank() && ranges.isNotEmpty()) {
            ranges.forEachIndexed { index, range ->
                val color = if (index == currentIndex) {
                    Color.YELLOW // 当前匹配项
                } else {
                    Color.LTGRAY // 其他匹配项
                }
                spannable.setSpan(
                    BackgroundColorSpan(color),
                    range.first,
                    range.last + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        spannable
    }.stateIn(viewModelScope,
        SharingStarted.WhileSubscribed(5000), SpannableString(""))
    // --- Public Functions for Search ---

    fun onNoteContentChanged(content: String) {
        _originalContent.value = content
        updateMatches()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQueryInNote.value = query
        updateMatches()
    }

    fun toggleSearchView() {
        val isVisible = !_isSearchViewVisible.value
        _isSearchViewVisible.value = isVisible
        if (!isVisible) {
            // 关闭搜索时，清空搜索词
            onSearchQueryChanged("")
        }
    }

    fun nextMatch() {
        if (_matchRanges.value.isNotEmpty()) {
            _currentMatchIndex.value = (_currentMatchIndex.value + 1) % _matchRanges.value.size
        }
    }

    fun previousMatch() {
        if (_matchRanges.value.isNotEmpty()) {
            _currentMatchIndex.value = if (_currentMatchIndex.value > 0) {
                _currentMatchIndex.value - 1
            } else {
                _matchRanges.value.size - 1
            }
        }
    }

    private fun updateMatches() {
        val content = _originalContent.value
        val query = _searchQueryInNote.value
        if (query.isBlank()) {
            _matchRanges.value = emptyList()
            _currentMatchIndex.value = -1
            return
        }

        val ranges = mutableListOf<IntRange>()
        var index = content.indexOf(query, 0, ignoreCase = true)
        while (index != -1) {
            ranges.add(index until (index + query.length))
            index = content.indexOf(query, index + 1, ignoreCase = true)
        }
        _matchRanges.value = ranges
        _currentMatchIndex.value = if (ranges.isEmpty()) -1 else 0
    }
}