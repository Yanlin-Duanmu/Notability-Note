package com.noteability.mynote.ui.activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.noteability.mynote.R
import com.noteability.mynote.data.repository.UserRepository
import com.noteability.mynote.databinding.ActivitySettingsBinding
import com.noteability.mynote.databinding.DialogChangePasswordBinding
import com.noteability.mynote.databinding.DialogChangeUsernameBinding
import com.noteability.mynote.di.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var userRepository: UserRepository
    private lateinit var sharedPreferences: SharedPreferences
    private var loggedInUserId: Long = 0L
    private lateinit var currentUsername: String
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化共享首选项
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        loggedInUserId = sharedPreferences.getLong("logged_in_user_id", 0L)

        // 从ServiceLocator获取UserRepository
        userRepository = ServiceLocator.provideUserRepository()

        // 设置Toolbar和侧边栏
        setupToolbarAndDrawer()

        // 加载当前用户信息
        loadCurrentUserInfo()

        // 设置按钮点击事件
        setupButtonListeners()
    }

    private fun setupToolbarAndDrawer() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,    // DrawerLayout 实例
            binding.toolbar,         // Toolbar 实例
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // 设置侧边栏选中项为设置
        binding.navigationView.setCheckedItem(R.id.nav_settings)
        
        setupNavigationDrawerListener()
    }

    private fun setupNavigationDrawerListener() {
        binding.navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_notes -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                R.id.nav_tags -> {
                    startActivity(Intent(this, TagManagementActivity::class.java))
                    finish()
                }
            }
            binding.drawerLayout.closeDrawer(Gravity.START)
            true
        }
    }

    private fun loadCurrentUserInfo() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val user = userRepository.getUserById(loggedInUserId)
                withContext(Dispatchers.Main) {
                    if (user != null) {
                        currentUsername = user.username
                        binding.welcomeText.text = "Hi, ${user.username}"
                    }
                }
            }
        }
    }

    private fun setupButtonListeners() {
        // 设置修改用户名选项的点击事件
        binding.usernameOption.setOnClickListener {
            showChangeUsernameDialog()
        }

        // 设置修改密码选项的点击事件
        binding.passwordOption.setOnClickListener {
            showChangePasswordDialog()
        }

        // 设置退出登录选项的点击事件
        binding.logoutOption.setOnClickListener {
            logout()
        }
    }

    private fun validateNewUsername(newUsername: String): Boolean {
        if (newUsername.isEmpty()) {
            showToast("请输入新用户名")
            return false
        }

        if (newUsername.length < 3) {
            showToast("用户名长度不能少于3个字符")
            return false
        }

        if (newUsername == currentUsername) {
            showToast("新用户名不能与当前用户名相同")
            return false
        }

        return true
    }

    private fun validatePasswordChange(oldPassword: String, newPassword: String, confirmPassword: String): Boolean {
        if (oldPassword.isEmpty()) {
            showToast("请输入旧密码")
            return false
        }

        if (newPassword.isEmpty()) {
            showToast("请输入新密码")
            return false
        }

        if (newPassword.length < 6) {
            showToast("密码长度不能少于6个字符")
            return false
        }

        if (newPassword != confirmPassword) {
            showToast("两次输入的新密码不一致")
            return false
        }

        return true
    }

    private fun showChangeUsernameDialog() {
        val dialogBinding = DialogChangeUsernameBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setTitle("修改用户名")
            .setView(dialogBinding.root)
            .setPositiveButton("确认") { _, _ -> 
                val newUsername = dialogBinding.newUsernameEditText.text.toString().trim()
                if (validateNewUsername(newUsername)) {
                    updateUsername(newUsername, dialogBinding)
                }
            }
            .setNegativeButton("取消", null)
            .create()

        dialog.show()
    }

    private fun updateUsername(newUsername: String, dialogBinding: DialogChangeUsernameBinding) {
        dialogBinding.loadingIndicator.visibility = View.VISIBLE

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                // 检查用户名是否已存在
                val isUsernameExists = userRepository.isUsernameExists(newUsername)
                if (isUsernameExists) {
                    withContext(Dispatchers.Main) {
                        showToast("用户名已存在")
                        dialogBinding.loadingIndicator.visibility = View.GONE
                    }
                    return@withContext
                }

                // 更新用户名
                val isSuccess = userRepository.updateUsername(loggedInUserId, newUsername)
                withContext(Dispatchers.Main) {
                    dialogBinding.loadingIndicator.visibility = View.GONE

                    if (isSuccess) {
                        showToast("用户名更新成功")
                        currentUsername = newUsername
                        loadCurrentUserInfo() // 重新加载用户信息以更新欢迎文本
                    } else {
                        showToast("用户名更新失败")
                    }
                }
            }
        }
    }

    private fun showChangePasswordDialog() {
        val dialogBinding = DialogChangePasswordBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setTitle("修改密码")
            .setView(dialogBinding.root)
            .setPositiveButton("确认") { _, _ -> 
                val oldPassword = dialogBinding.oldPasswordEditText.text.toString()
                val newPassword = dialogBinding.newPasswordEditText.text.toString()
                val confirmPassword = dialogBinding.confirmPasswordEditText.text.toString()
                if (validatePasswordChange(oldPassword, newPassword, confirmPassword)) {
                    updatePassword(oldPassword, newPassword, dialogBinding)
                }
            }
            .setNegativeButton("取消", null)
            .create()

        dialog.show()
    }

    private fun updatePassword(oldPassword: String, newPassword: String, dialogBinding: DialogChangePasswordBinding) {
        dialogBinding.loadingIndicator.visibility = View.VISIBLE

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                // 验证旧密码
                val isPasswordCorrect = userRepository.verifyPassword(loggedInUserId, oldPassword)
                if (!isPasswordCorrect) {
                    withContext(Dispatchers.Main) {
                        showToast("旧密码错误")
                        dialogBinding.loadingIndicator.visibility = View.GONE
                    }
                    return@withContext
                }

                // 更新密码
                val isSuccess = userRepository.updatePassword(loggedInUserId, newPassword)
                withContext(Dispatchers.Main) {
                    dialogBinding.loadingIndicator.visibility = View.GONE

                    if (isSuccess) {
                        showToast("密码更新成功")
                    } else {
                        showToast("密码更新失败")
                    }
                }
            }
        }
    }

    private fun logout() {
        // 清除共享首选项中的登录状态
        sharedPreferences.edit().remove("logged_in_user_id").apply()

        // 跳转到登录页面
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(Gravity.START)) {
            binding.drawerLayout.closeDrawer(Gravity.START)
        } else {
            super.onBackPressed()
        }
    }
}
