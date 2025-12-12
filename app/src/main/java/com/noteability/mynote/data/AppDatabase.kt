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
import com.noteability.mynote.data.entity.NoteFts
import com.noteability.mynote.data.entity.NoteContentVersion
import com.noteability.mynote.data.entity.Tag

@Database(
    entities = [User::class, Note::class, Tag::class, NoteFts::class, NoteContentVersion::class],
    version = 9, // 升级数据库版本，添加NoteContentVersion实体
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
                    // 2. 使用“破坏性迁移”来解决所有迁移问题
                    // 这会在版本升级时删除所有数据并根据最新代码重建所有表
                    // 对于修复棘手的FTS迁移问题，这是最可靠的方法
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
