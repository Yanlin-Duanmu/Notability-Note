package com.noteability.mynote.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noteability.mynote.data.entity.User
import com.noteability.mynote.data.repository.UserRepository
import com.noteability.mynote.util.SecurityUtils
import kotlinx.coroutines.launch

class AuthViewModel(private val userDao: UserRepository) : ViewModel() {

    fun register(username: String, password: String, email: String?, callback: (Long?) -> Unit) {
        viewModelScope.launch {
            // 对密码进行哈希处理
            val passwordHash = SecurityUtils.sha256(password)
            val user = User(username = username, passwordHash = passwordHash, email = email)
            val id = userDao.insertUser(user)
            callback(id)
        }
    }

    fun login(username: String, password: String, callback: (User?) -> Unit) {
        viewModelScope.launch {
            // 使用UserRepository中的loginAndGetUser方法，内部会处理密码哈希
            val user = userDao.loginAndGetUser(username, password)
            callback(user)
        }
    }
}
