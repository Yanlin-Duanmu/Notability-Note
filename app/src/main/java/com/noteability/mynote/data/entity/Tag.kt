package com.noteability.mynote.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "tags",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["userId"])
    ]
)
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val tagId: Long = 0,
    val userId: Long, // 用户级标签
    val name: String,
    val noteCount: Int = 0
)
