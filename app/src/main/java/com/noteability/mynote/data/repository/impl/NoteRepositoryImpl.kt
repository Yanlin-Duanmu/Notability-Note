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
        val startTime = System.currentTimeMillis()
        val noteToSave = note.copy(userId = currentUserId)
        val contentLength = note.content.length
        
        noteDao.insertNote(noteToSave)
        
        val endTime = System.currentTimeMillis()
        val elapsedTime = endTime - startTime
        
        // 打印保存操作耗时统计
        println("=== 保存笔记性能统计 ===")
        println("操作类型: 新建笔记")
        println("笔记ID: ${noteToSave.noteId}")
        println("标题: ${noteToSave.title}")
        println("内容长度: $contentLength 字符")
        println("执行时间: $elapsedTime ms")
        println("====================")
    }

    override suspend fun updateNote(note: Note) {
        val startTime = System.currentTimeMillis()
        val noteToUpdate = note.copy(
            userId = currentUserId,
            updatedAt = System.currentTimeMillis()
        )
        val contentLength = note.content.length
        
        noteDao.updateNote(noteToUpdate)
        
        val endTime = System.currentTimeMillis()
        val elapsedTime = endTime - startTime
        
        // 打印更新操作耗时统计
        println("=== 更新笔记性能统计 ===")
        println("操作类型: 更新笔记")
        println("笔记ID: ${noteToUpdate.noteId}")
        println("标题: ${noteToUpdate.title}")
        println("内容长度: $contentLength 字符")
        println("执行时间: $elapsedTime ms")
        println("====================")
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

    //批量删除
    override suspend fun deleteNotesList(noteIds: List<Long>) {
        noteDao.deleteNoteList(noteIds)
    }
    
    override suspend fun updateNoteTitle(noteId: Long, title: String): Int {
        val startTime = System.currentTimeMillis()
        val updatedAt = System.currentTimeMillis()
        
        val affectedRows = noteDao.updateNoteTitle(noteId, currentUserId, title, updatedAt)
        
        val endTime = System.currentTimeMillis()
        val elapsedTime = endTime - startTime
        
        // 打印更新操作耗时统计
        println("=== 更新笔记标题性能统计 ===")
        println("操作类型: 更新标题")
        println("笔记ID: $noteId")
        println("标题: $title")
        println("执行时间: $elapsedTime ms")
        println("受影响行数: $affectedRows")
        println("====================")
        
        return affectedRows
    }
    
    override suspend fun updateNoteContent(noteId: Long, content: String): Int {
        val startTime = System.currentTimeMillis()
        val updatedAt = System.currentTimeMillis()
        val contentLength = content.length
        
        val affectedRows = noteDao.updateNoteContent(noteId, currentUserId, content, updatedAt)
        
        val endTime = System.currentTimeMillis()
        val elapsedTime = endTime - startTime
        
        // 打印更新操作耗时统计
        println("=== 更新笔记内容性能统计 ===")
        println("操作类型: 更新内容")
        println("笔记ID: $noteId")
        println("内容长度: $contentLength 字符")
        println("执行时间: $elapsedTime ms")
        println("受影响行数: $affectedRows")
        println("====================")
        
        return affectedRows
    }
    
    override suspend fun updateNoteTag(noteId: Long, tagId: Long): Int {
        val startTime = System.currentTimeMillis()
        val updatedAt = System.currentTimeMillis()
        
        val affectedRows = noteDao.updateNoteTag(noteId, currentUserId, tagId, updatedAt)
        
        val endTime = System.currentTimeMillis()
        val elapsedTime = endTime - startTime
        
        // 打印更新操作耗时统计
        println("=== 更新笔记标签性能统计 ===")
        println("操作类型: 更新标签")
        println("笔记ID: $noteId")
        println("标签ID: $tagId")
        println("执行时间: $elapsedTime ms")
        println("受影响行数: $affectedRows")
        println("====================")
        
        return affectedRows
    }
    
    override suspend fun updateNoteStyle(noteId: Long, styleData: String): Int {
        val startTime = System.currentTimeMillis()
        val updatedAt = System.currentTimeMillis()
        
        val affectedRows = noteDao.updateNoteStyle(noteId, currentUserId, styleData, updatedAt)
        
        val endTime = System.currentTimeMillis()
        val elapsedTime = endTime - startTime
        
        // 打印更新操作耗时统计
        println("=== 更新笔记样式性能统计 ===")
        println("操作类型: 更新样式")
        println("笔记ID: $noteId")
        println("执行时间: $elapsedTime ms")
        println("受影响行数: $affectedRows")
        println("====================")
        
        return affectedRows
    }

}
