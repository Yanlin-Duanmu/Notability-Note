package com.noteability.mynote.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.noteability.mynote.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            // 暂时不写逻辑，只确保能编译
        }
    }
}
