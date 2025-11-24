package com.noteability.mynote.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.noteability.mynote.data.entity.User

@Dao
interface UserDao {

    @Insert
    suspend fun insertUser(user: User): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE userId = :id LIMIT 1")
    suspend fun getUserById(id: Long): User?
}
