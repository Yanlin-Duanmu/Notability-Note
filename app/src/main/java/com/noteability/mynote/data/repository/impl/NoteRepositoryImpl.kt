package com.noteability.mynote.data.repository.impl

import android.content.Context
import androidx.paging.PagingSource
import com.noteability.mynote.data.AppDatabase
import com.noteability.mynote.data.dao.NoteDao
import com.noteability.mynote.data.dao.SortOrder
import com.noteability.mynote.data.entity.Note
import com.noteability.mynote.data.entity.NoteContentVersion
import com.noteability.mynote.data.repository.NoteRepository
import com.noteability.mynote.util.TextDiffUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class NoteRepositoryImpl(private val context: Context) : NoteRepository {
    private val noteDao: NoteDao by lazy {
        AppDatabase.getDatabase(context).noteDao()
    }

    private var currentUserId = 1L
    private val diffUtils = TextDiffUtils()

    override fun setCurrentUserId(userId: Long) {
        this.currentUserId = userId
    }

    override fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getNotesByUserId(currentUserId)
    }

    override fun getNotesByTagId(tagId: Long): Flow<List<Note>> {
        return noteDao.getNotesByTagId(currentUserId, tagId)
    }

    override fun getNoteById(noteId: Long): Flow<Note?> = flow {
        val note = getNoteWithFullContent(noteId)
        // 确保只返回当前用户的笔记
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
        val isLongText = diffUtils.shouldUseDiffStorage(note.content)
        val noteToSave = note.copy(
            userId = currentUserId,
            isLongText = isLongText
        )
        
        noteDao.insertNote(noteToSave)
    }

    override suspend fun updateNote(note: Note) {
        val isLongText = diffUtils.shouldUseDiffStorage(note.content)
        val noteToUpdate = note.copy(
            userId = currentUserId,
            updatedAt = System.currentTimeMillis(),
            isLongText = isLongText
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

    //批量删除
    override suspend fun deleteNotesList(noteIds: List<Long>) {
        noteDao.deleteNoteList(noteIds)
    }
    override fun getNotesByExactTitlePagingSource(userId: Long, exactTitle: String, tagId: Long?): PagingSource<Int, Note> {
        return noteDao.getNotesByExactTitlePagingSource(userId, exactTitle, tagId)
    }

    //根据选择的排序方式排序
    override fun getAllNotesStream(userId: Long, sortOrder: SortOrder): PagingSource<Int, Note> {
        return when (sortOrder) {
            SortOrder.EDIT_TIME_DESC -> noteDao.getNotesOrderByTimeDesc(userId)
            SortOrder.CREATE_TIME_ASC -> noteDao.getNotesOrderByTimeAsc(userId)
            SortOrder.TITLE_ASC -> noteDao.getNotesOrderByTitle(userId)
        }
    }
    
    override suspend fun updateNoteTitle(noteId: Long, title: String): Int {
        val updatedAt = System.currentTimeMillis()
        
        return noteDao.updateNoteTitle(noteId, currentUserId, title, updatedAt)
    }
    
    override suspend fun updateNoteContent(noteId: Long, content: String): Int {
        val updatedAt = System.currentTimeMillis()
        
        val note = noteDao.getNoteById(noteId)
        
        if (note == null || note.userId != currentUserId) {
            return 0
        }
        
        val isLongText = diffUtils.shouldUseDiffStorage(content)
        
        var affectedRows = 0
        
        // 根据文本长度决定使用普通更新还是差分存储
        if (isLongText) {
            // 使用差分存储
            affectedRows = updateNoteContentWithDiff(noteId, note.content, content)
            
            // 只有当笔记之前不是长文本时，才需要更新isLongText字段
            if (!note.isLongText) {
                noteDao.updateNoteIsLongText(noteId, currentUserId, true, updatedAt)
            }
        } else {
            // 短文本直接更新
            affectedRows = noteDao.updateNoteContent(noteId, currentUserId, content, updatedAt)
            
            // 如果之前是长文本，现在变成短文本，更新isLongText字段
            if (note.isLongText) {
                noteDao.updateNoteIsLongText(noteId, currentUserId, false, updatedAt)
            }
        }
        
        return affectedRows
    }
    
    override suspend fun updateNoteContentWithDiff(noteId: Long, oldContent: String, newContent: String): Int {
        val diffData = diffUtils.generateDiff(oldContent, newContent)
        
        if (diffData.isEmpty()) {
            return 0
        }
        
        val latestVersion = noteDao.getLatestNoteContentVersion(noteId)
        val nextVersionNumber = latestVersion?.versionNumber?.plus(1) ?: 1
        
        val version = NoteContentVersion(
            noteId = noteId,
            versionNumber = nextVersionNumber,
            diffData = diffData,
            contentLength = newContent.length
        )
        
        noteDao.insertNoteContentVersion(version)
        
        // 定期合并版本，避免版本过多
        if (nextVersionNumber % 10 == 0) {
            mergeVersions(noteId)
        }
        
        return 1
    }
    
    private suspend fun mergeVersions(noteId: Long) {
        // 获取所有版本
        val versions = noteDao.getAllNoteContentVersions(noteId)
        if (versions.size < 5) return
        
        // 获取原内容
        val note = noteDao.getNoteById(noteId) ?: return
        
        // 合并所有版本生成最新内容
        var mergedContent = note.content
        versions.forEach { version ->
            mergedContent = diffUtils.applyDiff(mergedContent, version.diffData)
        }
        
        // 更新原Note的content字段
        noteDao.updateNoteContent(noteId, currentUserId, mergedContent, System.currentTimeMillis())
        
        // 删除所有旧版本
        noteDao.deleteOldNoteContentVersions(noteId, versions.last().versionNumber)
    }
    
    override suspend fun getNoteWithFullContent(noteId: Long): Note? {
        val note = noteDao.getNoteById(noteId) ?: return null
        
        if (!note.isLongText || note.userId != currentUserId) {
            return note
        }
        
        // 检查是否有差分版本
        val versions = noteDao.getAllNoteContentVersions(noteId)
        if (versions.isEmpty()) {
            return note
        }
        
        // 应用所有差分版本生成完整内容
        var fullContent = note.content
        versions.forEach { version ->
            fullContent = diffUtils.applyDiff(fullContent, version.diffData)
        }
        
        // 返回包含完整内容的Note
        return note.copy(content = fullContent)
    }
    
    override suspend fun updateNoteTag(noteId: Long, tagId: Long): Int {
        val updatedAt = System.currentTimeMillis()
        
        return noteDao.updateNoteTag(noteId, currentUserId, tagId, updatedAt)
    }
    
    override suspend fun updateNoteStyle(noteId: Long, styleData: String): Int {
        val updatedAt = System.currentTimeMillis()
        
        return noteDao.updateNoteStyle(noteId, currentUserId, styleData, updatedAt)
    }

}
