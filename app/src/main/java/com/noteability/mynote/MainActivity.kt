package com.noteability.mynote

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.noteability.mynote.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 绑定 XML
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 示例：显示欢迎文字
        binding.textViewHello.text = "Hello Android!"
    }
}
