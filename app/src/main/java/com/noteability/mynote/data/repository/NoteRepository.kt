package com.noteability.mynote.data.repository

import com.noteability.mynote.data.dao.NoteDao
import com.noteability.mynote.data.entity.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class NoteRepository(private val noteDao: NoteDao) {

    // 获取默认用户的ID（这里简化处理，实际应该从用户管理获取）
    private val defaultUserId = 1L

    // 任务3：从本地数据库加载列表（首页列表）
    fun getAllNotes(): Flow<List<Note>> = flow {
        val notes = noteDao.getNotesByUserId(defaultUserId)
        emit(notes.sortedByDescending { it.updatedAt })
    }

    // 基础CRUD操作
    suspend fun insertNote(note: Note): Long {
        return noteDao.insertNote(note)
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
    }

    suspend fun getNoteById(id: Long): Note? {
        return noteDao.getNoteById(id)
    }

    // 任务4：新增内容接口（供富文本模块使用）
    suspend fun insertArticle(title: String, content: String, tagId: Long = 0): Long {
        val currentTime = System.currentTimeMillis()
        val note = Note(
            userId = defaultUserId,
            tagId = tagId,
            title = title,
            content = content,
            createdAt = currentTime,
            updatedAt = currentTime
        )
        return noteDao.insertNote(note)
    }

    // 任务5：更新内容接口（供富文本模块使用）
    suspend fun updateArticle(id: Long, newTitle: String, newContent: String) {
        val updatedTime = System.currentTimeMillis()
        noteDao.updateArticle(id, newTitle, newContent, updatedTime)
    }

    // 便捷方法 - 自动处理时间戳的插入
    suspend fun insertArticleWithTimestamp(title: String, content: String, userId: Long, tagId: Long, createdTime: Long, updatedTime: Long): Long {
        return noteDao.insertArticleWithTimestamp(title, content, defaultUserId, tagId, createdTime, updatedTime)
    }

    // 搜索功能
    suspend fun searchNotes(query: String): List<Note> {
        return noteDao.searchNotes(defaultUserId, query)
    }

    // 全文搜索功能（搜索标题和内容）
    suspend fun searchNotesByTitleOrContent(query: String): List<Note> {
        return noteDao.searchNotesByTitleOrContent(defaultUserId, query)
    }

    // 更新笔记标签
    suspend fun updateNoteTag(noteId: Long, newTagId: Long): Int {
        return noteDao.updateNoteTag(noteId, newTagId)
    }

    // 获取笔记数量
    suspend fun getNoteCount(): Int {
        return noteDao.getNoteCountByUser(defaultUserId)
    }

    // 获取指定标签下的笔记数量
    suspend fun getNoteCountByTag(tagId: Long): Int {
        return noteDao.getNoteCountByTag(defaultUserId, tagId)
    }

    // 删除指定标签下的所有笔记
    suspend fun deleteNotesByTagId(tagId: Long): Int {
        return noteDao.deleteNotesByTagId(defaultUserId, tagId)
    }

    // 根据ID删除单条笔记
    suspend fun deleteNoteById(noteId: Long): Int {
        return noteDao.deleteNoteById(noteId)
    }

    // 根据标签ID获取笔记列表
    suspend fun getNotesByTagId(tagId: Long): List<Note> {
        return noteDao.getNotesByTagId(defaultUserId, tagId)
    }

    // 更新文章内容（带时间戳参数）
    suspend fun updateArticleWithTimestamp(id: Long, newTitle: String, newContent: String, updatedTime: Long): Int {
        return noteDao.updateArticleWithTimestamp(id, newTitle, newContent, updatedTime)
    }

    // 删除用户的所有笔记
    suspend fun deleteAllNotesByUser(): Int {
        return noteDao.deleteAllNotesByUser(defaultUserId)
    }
}