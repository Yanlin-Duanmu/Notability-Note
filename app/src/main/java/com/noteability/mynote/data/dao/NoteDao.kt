package com.noteability.mynote.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Query
import com.noteability.mynote.data.entity.Note
import com.noteability.mynote.data.entity.NoteContentVersion
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


    //批量删除
    @Query("DELETE FROM notes WHERE noteId IN (:noteIds)")
    suspend fun deleteNoteList(noteIds: List<Long>)
    
    // 单个字段更新方法
    @Query("UPDATE notes SET title = :title, updatedAt = :updatedAt WHERE noteId = :noteId AND userId = :userId")
    suspend fun updateNoteTitle(noteId: Long, userId: Long, title: String, updatedAt: Long): Int
    
    @Query("UPDATE notes SET content = :content, updatedAt = :updatedAt WHERE noteId = :noteId AND userId = :userId")
    suspend fun updateNoteContent(noteId: Long, userId: Long, content: String, updatedAt: Long): Int
    
    @Query("UPDATE notes SET tagId = :tagId, updatedAt = :updatedAt WHERE noteId = :noteId AND userId = :userId")
    suspend fun updateNoteTag(noteId: Long, userId: Long, tagId: Long, updatedAt: Long): Int
    
    @Query("UPDATE notes SET styleData = :styleData, updatedAt = :updatedAt WHERE noteId = :noteId AND userId = :userId")
    suspend fun updateNoteStyle(noteId: Long, userId: Long, styleData: String, updatedAt: Long): Int

    // NoteContentVersion相关方法
    @Insert
    suspend fun insertNoteContentVersion(version: NoteContentVersion): Long
    
    @Query("SELECT * FROM note_content_versions WHERE noteId = :noteId ORDER BY versionNumber DESC LIMIT 1")
    suspend fun getLatestNoteContentVersion(noteId: Long): NoteContentVersion?
    
    @Query("SELECT * FROM note_content_versions WHERE noteId = :noteId ORDER BY versionNumber")
    suspend fun getAllNoteContentVersions(noteId: Long): List<NoteContentVersion>
    
    @Query("DELETE FROM note_content_versions WHERE noteId = :noteId AND versionNumber < :minVersionNumber")
    suspend fun deleteOldNoteContentVersions(noteId: Long, minVersionNumber: Int): Int
    
    // 更新笔记的isLongText字段
    @Query("UPDATE notes SET isLongText = :isLongText, updatedAt = :updatedAt WHERE noteId = :noteId AND userId = :userId")
    suspend fun updateNoteIsLongText(noteId: Long, userId: Long, isLongText: Boolean, updatedAt: Long): Int
    
    // 更新笔记的updatedAt字段
    @Query("UPDATE notes SET updatedAt = :updatedAt WHERE noteId = :noteId AND userId = :userId")
    suspend fun updateNoteUpdatedAt(noteId: Long, userId: Long, updatedAt: Long): Int


    // 1. 按编辑时间倒序
    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getNotesOrderByTimeDesc(userId: Long): PagingSource<Int, Note>

    // 2. 按创建时间正序
    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY createdAt ASC")
    fun getNotesOrderByTimeAsc(userId: Long): PagingSource<Int, Note>

    // 3.按标题排序
    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY title ASC")
    fun getNotesOrderByTitle(userId: Long): PagingSource<Int, Note>
    @Query("""
    SELECT * FROM notes 
    WHERE userId = :userId 
    AND (:tagId IS NULL OR tagId = :tagId) 
    AND title LIKE :exactTitle
    ORDER BY updatedAt DESC
""")
    fun getNotesByExactTitlePagingSource(userId: Long, exactTitle: String, tagId: Long?): PagingSource<Int, Note>
}
