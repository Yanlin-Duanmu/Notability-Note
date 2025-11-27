package com.noteability.mynote.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.noteability.mynote.data.entity.Tag

@Dao
interface TagDao {

    @Insert
    suspend fun insertTag(tag: Tag): Long

    @Update
    suspend fun updateTag(tag: Tag): Int

    @Delete
    suspend fun deleteTag(tag: Tag): Int

    @Query("SELECT * FROM tags WHERE userId = :userId ORDER BY name ASC")
    suspend fun getTagsByUserId(userId: Long): List<Tag>

    @Query("SELECT * FROM tags WHERE userId = :userId AND tagId = :tagId LIMIT 1")
    suspend fun getTagById(userId: Long, tagId: Long): Tag?

    @Query("SELECT * FROM tags WHERE userId = :userId AND name = :tagName LIMIT 1")
    suspend fun getTagByName(userId: Long, tagName: String): Tag?

    @Query("DELETE FROM tags WHERE userId = :userId AND tagId = :tagId")
    suspend fun deleteTagById(userId: Long, tagId: Long): Int
}
