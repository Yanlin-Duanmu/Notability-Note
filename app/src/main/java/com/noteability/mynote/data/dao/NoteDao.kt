package com.noteability.mynote.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Query
import com.noteability.mynote.data.entity.Note

@Dao
interface NoteDao {

    @Insert
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY updatedAt DESC")
    suspend fun getNotesByUserId(userId: Long): List<Note>

    @Query("SELECT * FROM notes WHERE userId = :userId AND title LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    suspend fun searchNotes(userId: Long, query: String): List<Note>
}
