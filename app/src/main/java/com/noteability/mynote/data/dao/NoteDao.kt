package com.noteability.mynote.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Query
import com.noteability.mynote.data.entity.Note

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
    suspend fun getNotesByUserId(userId: Long): List<Note>

    @Query("SELECT * FROM notes WHERE userId = :userId AND tagId = :tagId ORDER BY updatedAt DESC")
    suspend fun getNotesByTagId(userId: Long, tagId: Long): List<Note>

    @Query("SELECT * FROM notes WHERE noteId = :id")
    suspend fun getNoteById(id: Long): Note?

    // 搜索功能
    @Query("SELECT * FROM notes WHERE userId = :userId AND title LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    suspend fun searchNotes(userId: Long, query: String): List<Note>

    @Query("SELECT * FROM notes WHERE userId = :userId AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    suspend fun searchNotesByTitleOrContent(userId: Long, query: String): List<Note>

    @Query("SELECT * FROM notes WHERE userId = :userId AND tagId = :tagId AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    suspend fun searchNotesByTagAndTitleOrContent(userId: Long, tagId: Long, query: String): List<Note>

    // 为富文本模块提供的专用方法
    @Insert
    suspend fun insertArticle(note: Note): Long

    @Query("UPDATE notes SET title = :newTitle, content = :newContent, updatedAt = :updatedTime WHERE noteId = :id")
    suspend fun updateArticle(id: Long, newTitle: String, newContent: String, updatedTime: Long): Int

    // 便捷方法 - 自动处理时间戳的插入和更新
    @Query("INSERT INTO notes (title, content, userId, tagId, createdAt, updatedAt) VALUES (:title, :content, :userId, :tagId, :createdTime, :updatedTime)")
    suspend fun insertArticleWithTimestamp(title: String, content: String, userId: Long, tagId: Long, createdTime: Long, updatedTime: Long): Long

    @Query("UPDATE notes SET title = :newTitle, content = :newContent, updatedAt = :updatedTime WHERE noteId = :id")
    suspend fun updateArticleWithTimestamp(id: Long, newTitle: String, newContent: String, updatedTime: Long): Int

    @Query("UPDATE notes SET tagId = :newTagId WHERE noteId = :id")
    suspend fun updateNoteTag(id: Long, newTagId: Long): Int

    // 统计相关
    @Query("SELECT COUNT(*) FROM notes WHERE userId = :userId")
    suspend fun getNoteCountByUser(userId: Long): Int

    @Query("SELECT COUNT(*) FROM notes WHERE userId = :userId AND tagId = :tagId")
    suspend fun getNoteCountByTag(userId: Long, tagId: Long): Int

    // 批量操作
    @Query("DELETE FROM notes WHERE userId = :userId")
    suspend fun deleteAllNotesByUser(userId: Long): Int

    @Query("DELETE FROM notes WHERE userId = :userId AND tagId = :tagId")
    suspend fun deleteNotesByTagId(userId: Long, tagId: Long): Int

    @Query("DELETE FROM notes WHERE noteId = :id")
    suspend fun deleteNoteById(id: Long): Int
}