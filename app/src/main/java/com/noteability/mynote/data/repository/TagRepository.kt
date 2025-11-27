package com.noteability.mynote.data.repository

import com.noteability.mynote.data.dao.TagDao
import com.noteability.mynote.data.entity.Tag

class TagRepository(private val tagDao: TagDao) {

    // 获取默认用户的ID（与NoteRepository保持一致）
    private val defaultUserId = 1L

    // 创建标签
    suspend fun createTag(tagName: String): Long {
        val tag = Tag(
            userId = defaultUserId,
            name = tagName
        )
        return tagDao.insertTag(tag)
    }

    // 更新标签
    suspend fun updateTag(tag: Tag): Int {
        return tagDao.updateTag(tag)
    }

    // 删除标签
    suspend fun deleteTag(tag: Tag): Int {
        return tagDao.deleteTag(tag)
    }

    // 根据ID删除标签
    suspend fun deleteTagById(tagId: Long): Int {
        return tagDao.deleteTagById(defaultUserId, tagId)
    }

    // 获取所有标签
    suspend fun getAllTags(): List<Tag> {
        return tagDao.getTagsByUserId(defaultUserId)
    }

    // 根据ID获取标签
    suspend fun getTagById(tagId: Long): Tag? {
        return tagDao.getTagById(defaultUserId, tagId)
    }

    // 根据名称获取标签
    suspend fun getTagByName(tagName: String): Tag? {
        return tagDao.getTagByName(defaultUserId, tagName)
    }
}
