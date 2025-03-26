package com.example.application

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.application.databinding.ActivityMenuBinding
import java.io.IOException

class MenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMenuBinding
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        email = sharedPref.getString("user_email", null)
    }

}
