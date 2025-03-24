package com.example.application

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.application.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // signup 버튼 클릭 시 TouchSignupActivity로 이동
        binding.signup.setOnClickListener {
            val intent = Intent(this, TouchSignupActivity::class.java)
            startActivity(intent)
        }

        // login 버튼 클릭 시 TouchLoginActivity로 이동
        binding.login.setOnClickListener {
            val intent = Intent(this, TouchLoginActivity::class.java)
            startActivity(intent)
        }
    }
}