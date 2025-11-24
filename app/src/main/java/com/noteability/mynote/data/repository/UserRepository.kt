package com.noteability.mynote.data.repository

import com.noteability.mynote.data.dao.UserDao
import com.noteability.mynote.data.entity.User
import com.noteability.mynote.util.SecurityUtils

class UserRepository(private val userDao: UserDao) {

    suspend fun getUserByUsername(username: String): User? {
        return userDao.getUserByUsername(username)
    }

    suspend fun insertUser(user: User): Long {
        return userDao.insertUser(user)
    }

    /**
     * 校验用户名/密码，返回 User 对象（成功）或 null（失败）。
     * 注意：传入的 password 是明文，内部会 hash 再比对。
     */
    suspend fun loginAndGetUser(username: String, passwordPlain: String): User? {
        val user = userDao.getUserByUsername(username) ?: return null
        val hashed = SecurityUtils.sha256(passwordPlain)
        return if (user.passwordHash == hashed) user else null
    }
}
