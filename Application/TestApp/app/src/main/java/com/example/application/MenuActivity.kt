package com.example.application

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.application.databinding.ActivityMenuBinding

class MenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 예시 텍스트 표시
        binding.tvWelcome.text = "메뉴에 오신 걸 환영합니다!"
    }
}
