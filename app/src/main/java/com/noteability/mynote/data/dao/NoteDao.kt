package com.noteability.mynote.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Query
import com.noteability.mynote.data.entity.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    // 基础 CRUD 操作
    @Insert
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note): Int

    @Delete
    suspend fun deleteNote(note: Note): Int

    // 查询操作
    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getNotesByUserId(userId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE userId = :userId AND tagId = :tagId ORDER BY updatedAt DESC")
    fun getNotesByTagId(userId: Long, tagId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE noteId = :id")
    suspend fun getNoteById(id: Long): Note?

    // FTS 全文搜索功能
    @Query("""
        SELECT * FROM notes JOIN notes_fts ON notes.rowid = notes_fts.rowid 
        WHERE notes_fts MATCH :query AND userId = :userId    """)
    fun searchNotes(userId: Long, query: String): Flow<List<Note>>

    @Query("""
        SELECT * FROM notes JOIN notes_fts ON notes.rowid = notes_fts.rowid 
        WHERE notes_fts MATCH :query AND userId = :userId AND tagId = :tagId
    """)
    fun searchNotesByTag(userId: Long, tagId: Long, query: String): Flow<List<Note>>


    // 统计相关
    @Query("SELECT COUNT(*) FROM notes WHERE userId = :userId AND tagId = :tagId")
    suspend fun getNoteCountByTag(userId: Long, tagId: Long): Int

    // 批量操作
    @Query("DELETE FROM notes WHERE noteId = :id")
    suspend fun deleteNoteById(id: Long): Int

    // --- 以下是旧的、有问题的或重复的方法，已被移除 ---
    // 1. searchNotesByTagAndTitleOrContent 和 searchNotesByTitleOrContent 已被FTS查询替代。
    // 2. 之前报错的两个方法也已被正确的FTS版本和普通查询版本所覆盖和修正。
}
