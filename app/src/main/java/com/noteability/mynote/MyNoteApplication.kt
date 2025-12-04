package com.noteability.mynote

import android.app.Application
import android.content.Context
import com.noteability.mynote.data.AppDatabase
import com.noteability.mynote.data.entity.User
import com.noteability.mynote.di.ServiceLocator
import com.noteability.mynote.util.SecurityUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MyNoteApplication : Application() {

    companion object {
        // 添加全局 Context 供 MarkdownUtils 使用
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()

        // 设置全局 Context
        context = applicationContext
        
        // 设置ServiceLocator上下文
        ServiceLocator.setContext(this)

        // 初始化数据库
        AppDatabase.getDatabase(this)

        // 创建默认用户
        createDefaultUser()
    }

    private fun createDefaultUser() {
        GlobalScope.launch {
            val userDao = AppDatabase.getDatabase(this@MyNoteApplication).userDao()
            val defaultUser = userDao.getUserByUsername("default")

            if (defaultUser == null) {
                val hashedPassword = SecurityUtils.sha256("123456") // 默认密码
                val user = User(
                    username = "default",
                    passwordHash = hashedPassword,
                    email = "default@noteability.com"
                )
                userDao.insertUser(user)
            }
        }
    }
}