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
    private val currentUserId = 1L
    
    override fun getAllNotes(): Flow<List<Note>> = flow {
        emit(noteDao.getNotesByUserId(currentUserId))
    }
    
    override fun getNotesByTagId(tagId: Long): Flow<List<Note>> = flow {
        emit(noteDao.getNotesByTagId(currentUserId, tagId))
    }
    
    override fun getNoteById(noteId: Long): Flow<Note?> = flow {
        emit(noteDao.getNoteById(noteId))
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
        noteDao.deleteNoteById(noteId)
    }
}