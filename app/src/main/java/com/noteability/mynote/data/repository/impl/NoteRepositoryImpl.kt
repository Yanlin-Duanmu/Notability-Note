package com.noteability.mynote.data.repository.impl

import android.content.Context
import androidx.paging.PagingSource
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

    private var currentUserId = 1L

    fun updateCurrentUserId(userId: Long) {
        this.currentUserId = userId
    }

    override fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getNotesByUserId(currentUserId)
    }

    override fun getNotesByTagId(tagId: Long): Flow<List<Note>> {
        return noteDao.getNotesByTagId(currentUserId, tagId)
    }

    override fun getNoteById(noteId: Long): Flow<Note?> = flow {
        val note = noteDao.getNoteById(noteId)
        emit(if (note?.userId == currentUserId) note else null)
    }

    // --- 这是唯一需要的搜索方法 ---
    override fun searchNotes(query: String, tagId: Long?): Flow<List<Note>> {
        return if (query.isBlank()) {
            if (tagId != null && tagId != 0L) {
                noteDao.getNotesByTagId(currentUserId, tagId)
            } else {
                noteDao.getNotesByUserId(currentUserId)
            }
        } else {
            // FTS查询词需要转义特殊字符，特别是双引号
            val escapedQuery = query.replace("\"", "\"\"")
            // 构造一个精确的短语查询，并在末尾加上*号
            val ftsQuery = "\"$escapedQuery\"*"

            if (tagId != null && tagId != 0L) {
                noteDao.searchNotesByTag(currentUserId, tagId, ftsQuery)
            } else {
                noteDao.searchNotes(currentUserId, ftsQuery)
            }
        }
    }

    // --- `searchNotesByTag` 方法是多余的，已被删除 ---

    override suspend fun getNoteCountByTag(tagId: Long): Int {
        return noteDao.getNoteCountByTag(currentUserId, tagId)
    }

    override suspend fun saveNote(note: Note) {
        val noteToSave = note.copy(userId = currentUserId)
        noteDao.insertNote(noteToSave)
    }

    override suspend fun updateNote(note: Note) {
        val noteToUpdate = note.copy(
            userId = currentUserId,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.updateNote(noteToUpdate)
    }

    override suspend fun deleteNote(noteId: Long) {
        val note = noteDao.getNoteById(noteId)
        if (note?.userId == currentUserId) {
            noteDao.deleteNoteById(noteId)
        }
    }

    override fun getNotesPagingSource(userId: Long, query: String, tagId: Long?): PagingSource<Int, Note> {
        return noteDao.getNotesPagingSource(userId, query, tagId)
    }
}
