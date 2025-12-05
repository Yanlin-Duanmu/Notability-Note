package com.noteability.mynote.data.repository

import com.noteability.mynote.data.dao.UserDao
import com.noteability.mynote.data.entity.User
import com.noteability.mynote.util.SecurityUtils

class UserRepository(private val userDao: UserDao) {

    suspend fun getUserByUsername(username: String): User? {
        return userDao.getUserByUsername(username)
    }

    suspend fun getUserById(userId: Long): User? {
        return userDao.getUserById(userId)
    }

    suspend fun insertUser(user: User): Long {
        return userDao.insertUser(user)
    }

    /**
     * 检查用户名是否已存在
     */
    suspend fun isUsernameExists(username: String): Boolean {
        return userDao.getUserByUsername(username) != null
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

    /**
     * 更新用户名
     * @param userId 用户ID
     * @param newUsername 新用户名
     * @return 是否更新成功
     */
    suspend fun updateUsername(userId: Long, newUsername: String): Boolean {
        return userDao.updateUsername(userId, newUsername) > 0
    }

    /**
     * 更新密码
     * @param userId 用户ID
     * @param newPasswordPlain 新密码（明文）
     * @return 是否更新成功
     */
    suspend fun updatePassword(userId: Long, newPasswordPlain: String): Boolean {
        val newPasswordHash = SecurityUtils.sha256(newPasswordPlain)
        return userDao.updatePassword(userId, newPasswordHash) > 0
    }

    /**
     * 验证旧密码是否正确
     * @param userId 用户ID
     * @param oldPasswordPlain 旧密码（明文）
     * @return 是否验证成功
     */
    suspend fun verifyPassword(userId: Long, oldPasswordPlain: String): Boolean {
        val user = userDao.getUserById(userId) ?: return false
        val oldPasswordHash = SecurityUtils.sha256(oldPasswordPlain)
        return user.passwordHash == oldPasswordHash
    }
}
