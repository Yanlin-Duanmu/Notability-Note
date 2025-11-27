package com.noteability.mynote

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化界面组件
        val welcomeText = findViewById<TextView>(R.id.welcomeText)
        val testDatabaseButton = findViewById<Button>(R.id.testDatabaseButton)

        // 设置欢迎信息
        welcomeText.text = "欢迎使用 MyNote 应用"

        // 设置按钮点击事件，导航到数据库测试界面
        testDatabaseButton.setOnClickListener {
            val intent = Intent(this, DatabaseTestActivity::class.java)
            startActivity(intent)
        }
    }
}