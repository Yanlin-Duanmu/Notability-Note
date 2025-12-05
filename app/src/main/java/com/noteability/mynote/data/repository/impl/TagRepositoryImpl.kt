package com.noteability.mynote.data.repository.impl

import android.content.Context
import com.noteability.mynote.data.AppDatabase
import com.noteability.mynote.data.dao.TagDao
import com.noteability.mynote.data.entity.Tag
import com.noteability.mynote.data.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TagRepositoryImpl(private val context: Context) : TagRepository {
    private val tagDao: TagDao by lazy {
        AppDatabase.getDatabase(context).tagDao()
    }
    
    // 当前用户ID（实际应用中应该从登录状态获取）
    private var currentUserId = 0L
    
    // 更新当前用户ID
    fun updateCurrentUserId(userId: Long) {
        this.currentUserId = userId
    }
    
    // 初始化默认标签（如果数据库中没有标签）
    init {
        initializeDefaultTags()
    }
    
    private fun initializeDefaultTags() {
        // 在协程中初始化默认标签
        // 注意：这里简化处理，实际应用中应该在应用启动时初始化
    }
    
    override fun getAllTags(): Flow<List<Tag>> = flow {
        emit(tagDao.getTagsByUserId(currentUserId))
    }
    
    override fun getTagById(tagId: Long): Flow<Tag?> = flow {
        emit(tagDao.getTagById(currentUserId, tagId))
    }
    
    // 根据标签名获取标签
    override suspend fun getTagByName(userId: Long, tagName: String): Tag? {
        return tagDao.getTagByName(userId, tagName)
    }
    
    override fun searchTags(query: String): Flow<List<Tag>> = flow {
        try {
            if (query.isBlank()) {
                // 如果查询为空，返回所有标签
                emit(tagDao.getTagsByUserId(currentUserId))
            } else {
                // 使用数据库层面的模糊搜索
                emit(tagDao.getTagsByNameContaining(currentUserId, query))
            }
        } catch (e: Exception) {
            // 如果发生错误，记录异常并返回空列表
            e.printStackTrace()
            emit(emptyList())
        }
    }
    
    override suspend fun saveTag(tag: Tag) {
        // 确保使用当前用户ID
        val tagToSave = tag.copy(userId = currentUserId)
        tagDao.insertTag(tagToSave)
    }
    
    override suspend fun updateTag(tag: Tag) {
        // 确保使用当前用户ID
        val tagToUpdate = tag.copy(userId = currentUserId)
        tagDao.updateTag(tagToUpdate)
    }
    
    override suspend fun deleteTag(tagId: Long) {
        tagDao.deleteTagById(currentUserId, tagId)
    }
    
    override suspend fun updateTagNoteCount(tagId: Long, noteCount: Int) {
        // 获取标签并更新笔记数量
        val tag = tagDao.getTagById(currentUserId, tagId)
        if (tag != null) {
            tagDao.updateTag(tag.copy(noteCount = noteCount))
        }
    }
}