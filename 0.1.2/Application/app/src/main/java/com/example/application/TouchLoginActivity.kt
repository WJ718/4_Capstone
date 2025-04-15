package com.example.application

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.application.databinding.ActivityTouchloginBinding
import com.example.application.WebSocketManager
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class TouchLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTouchloginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTouchloginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginBtn.setOnClickListener {
            val email = binding.loginEmail.text.toString().trim()
            val password = binding.loginPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 모두 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val body = FormBody.Builder()
                .add("email", email)
                .add("password", password)
                .build()

            val request = Request.Builder()
                .url("http://10.0.2.2:4141/auth/login")
                .post(body)
                .build()

            OkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@TouchLoginActivity, "서버 연결 실패", Toast.LENGTH_SHORT).show()
                        Log.e("Login", "연결 실패: ${e.message}")
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val bodyStr = response.body?.string()
                    Log.d("Login", "응답 수신: $bodyStr")

                    val json = JSONObject(bodyStr ?: "{}")
                    val token = json.optString("token", "")
                    val message = json.optString("message", "")
                    val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

                    runOnUiThread {
                        if (token.isNotEmpty()) {
                            prefs.edit().putString("jwt_token", token).putString("email", email).apply()
                            WebSocketManager.connect(email)
                            Toast.makeText(this@TouchLoginActivity, "로그인 성공", Toast.LENGTH_SHORT).show()

                            startActivity(Intent(this@TouchLoginActivity, MenuActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@TouchLoginActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }
    }
}
