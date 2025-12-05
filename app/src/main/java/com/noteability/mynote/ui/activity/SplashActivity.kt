package com.noteability.mynote.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.noteability.mynote.R
import com.noteability.mynote.databinding.ActivitySplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 检查是否已经登录
        lifecycleScope.launch {
            delay(1500) // 显示启动页1.5秒
            checkLoginStatus()
        }
    }

    private fun checkLoginStatus() {
        // 直接跳转到登录页面，让用户可以选择登录或注册
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}