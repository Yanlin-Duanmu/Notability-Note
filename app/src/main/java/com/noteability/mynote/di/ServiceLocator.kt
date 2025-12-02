package com.noteability.mynote.di

import android.content.Context
import androidx.room.Room
import com.noteability.mynote.data.AppDatabase
import com.noteability.mynote.data.repository.NoteRepository
import com.noteability.mynote.data.repository.TagRepository
import com.noteability.mynote.data.repository.UserRepository
// UserRepository是一个类而不是接口，不需要导入Impl实现
import com.noteability.mynote.data.repository.impl.NoteRepositoryImpl
import com.noteability.mynote.data.repository.impl.TagRepositoryImpl
import com.noteability.mynote.ui.viewmodel.NoteDetailViewModel
import com.noteability.mynote.ui.viewmodel.NotesViewModel
import com.noteability.mynote.ui.viewmodel.TagsViewModel
import com.noteability.mynote.data.entity.User

/**
 * 简单的服务定位器，用于管理依赖注入
 * 在实际项目中，可以考虑使用Hilt/Dagger等依赖注入框架
 */
object ServiceLocator {
    
    // 初始化应用上下文
    private var context: Context? = null
    
    // 设置上下文
    fun setContext(appContext: Context) {
        this.context = appContext
    }
    
    // 获取数据库实例
    private val database: AppDatabase by lazy {
        requireNotNull(context) { "Context must be set before using ServiceLocator" }
        Room.databaseBuilder(
            context!!,
            AppDatabase::class.java,
            "mynote_database"
        ).build()
    }
    
    private val userRepository: UserRepository by lazy { 
        // 直接使用UserRepository类，并传入userDao
        UserRepository(database.userDao())
    }
    
    // ViewModel现在通过各Activity中的ViewModelFactory创建，此处不再提供
    
    // NoteDetailViewModel现在通过NoteEditActivity中的ViewModelFactory创建，此处不再提供
    
    // 初始化NoteRepository和TagRepository
    private val noteRepository: NoteRepository by lazy {
        requireNotNull(context) { "Context must be set before using ServiceLocator" }
        val repository = NoteRepositoryImpl(context!!)
        // 设置当前登录用户ID
        val sharedPreferences = context!!.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getLong("logged_in_user_id", 0L)
        if (userId > 0) {
            (repository as NoteRepositoryImpl).updateCurrentUserId(userId)
        }
        repository
    }
    
    private val tagRepository: TagRepository by lazy {
        requireNotNull(context) { "Context must be set before using ServiceLocator" }
        val repository = TagRepositoryImpl(context!!)
        // 设置当前登录用户ID
        val sharedPreferences = context!!.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getLong("logged_in_user_id", 0L)
        if (userId > 0) {
            (repository as TagRepositoryImpl).updateCurrentUserId(userId)
        }
        repository
    }
    
    // Repository提供者方法
    fun provideUserRepository(): UserRepository {
        return userRepository
    }
    
    fun provideNoteRepository(): NoteRepository {
        return noteRepository
    }
    
    fun provideTagRepository(): TagRepository {
        return tagRepository
    }
    
    // 更新当前登录用户ID到所有需要的仓库
    fun updateLoggedInUserId(userId: Long) {
        // 更新NoteRepository
        (noteRepository as? NoteRepositoryImpl)?.updateCurrentUserId(userId)
        // 更新TagRepository
        (tagRepository as? TagRepositoryImpl)?.updateCurrentUserId(userId)
    }
}