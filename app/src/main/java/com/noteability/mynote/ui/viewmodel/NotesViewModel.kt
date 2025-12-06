package com.noteability.mynote.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noteability.mynote.data.entity.Note
import com.noteability.mynote.data.repository.NoteRepository
import com.noteability.mynote.ui.adapter.SearchSuggestion
import com.noteability.mynote.ui.search.SearchHistoryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.noteability.mynote.ui.adapter.SearchSuggestionType
import kotlinx.coroutines.flow.first

private const val WHILE_SUBSCRIBED_TIMEOUT = 5000L

class NotesViewModel(private val noteRepository: NoteRepository, private val searchHistoryManager: SearchHistoryManager) : ViewModel() {
    
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes
    private val _suggestions = MutableStateFlow<List<SearchSuggestion>>(emptyList())
    val suggestions: StateFlow<List<SearchSuggestion>> = _suggestions
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _tagId = MutableStateFlow(0L)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val searchResults: StateFlow<List<Note>> = combine(_searchQuery, _tagId) { query, tagId -> query to tagId }
        .debounce(300)
        .flatMapLatest { (query, tagId) ->
            if (query.isBlank()) {
                if (tagId == 0L) {
                    noteRepository.getAllNotes()
                } else {
                    noteRepository.getNotesByTagId(tagId)
                }
            } else {
                if (tagId == 0L) {
                    noteRepository.searchNotes(query)
                } else {
                    noteRepository.searchNotes(query, tagId)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(WHILE_SUBSCRIBED_TIMEOUT),
            initialValue = emptyList()
        )

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
        _tagId.value = 0L
        _searchQuery.value = ""
    }
    
    // 根据标签加载笔记
    fun loadNotesByTag(tagId: Long) {
        _tagId.value = tagId
        _searchQuery.value = ""
    }
    
    // 搜索笔记
    fun searchNotes(query: String, tagId: Long) {
        _searchQuery.value = query
        _tagId.value = tagId
    }
    
    fun saveSearchToHistory(query: String) {
        searchHistoryManager.saveSearchQuery(query)
    }

    fun getSearchHistory(): List<String> {
        return searchHistoryManager.getSearchHistory()
    }

    // 保存笔记
    fun saveNote(note: Note) {
        viewModelScope.launch {
            try {
                noteRepository.saveNote(note)
                // 保存成功后重新加载笔记
                loadNotes()
            } catch (e: Exception) {
                _error.value = "保存笔记失败"
            }
        }
    }
    fun loadSuggestions(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                // 输入为空，显示历史记录
                val history = searchHistoryManager.getSearchHistory().map {
                    SearchSuggestion(it, SearchSuggestionType.HISTORY)
                }
                _suggestions.value = history
            } else {
                // 有输入，显示智能建议 (我们假设Repository有一个获取建议的方法)
                // 如果您还没实现，我将提供一个模拟实现
                val titleSuggestions = noteRepository.searchNotes(query)
                    .first() // 获取Flow的第一个值
                    .map { note -> SearchSuggestion(note.title, SearchSuggestionType.SUGGESTION) }
                    .distinct() // 去重
                    .take(5) // 最多取5条
                _suggestions.value = titleSuggestions
            }
        }
    }

    // 3. 添加一个方法来处理删除历史
    fun deleteSearchFromHistory(query: String) {
        searchHistoryManager.removeSearchQuery(query) // 假设SearchHistoryManager有此方法
        loadSuggestions("") // 重新加载历史记录以刷新UI
    }
    // 删除笔记
    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            try {
                noteRepository.deleteNote(noteId)
                // 删除成功后重新加载笔记
                loadNotes()
            } catch (e: Exception) {
                _error.value = "删除笔记失败"
            }
        }
    }
}