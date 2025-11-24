package com.noteability.mynote.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.noteability.mynote.data.entity.Tag

@Dao
interface TagDao {

    @Insert
    suspend fun insertTag(tag: Tag): Long

    @Query("SELECT * FROM tags WHERE userId = :userId")
    suspend fun getTagsByUserId(userId: Long): List<Tag>
}
