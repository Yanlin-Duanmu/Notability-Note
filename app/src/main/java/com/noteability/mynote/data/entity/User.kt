package com.noteability.mynote.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val userId: Long = 0,
    val username: String,
    val passwordHash: String, // 密码请存哈希，不存明文
    val email: String? = null
)
