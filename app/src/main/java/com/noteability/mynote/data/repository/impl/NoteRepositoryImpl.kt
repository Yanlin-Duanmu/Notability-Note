package com.noteability.mynote.data.repository.impl

import android.content.Context
import com.noteability.mynote.data.AppDatabase
import com.noteability.mynote.data.dao.NoteDao
import com.noteability.mynote.data.entity.Note
import com.noteability.mynote.data.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class NoteRepositoryImpl(private val context: Context) : NoteRepository {
    private val noteDao: NoteDao by lazy {
        AppDatabase.getDatabase(context).noteDao()
    }
    
    // 当前用户ID（实际应用中应该从登录状态获取）
    private var currentUserId = 1L // 默认值，会在登录后更新
    
    // 更新当前用户ID
    fun updateCurrentUserId(userId: Long) {
        this.currentUserId = userId
    }
    
    override fun getAllNotes(): Flow<List<Note>> = flow {
        emit(noteDao.getNotesByUserId(currentUserId))
    }
    
    override fun getNotesByTagId(tagId: Long): Flow<List<Note>> = flow {
        emit(noteDao.getNotesByTagId(currentUserId, tagId))
    }
    
    override fun getNoteById(noteId: Long): Flow<Note?> = flow {
        // 获取笔记后验证是否属于当前用户
        val note = noteDao.getNoteById(noteId)
        // 只有当笔记存在且属于当前用户时才返回，否则返回null
        emit(if (note?.userId == currentUserId) note else null)
    }
    
    override fun searchNotes(query: String, tagId: Long?): Flow<List<Note>> = flow {
        if (query.isBlank()) {
            if (tagId != null) {
                emit(noteDao.getNotesByTagId(currentUserId, tagId))
            } else {
                emit(noteDao.getNotesByUserId(currentUserId))
            }
        } else {
            if (tagId != null) {
                emit(noteDao.searchNotesByTagAndTitleOrContent(currentUserId, tagId, query))
            } else {
                emit(noteDao.searchNotesByTitleOrContent(currentUserId, query))
            }
        }
    }
    
    override suspend fun getNoteCountByTag(tagId: Long): Int {
        return noteDao.getNoteCountByTag(currentUserId, tagId)
    }
    
    override suspend fun saveNote(note: Note) {
        // 确保使用当前用户ID
        val noteToSave = note.copy(userId = currentUserId)
        noteDao.insertNote(noteToSave)
    }
    
    override suspend fun updateNote(note: Note) {
        // 确保使用当前用户ID并更新时间戳
        val noteToUpdate = note.copy(
            userId = currentUserId,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.updateNote(noteToUpdate)
    }
    
    override suspend fun deleteNote(noteId: Long) {
        // 验证笔记是否属于当前用户后再删除
        val note = noteDao.getNoteById(noteId)
        if (note?.userId == currentUserId) {
            noteDao.deleteNoteById(noteId)
        }
    }
}