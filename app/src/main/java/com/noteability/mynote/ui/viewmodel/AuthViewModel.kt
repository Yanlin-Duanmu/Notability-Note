package com.noteability.mynote.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noteability.mynote.data.entity.User
import com.noteability.mynote.data.repository.UserRepository
import kotlinx.coroutines.launch

class AuthViewModel(private val userDao: UserRepository) : ViewModel() {

    fun register(username: String, passwordHash: String, email: String?, callback: (Long?) -> Unit) {
        viewModelScope.launch {
            val user = User(username = username, passwordHash = passwordHash, email = email)
            val id = userDao.insertUser(user)
            callback(id)
        }
    }

    fun login(username: String, passwordHash: String, callback: (User?) -> Unit) {
        viewModelScope.launch {
            val user = userDao.getUserByUsername(username)
            if (user != null && user.passwordHash == passwordHash) {
                callback(user)
            } else {
                callback(null)
            }
        }
    }
}
