package com.noteability.mynote.data.repository

import androidx.paging.PagingSource
import com.noteability.mynote.data.dao.SortOrder
import com.noteability.mynote.data.entity.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    // 获取所有笔记
    fun getAllNotes(): Flow<List<Note>>
    
    // 根据标签ID获取笔记
    fun getNotesByTagId(tagId: Long): Flow<List<Note>>
    // 根据ID获取单条笔记
    fun getNoteById(noteId: Long): Flow<Note?>

    // 搜索笔记
    fun searchNotes(query: String, tagId: Long? = null): Flow<List<Note>>
    
    // 获取指定标签下的笔记数量
    suspend fun getNoteCountByTag(tagId: Long): Int
    
    // 保存笔记
    suspend fun saveNote(note: Note)
    
    // 更新笔记
    suspend fun updateNote(note: Note)
    
    // 删除笔记
    suspend fun deleteNote(noteId: Long)

    // paging 3 获取分页数据源
    fun getNotesPagingSource(userId: Long, query: String, tagId: Long?): PagingSource<Int, Note>

    //批量删除
    suspend fun deleteNotesList(noteIds: List<Long>)


    // 按选择的排序方式排序
    fun getAllNotesStream(userId: Long, sortOrder: SortOrder): PagingSource<Int, Note>

}