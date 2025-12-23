package com.noteability.mynote

import android.app.Application
import android.content.Context
import com.noteability.mynote.data.AppDatabase
import com.noteability.mynote.data.entity.User
import com.noteability.mynote.di.ServiceLocator
import com.noteability.mynote.ui.component.WebViewManager
import com.noteability.mynote.util.SecurityUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MyNoteApplication : Application() {

    companion object {
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()

        context = applicationContext
        ServiceLocator.setContext(this)
        AppDatabase.getDatabase(this)
        
        // Prewarm WebView for instant editor loading
        WebViewManager.prewarm(this)
        
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