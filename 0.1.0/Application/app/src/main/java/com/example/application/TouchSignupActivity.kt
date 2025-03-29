package com.example.application

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.application.databinding.ActivityTouchsignupBinding
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class TouchSignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTouchsignupBinding
    private val client = OkHttpClient()
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTouchsignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 가입 버튼 클릭 이벤트
        binding.signupBtn.setOnClickListener {
            sendDataToServer()
        }
    }

    private fun saveEmail(email: String) {
        val prefs: SharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        prefs.edit().putString("user_email", email).apply()
    }

    private fun sendDataToServer() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // 입력값 검증
        if (email.isEmpty() || password.isEmpty()) {
            showToast("이메일과 비밀번호를 입력하세요.")
            return
        }

        // JSON 데이터 생성 (Gson 사용)
        val json = Gson().toJson(SignupRequest(email, password))
        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), json)

        // POST 요청
        val request = Request.Builder()
            .url("http://10.0.2.2:4141/auth/signup")
            .post(requestBody)
            .build()

        Log.d("회원가입 요청", "보낸 데이터: $json")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread { showToast("서버 요청 실패. 네트워크를 확인하세요.") }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string() ?: "{}"
                if (response.isSuccessful) {
                    handleSuccessResponse(responseData)
                } else {
                    runOnUiThread {
                        showToast("회원가입 실패: $responseData")
                        Log.e("회원가입 실패", responseData)
                    }
                }
            }
        })
    }

    private fun handleSuccessResponse(responseData: String) {
        try {
            val jsonObject = JSONObject(responseData)
            val message = jsonObject.optString("message", "회원가입 완료")
            val token = jsonObject.optString("token", "")

            runOnUiThread {
                showToast(message)


                if (token.isNotEmpty()) {
                    Log.d("JWT", "서버로부터 받은 토큰: $token")
                    saveToken(token)
                    saveEmail(email)
                }
                // 메뉴 화면으로 이동
                startActivity(Intent(this, MenuActivity::class.java))
                finish()
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun saveToken(token: String) {
        val prefs: SharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        prefs.edit().putString("jwt_token", token).apply()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    data class SignupRequest(val email: String, val password: String)
}
