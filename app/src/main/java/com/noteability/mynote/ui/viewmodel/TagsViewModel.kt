package com.noteability.mynote.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noteability.mynote.data.entity.Tag
import com.noteability.mynote.data.repository.TagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TagsViewModel(private val tagRepository: TagRepository) : ViewModel() {
    
    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    init {
        loadTags()
    }
    
    // 加载所有标签
    fun loadTags() {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                tagRepository.getAllTags().collect { tags ->
                    _tags.value = tags
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "加载标签失败"
                _isLoading.value = false
            }
        }
    }
    
    // 搜索标签
    fun searchTags(query: String) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                tagRepository.searchTags(query).collect { searchResults ->
                    _tags.value = searchResults
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "搜索标签失败"
                _isLoading.value = false
            }
        }
    }
    
    // 创建新标签
    fun createTag(name: String) {
        viewModelScope.launch {
            try {
                val newTag = Tag(tagId = 0, userId = 1, name = name)
                tagRepository.saveTag(newTag)
                loadTags() // 创建成功后重新加载标签列表
            } catch (e: Exception) {
                _error.value = "创建标签失败"
            }
        }
    }
    
    // 删除标签
    fun deleteTag(tagId: Long) {
        viewModelScope.launch {
            try {
                tagRepository.deleteTag(tagId)
                loadTags() // 删除成功后重新加载标签列表
            } catch (e: Exception) {
                _error.value = "删除标签失败"
            }
        }
    }
    
    // 更新标签
    fun updateTag(tag: Tag) {
        viewModelScope.launch {
            try {
                tagRepository.updateTag(tag)
                loadTags() // 更新成功后重新加载标签列表
            } catch (e: Exception) {
                _error.value = "更新标签失败"
            }
        }
    }
}