package com.noteability.mynote.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "note_content_versions",
    indices = [
        Index(value = ["noteId"]),
        Index(value = ["versionNumber"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["noteId"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NoteContentVersion(
    @PrimaryKey(autoGenerate = true)
    val versionId: Long = 0,
    val noteId: Long,
    val versionNumber: Int, // 版本号，递增
    val diffData: String, // 差分数据，JSON格式存储修改操作
    val contentLength: Int, // 当前版本内容长度
    val createdAt: Long = System.currentTimeMillis()
)