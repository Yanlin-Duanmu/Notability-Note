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
    
    // 耗时统计相关
    private var loadStartTime: Long = 0L
    private var dbQueryEndTime: Long = 0L
    
    // 获取加载开始时间
    fun getLoadStartTime(): Long = loadStartTime
    
    // 获取数据库查询完成时间
    fun getDbQueryEndTime(): Long = dbQueryEndTime
    
    // 加载笔记详情
    fun loadNote(noteId: Long) {
        loadStartTime = System.currentTimeMillis()
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                // 使用仓库的getNoteById方法，该方法已经被实现为返回完整内容
                // 包括应用所有差分版本后的长文本内容
                noteRepository.getNoteById(noteId).collect { note ->
                    dbQueryEndTime = System.currentTimeMillis()
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
        val viewModelStartTime = System.currentTimeMillis()
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
                
                val viewModelEndTime = System.currentTimeMillis()
                val viewModelElapsedTime = viewModelEndTime - viewModelStartTime
                
                // 打印ViewModel层保存操作耗时统计
                println("=== ViewModel 保存操作统计 ===")
                println("操作类型: ${if (note.noteId == 0L || note.noteId == -1L) "新建笔记" else "更新笔记"}")
                println("ViewModel 层总耗时: $viewModelElapsedTime ms")
                println("========================")
            } catch (e: Exception) {
                _error.value = "保存笔记失败"
                _isLoading.value = false
                
                val viewModelEndTime = System.currentTimeMillis()
                val viewModelElapsedTime = viewModelEndTime - viewModelStartTime
                
                // 打印错误情况下的耗时统计
                println("=== ViewModel 保存操作统计（错误） ===")
                println("操作类型: ${if (note.noteId == 0L || note.noteId == -1L) "新建笔记" else "更新笔记"}")
                println("ViewModel 层总耗时: $viewModelElapsedTime ms")
                println("错误信息: ${e.message}")
                println("==============================")
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
        val viewModelStartTime = System.currentTimeMillis()
        _isLoading.value = true
        _error.value = null
        _isSaved.value = false
        
        viewModelScope.launch {
            try {
                noteRepository.updateNoteTitle(noteId, title)
                _isSaved.value = true
                _isLoading.value = false
                
                val viewModelEndTime = System.currentTimeMillis()
                val viewModelElapsedTime = viewModelEndTime - viewModelStartTime
                
                // 打印ViewModel层更新操作耗时统计
                println("=== ViewModel 更新标题操作统计 ===")
                println("操作类型: 更新标题")
                println("笔记ID: $noteId")
                println("ViewModel 层总耗时: $viewModelElapsedTime ms")
                println("========================")
            } catch (e: Exception) {
                _error.value = "更新标题失败"
                _isLoading.value = false
                
                val viewModelEndTime = System.currentTimeMillis()
                val viewModelElapsedTime = viewModelEndTime - viewModelStartTime
                
                // 打印错误情况下的耗时统计
                println("=== ViewModel 更新标题操作统计（错误） ===")
                println("操作类型: 更新标题")
                println("笔记ID: $noteId")
                println("ViewModel 层总耗时: $viewModelElapsedTime ms")
                println("错误信息: ${e.message}")
                println("================================")
            }
        }
    }
    
    fun updateNoteContent(noteId: Long, content: String) {
        val viewModelStartTime = System.currentTimeMillis()
        _isLoading.value = true
        _error.value = null
        _isSaved.value = false
        
        viewModelScope.launch {
            try {
                noteRepository.updateNoteContent(noteId, content)
                _isSaved.value = true
                _isLoading.value = false
                
                val viewModelEndTime = System.currentTimeMillis()
                val viewModelElapsedTime = viewModelEndTime - viewModelStartTime
                
                // 打印ViewModel层更新操作耗时统计
                println("=== ViewModel 更新内容操作统计 ===")
                println("操作类型: 更新内容")
                println("笔记ID: $noteId")
                println("ViewModel 层总耗时: $viewModelElapsedTime ms")
                println("========================")
            } catch (e: Exception) {
                _error.value = "更新内容失败"
                _isLoading.value = false
                
                val viewModelEndTime = System.currentTimeMillis()
                val viewModelElapsedTime = viewModelEndTime - viewModelStartTime
                
                // 打印错误情况下的耗时统计
                println("=== ViewModel 更新内容操作统计（错误） ===")
                println("操作类型: 更新内容")
                println("笔记ID: $noteId")
                println("ViewModel 层总耗时: $viewModelElapsedTime ms")
                println("错误信息: ${e.message}")
                println("================================")
            }
        }
    }
    
    fun updateNoteTag(noteId: Long, tagId: Long) {
        val viewModelStartTime = System.currentTimeMillis()
        _isLoading.value = true
        _error.value = null
        _isSaved.value = false
        
        viewModelScope.launch {
            try {
                noteRepository.updateNoteTag(noteId, tagId)
                _isSaved.value = true
                _isLoading.value = false
                
                val viewModelEndTime = System.currentTimeMillis()
                val viewModelElapsedTime = viewModelEndTime - viewModelStartTime
                
                // 打印ViewModel层更新操作统计
                println("=== ViewModel 更新标签操作统计 ===")
                println("操作类型: 更新标签")
                println("笔记ID: $noteId")
                println("ViewModel 层总耗时: $viewModelElapsedTime ms")
                println("========================")
            } catch (e: Exception) {
                _error.value = "更新标签失败"
                _isLoading.value = false
                
                val viewModelEndTime = System.currentTimeMillis()
                val viewModelElapsedTime = viewModelEndTime - viewModelStartTime
                
                // 打印错误情况下的耗时统计
                println("=== ViewModel 更新标签操作统计（错误） ===")
                println("操作类型: 更新标签")
                println("笔记ID: $noteId")
                println("ViewModel 层总耗时: $viewModelElapsedTime ms")
                println("错误信息: ${e.message}")
                println("================================")
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
}