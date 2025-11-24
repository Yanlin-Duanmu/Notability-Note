package com.noteability.mynote.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.noteability.mynote.data.dao.UserDao
import com.noteability.mynote.data.dao.NoteDao
import com.noteability.mynote.data.dao.TagDao
import com.noteability.mynote.data.entity.User
import com.noteability.mynote.data.entity.Note
import com.noteability.mynote.data.entity.Tag

@Database(entities = [User::class, Note::class, Tag::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
}
