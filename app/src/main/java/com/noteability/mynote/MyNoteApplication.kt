package com.noteability.mynote

import android.app.Application
import com.noteability.mynote.data.AppDatabase
import com.noteability.mynote.data.entity.User
import com.noteability.mynote.di.ServiceLocator
import com.noteability.mynote.util.SecurityUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MyNoteApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
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