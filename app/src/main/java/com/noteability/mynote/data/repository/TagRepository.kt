package com.noteability.mynote.data.repository

import com.noteability.mynote.data.entity.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    // 获取所有标签
    fun getAllTags(): Flow<List<Tag>>
    
    // 根据ID获取标签
    fun getTagById(tagId: Long): Flow<Tag?>
    
    // 搜索标签
    fun searchTags(query: String): Flow<List<Tag>>
    
    // 保存标签
    suspend fun saveTag(tag: Tag)
    
    // 更新标签
    suspend fun updateTag(tag: Tag)
    
    // 删除标签
    suspend fun deleteTag(tagId: Long)
    
    // 更新标签的笔记数量
    suspend fun updateTagNoteCount(tagId: Long, noteCount: Int)
}