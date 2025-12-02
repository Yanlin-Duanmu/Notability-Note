package com.noteability.mynote.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.noteability.mynote.MainActivity
import com.noteability.mynote.databinding.ActivityLoginBinding
import com.noteability.mynote.di.ServiceLocator
import com.noteability.mynote.ui.viewmodel.AuthViewModel
import com.noteability.mynote.ui.viewmodel.AuthViewModelFactory

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authViewModel: AuthViewModel
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化ViewModel
        val userRepository = ServiceLocator.provideUserRepository()
        val viewModelFactory = AuthViewModelFactory(userRepository)
        authViewModel = viewModelFactory.create(AuthViewModel::class.java)

        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun performLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

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

        // 隐藏错误信息
        binding.tvError.visibility = android.view.View.GONE

        // 调用登录方法
        authViewModel.login(username, password) { user ->
            if (user != null) {
                // 登录成功，保存用户信息
                Log.d(TAG, "Login successful for user: ${user.username}")
                saveLoggedInUser(user.userId)
                // 更新ServiceLocator中的当前用户ID，确保数据隔离正确工作
                ServiceLocator.updateLoggedInUserId(user.userId)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                // 登录失败，显示错误信息
                binding.tvError.text = "用户名或密码错误"
                binding.tvError.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun saveLoggedInUser(userId: Long) {
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong("logged_in_user_id", userId).apply()
    }
}
