package com.noteability.mynote.di

import com.noteability.mynote.data.repository.NoteRepository
import com.noteability.mynote.data.repository.TagRepository
import com.noteability.mynote.data.repository.UserRepository
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
    
    // Repository实例现在通过各Activity中的ViewModelFactory创建，此处不再初始化
    // 模拟的UserDao实现
    private val mockUserDao = object : com.noteability.mynote.data.dao.UserDao {
        override suspend fun insertUser(user: User): Long {
            return 1 // 总是返回1作为用户ID
        }
        
        override suspend fun getUserByUsername(username: String): User? {
            // 返回默认用户用于测试
            return User(userId = 1, username = username, passwordHash = "hash")
        }
        
        override suspend fun getUserById(id: Long): User? {
            // 返回默认用户用于测试
            return User(userId = 1, username = "default", passwordHash = "hash")
        }
    }
    
    private val userRepository: UserRepository by lazy { 
        // 使用模拟的UserDao创建UserRepository实例
        UserRepository(mockUserDao)
    }
    
    // ViewModel现在通过各Activity中的ViewModelFactory创建，此处不再提供
    
    // NoteDetailViewModel现在通过NoteEditActivity中的ViewModelFactory创建，此处不再提供
    
    // Repository提供者方法
    // 注意：由于NoteRepository和TagRepository现在需要Context参数，这些方法已被禁用
    // 请通过各Activity中的ViewModelFactory获取对应的Repository和ViewModel实例
    
    fun provideUserRepository(): UserRepository {
        return userRepository
    }
}