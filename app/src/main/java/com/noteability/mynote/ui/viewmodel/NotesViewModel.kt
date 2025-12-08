package com.noteability.mynote.ui.viewmodel

import androidx.compose.ui.semantics.text
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import androidx.paging.map
import com.noteability.mynote.data.entity.Note
import com.noteability.mynote.data.repository.NoteRepository
import com.noteability.mynote.ui.adapter.SearchSuggestion
import com.noteability.mynote.ui.adapter.SearchSuggestionType
import com.noteability.mynote.ui.search.SearchHistoryManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class NotesViewModel(
    private val noteRepository: NoteRepository,
    private val searchHistoryManager: SearchHistoryManager
) : ViewModel() {


    // 保留搜索建议状态
    private val _suggestions = MutableStateFlow<List<SearchSuggestion>>(emptyList())
    val suggestions: StateFlow<List<SearchSuggestion>> = _suggestions


    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // 筛选条件状态 (改为 Flow 以驱动 Paging)
    private val _tagId = MutableStateFlow<Long?>(null) // [修改] 默认 null 代表全部
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // 用户 ID 状态
    private val _loggedInUserId = MutableStateFlow(1L)

    // 定义 PagingData 流
    // 这是一个响应式流，当 用户ID、搜索词 或 标签ID 变化时，自动触发数据库分页查询
    @OptIn(ExperimentalCoroutinesApi::class)
    val notesPagingFlow: Flow<PagingData<Note>> = combine(
        _loggedInUserId,
        _searchQuery,
        _tagId
    ) { userId, query, tagId ->
        Triple(userId, query, tagId)
    }.flatMapLatest { (userId, query, tagId) ->

        Pager(
            config = PagingConfig(
                pageSize = 20,          // 每页加载 20 条
                enablePlaceholders = false,
                initialLoadSize = 20
            ),
            pagingSourceFactory = {
                // 调用 Repository 的分页接口
                noteRepository.getNotesPagingSource(userId, query, tagId)
            }
        ).flow
    }.cachedIn(viewModelScope)


    // 设置当前登录用户ID
    fun setLoggedInUserId(userId: Long) {
        _loggedInUserId.value = userId
        if (noteRepository is com.noteability.mynote.data.repository.impl.NoteRepositoryImpl) {
            noteRepository.updateCurrentUserId(userId)
        }
    }

    // 加载所有笔记
    fun loadNotes() {
        _tagId.value = null // null 代表全部
        _searchQuery.value = ""
        _error.value = null
    }

    // 根据标签加载笔记
    fun loadNotesByTag(tagId: Long) {
        _tagId.value = tagId
        _searchQuery.value = ""
        _error.value = null
    }

    // 搜索笔记
    fun searchNotes(query: String, tagId: Long) {
        _searchQuery.value = query
        // 如果 tagId 是 0，转为 null 表示搜索全部标签
        _tagId.value = if (tagId == 0L) null else tagId
    }




    fun loadSuggestions(query: String) {
        // 如果输入为空，直接清空智能推荐，不做任何事
        if (query.isBlank()) {
            _suggestions.value = emptyList()
            return
        }

        // 如果有输入，则加载智能推荐
        viewModelScope.launch {
            try {
                val suggestionsSource = noteRepository.getNotesPagingSource(_loggedInUserId.value, query, _tagId.value)
                val loadResult = suggestionsSource.load(
                    PagingSource.LoadParams.Refresh(key = null, loadSize = 5, placeholdersEnabled = false)
                )

                if (loadResult is PagingSource.LoadResult.Page) {
                    val titleSuggestions = loadResult.data
                        .map { note -> SearchSuggestion(note.title, SearchSuggestionType.SUGGESTION) }
                        .distinctBy { it.text }
                    _suggestions.value = titleSuggestions
                } else {
                    _suggestions.value = emptyList()
                }
            } catch (e: Exception) {
                _suggestions.value = emptyList() // 出错时也清空
            }
        }
    }

    fun getSearchHistory(): List<String> {
        return searchHistoryManager.getSearchHistory()
    }

    /**
     * 保存一条搜索历史。
     */
    fun saveSearchToHistory(query: String) {
        searchHistoryManager.addSearchQuery(query) // 调用新的添加方法
    }

    fun deleteSearchFromHistory(query: String) {
        searchHistoryManager.removeSearchQuery(query) // 调用新的删除方法
    }


    // -------------------------------------------------------------
    // 增删改操作
    // -------------------------------------------------------------

    // 保存笔记
    fun saveNote(note: Note) {
        viewModelScope.launch {
            try {
                noteRepository.saveNote(note)
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
            } catch (e: Exception) {
                _error.value = "删除笔记失败"
            }
        }
    }
}

