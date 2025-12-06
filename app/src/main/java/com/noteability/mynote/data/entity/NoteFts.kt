package com.noteability.mynote.data.entity

import androidx.room.Entity
import androidx.room.Fts4

// --- 修改这里的注解 ---
@Fts4(
    contentEntity = Note::class,
    tokenizer = "unicode61", // <-- 改为 unicode61
    tokenizerArgs = ["tokenchars=+"]
)
// --- 注解修改结束 ---
@Entity(tableName = "notes_fts")
data class NoteFts(
    val title: String,
    val content: String
)
