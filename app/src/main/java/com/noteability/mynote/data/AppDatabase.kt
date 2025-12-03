package com.noteability.mynote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.noteability.mynote.data.dao.UserDao
import com.noteability.mynote.data.dao.NoteDao
import com.noteability.mynote.data.dao.TagDao
import com.noteability.mynote.data.entity.User
import com.noteability.mynote.data.entity.Note
import com.noteability.mynote.data.entity.Tag

@Database(
    entities = [User::class, Note::class, Tag::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mynote_database"
                )
                // 开发阶段保留破坏性迁移，便于数据库结构频繁修改
                // 这会在数据库版本升级时删除所有表并重新创建
                // 注意：进入生产环境前，必须实现具体的Migration类来保留用户数据
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}