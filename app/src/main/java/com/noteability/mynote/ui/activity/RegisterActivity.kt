package com.noteability.mynote.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.noteability.mynote.MainActivity
import com.noteability.mynote.databinding.ActivityRegisterBinding
import com.noteability.mynote.di.ServiceLocator
import com.noteability.mynote.ui.viewmodel.AuthViewModel
import com.noteability.mynote.ui.viewmodel.AuthViewModelFactory

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authViewModel: AuthViewModel
    private val TAG = "RegisterActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化ViewModel
        val userRepository = ServiceLocator.provideUserRepository()
        val viewModelFactory = AuthViewModelFactory(userRepository)
        authViewModel = viewModelFactory.create(AuthViewModel::class.java)

        binding.btnRegister.setOnClickListener {
            performRegistration()
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun performRegistration() {
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // 表单验证
        if (username.isEmpty()) {
            binding.tvError.text = "用户名不能为空"
            binding.tvError.visibility = android.view.View.VISIBLE
            return
        }

        if (password.isEmpty()) {
            binding.tvError.text = "密码不能为空"
            binding.tvError.visibility = android.view.View.VISIBLE
            return
        }

        if (password != confirmPassword) {
            binding.tvError.text = "两次输入的密码不一致"
            binding.tvError.visibility = android.view.View.VISIBLE
            return
        }

        if (password.length < 6) {
            binding.tvError.text = "密码长度至少需要6个字符"
            binding.tvError.visibility = android.view.View.VISIBLE
            return
        }

        // 隐藏错误信息
        binding.tvError.visibility = android.view.View.GONE

        // 检查用户名是否已存在
        checkUsernameExists(username) { exists ->
            if (exists) {
                binding.tvError.text = "用户名已存在"
                binding.tvError.visibility = android.view.View.VISIBLE
            } else {
                // 调用注册方法
                authViewModel.register(username, password, email) { userId ->
                    if (userId != null) {
                        // 注册成功，保存用户信息
                        Log.d(TAG, "Registration successful for user: $username with id: $userId")
                        saveLoggedInUser(userId)
                        // 更新ServiceLocator中的当前用户ID，确保数据隔离正确工作
                        ServiceLocator.updateLoggedInUserId(userId)
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        // 注册失败，显示错误信息
                        binding.tvError.text = "注册失败，请重试"
                        binding.tvError.visibility = android.view.View.VISIBLE
                    }
                }
            }
        }
    }

    private fun checkUsernameExists(username: String, callback: (Boolean) -> Unit) {
        val userRepository = ServiceLocator.provideUserRepository()
        lifecycleScope.launch {
            val exists = kotlin.runCatching {
                userRepository.isUsernameExists(username)
            }.getOrDefault(false)

            callback(exists)
        }
    }

    private fun saveLoggedInUser(userId: Long) {
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong("logged_in_user_id", userId).apply()
    }
}