package com.noteability.mynote.data.dao

import androidx.paging.PagingSource
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

    //Paging 3分页查询
    @Query("""
        SELECT * FROM notes 
        WHERE userId = :userId 
        AND (:tagId IS NULL OR tagId = :tagId) 
        AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') 
        ORDER BY updatedAt DESC
    """)
    fun getNotesPagingSource(userId: Long, query: String, tagId: Long?): PagingSource<Int, Note>
}
