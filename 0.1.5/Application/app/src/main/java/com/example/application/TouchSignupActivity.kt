package com.example.application

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.application.databinding.ActivityTouchsignupBinding
import com.example.application.WebSocketManager
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class TouchSignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTouchsignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTouchsignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.signupBtn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val body = FormBody.Builder()
                .add("email", email)
                .add("password", password)
                .build()

            val url = App.BASE_URL + "auth/signup"

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            OkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@TouchSignupActivity, "서버 연결 실패", Toast.LENGTH_SHORT).show()
                        Log.e("Signup", "연결 실패: ${e.message}")
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val bodyStr = response.body?.string()
                    Log.d("Signup", "응답 수신: $bodyStr")

                    val json = JSONObject(bodyStr ?: "{}")
                    val token = json.optString("token", "")
                    val message = json.optString("message", "")
                    val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

                    runOnUiThread {
                        if (token.isNotEmpty()) {
                            prefs.edit().putString("jwt_token", token).putString("email", email).apply()
                            WebSocketManager.connect(email)
                            Toast.makeText(this@TouchSignupActivity, "회원가입 완료", Toast.LENGTH_SHORT).show()

                            startActivity(Intent(this@TouchSignupActivity, MenuActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@TouchSignupActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }
    }
}
