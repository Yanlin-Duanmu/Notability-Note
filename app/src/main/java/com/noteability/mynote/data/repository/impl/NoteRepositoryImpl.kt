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
        val startTime = System.currentTimeMillis()
        val isLongText = diffUtils.shouldUseDiffStorage(note.content)
        val noteToSave = note.copy(
            userId = currentUserId,
            isLongText = isLongText
        )
        val contentLength = note.content.length
        
        noteDao.insertNote(noteToSave)
        
        val endTime = System.currentTimeMillis()
        val elapsedTime = endTime - startTime
    }

    override suspend fun updateNote(note: Note) {
        val startTime = System.currentTimeMillis()
        val isLongText = diffUtils.shouldUseDiffStorage(note.content)
        val noteToUpdate = note.copy(
            userId = currentUserId,
            updatedAt = System.currentTimeMillis(),
            isLongText = isLongText
        )
        val contentLength = note.content.length
        
        noteDao.updateNote(noteToUpdate)
        
        val endTime = System.currentTimeMillis()
        val elapsedTime = endTime - startTime
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
        val startTime = System.currentTimeMillis()
        val updatedAt = System.currentTimeMillis()
        
        val affectedRows = noteDao.updateNoteTitle(noteId, currentUserId, title, updatedAt)
        
        val endTime = System.currentTimeMillis()
        val elapsedTime = endTime - startTime
        
        return affectedRows
    }
    
    override suspend fun updateNoteContent(noteId: Long, content: String): Int {
        val totalStartTime = System.currentTimeMillis()
        val updatedAt = System.currentTimeMillis()
        
        // 获取当前笔记 - 非存储操作
        val noteStartTime = System.currentTimeMillis()
        val note = noteDao.getNoteById(noteId)
        val noteEndTime = System.currentTimeMillis()
        val noteElapsedTime = noteEndTime - noteStartTime
        
        if (note == null || note.userId != currentUserId) {
            return 0
        }
        
        // 判断是否为长文本 - 非存储操作
        val isLongTextStartTime = System.currentTimeMillis()
        val isLongText = diffUtils.shouldUseDiffStorage(content)
        val isLongTextEndTime = System.currentTimeMillis()
        val isLongTextElapsedTime = isLongTextEndTime - isLongTextStartTime
        
        var affectedRows = 0
        
        // 根据文本长度决定使用普通更新还是差分存储
        if (isLongText) {
            // 使用差分存储
            affectedRows = updateNoteContentWithDiff(noteId, note.content, content)
            
            // 只有当笔记之前不是长文本时，才需要更新isLongText字段
            if (!note.isLongText) {
                val updateLongTextStartTime = System.currentTimeMillis()
                noteDao.updateNoteIsLongText(noteId, currentUserId, true, updatedAt)
                val updateLongTextEndTime = System.currentTimeMillis()
                val updateLongTextElapsedTime = updateLongTextEndTime - updateLongTextStartTime
            }
        } else {
            // 短文本直接更新
            val updateContentStartTime = System.currentTimeMillis()
            affectedRows = noteDao.updateNoteContent(noteId, currentUserId, content, updatedAt)
            val updateContentEndTime = System.currentTimeMillis()
            val updateContentElapsedTime = updateContentEndTime - updateContentStartTime
            
            // 如果之前是长文本，现在变成短文本，更新isLongText字段
            if (note.isLongText) {
                val updateLongTextStartTime = System.currentTimeMillis()
                noteDao.updateNoteIsLongText(noteId, currentUserId, false, updatedAt)
                val updateLongTextEndTime = System.currentTimeMillis()
                val updateLongTextElapsedTime = updateLongTextEndTime - updateLongTextStartTime
            }
        }
        
        val totalEndTime = System.currentTimeMillis()
        val totalElapsedTime = totalEndTime - totalStartTime
        
        println("=== Repository 更新内容操作详细统计 ===")
        println("总耗时: $totalElapsedTime ms")
        println("获取当前笔记耗时: $noteElapsedTime ms")
        println("判断是否为长文本耗时: $isLongTextElapsedTime ms")
        println("==================================")
        
        return affectedRows
    }
    
    override suspend fun updateNoteContentWithDiff(noteId: Long, oldContent: String, newContent: String): Int {
        val totalDiffStartTime = System.currentTimeMillis()
        
        // 生成差分数据 - 非存储操作，可能耗时
        val diffGenStartTime = System.currentTimeMillis()
        val diffData = diffUtils.generateDiff(oldContent, newContent)
        val diffGenEndTime = System.currentTimeMillis()
        val diffGenElapsedTime = diffGenEndTime - diffGenStartTime
        
        if (diffData.isEmpty()) {
            return 0
        }
        
        // 获取当前最新版本 - 存储操作
        val getLatestVersionStartTime = System.currentTimeMillis()
        val latestVersion = noteDao.getLatestNoteContentVersion(noteId)
        val getLatestVersionEndTime = System.currentTimeMillis()
        val getLatestVersionElapsedTime = getLatestVersionEndTime - getLatestVersionStartTime
        
        val nextVersionNumber = latestVersion?.versionNumber?.plus(1) ?: 1
        
        // 构建版本对象 - 非存储操作
        val buildVersionStartTime = System.currentTimeMillis()
        val version = NoteContentVersion(
            noteId = noteId,
            versionNumber = nextVersionNumber,
            diffData = diffData,
            contentLength = newContent.length
        )
        val buildVersionEndTime = System.currentTimeMillis()
        val buildVersionElapsedTime = buildVersionEndTime - buildVersionStartTime
        
        // 保存差分版本 - 核心存储操作
        val storageStartTime = System.currentTimeMillis()
        val insertedId = noteDao.insertNoteContentVersion(version)
        val storageEndTime = System.currentTimeMillis()
        val storageElapsedTime = storageEndTime - storageStartTime
        
        // 定期合并版本，避免版本过多
        if (nextVersionNumber % 10 == 0) {
            val mergeVersionsStartTime = System.currentTimeMillis()
            mergeVersions(noteId)
            val mergeVersionsEndTime = System.currentTimeMillis()
            val mergeVersionsElapsedTime = mergeVersionsEndTime - mergeVersionsStartTime
            
            println("  合并版本耗时: $mergeVersionsElapsedTime ms")
        }
        
        val totalDiffEndTime = System.currentTimeMillis()
        val totalDiffElapsedTime = totalDiffEndTime - totalDiffStartTime
        
        // 打印差分存储详细统计
        println("=== 差分存储操作详细统计 ===")
        println("总耗时: $totalDiffElapsedTime ms")
        println("生成差分数据耗时: $diffGenElapsedTime ms")
        println("获取最新版本耗时: $getLatestVersionElapsedTime ms")
        println("构建版本对象耗时: $buildVersionElapsedTime ms")
        println("核心存储操作耗时: $storageElapsedTime ms")
        println("==================================")
        
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
        val startTime = System.currentTimeMillis()
        val updatedAt = System.currentTimeMillis()
        
        val affectedRows = noteDao.updateNoteTag(noteId, currentUserId, tagId, updatedAt)
        
        val endTime = System.currentTimeMillis()
        val elapsedTime = endTime - startTime
        
        return affectedRows
    }
    
    override suspend fun updateNoteStyle(noteId: Long, styleData: String): Int {
        val startTime = System.currentTimeMillis()
        val updatedAt = System.currentTimeMillis()
        
        val affectedRows = noteDao.updateNoteStyle(noteId, currentUserId, styleData, updatedAt)
        
        val endTime = System.currentTimeMillis()
        val elapsedTime = endTime - startTime
        
        return affectedRows
    }

}
