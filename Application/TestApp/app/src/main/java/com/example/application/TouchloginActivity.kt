package com.example.application

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.application.databinding.ActivityTouchloginBinding
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class TouchLoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTouchloginBinding
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTouchloginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 로그인 버튼 클릭 시 서버에 로그인 요청
        binding.loginBtn.setOnClickListener {
            loginUser()
        }
    }

    private fun loginUser() {
        // email , password를 body에 넣어서 POST 요청.
        val email = binding.loginEmail.text.toString().trim()
        val password = binding.loginPassword.text.toString().trim()

        // 입력값 검증
        if (email.isEmpty() || password.isEmpty()) {
            showToast("이메일과 비밀번호를 입력하세요.")
            return
        }

        // 이메일과 비밀번호 토대로 JSON 데이터 생성
        val json = Gson().toJson(LoginRequest(email, password))
        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), json)

        // POST 요청 전송
        val request = Request.Builder()
            .url("http://10.0.2.2:4141/auth/login") // 서버 URL (최근 수정된 곳)
            .post(requestBody)
            .build()

        Log.d("로그인 요청", "보낸 데이터: $json")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread { showToast("서버 요청 실패. 네트워크를 확인하세요.") }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string() ?: "{}"
                // 서버에서 res.status(201)이 되었을 때
                if (response.isSuccessful) {
                    // 성공 응답 시의 트리거 호출
                    handleSuccessResponse(responseData)
                } else {
                    runOnUiThread {
                        showToast("로그인 실패: $responseData")
                        Log.e("로그인 실패", responseData)
                    }
                }
            }
        })
    }

    private fun handleSuccessResponse(responseData: String) {
        try {
            val jsonObject = JSONObject(responseData)
            val message = jsonObject.optString("message", "로그인 성공")
            val token = jsonObject.optString("token", "")

            runOnUiThread {
                showToast(message)

                // 토큰을 sharedPreferences에 저장
                if (token.isNotEmpty()) {
                    Log.d("JWT", "서버로부터 받은 토큰: $token")
                    saveToken(token)
                }

                // TODO : 메인 화면으로 이동
                // startActivity(Intent(this, MenuActivity::class.java))
                finish()
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    // 서버에서 받은 토큰 preferences에(어플 전용 데이터저장소) 저장
    private fun saveToken(token: String) {
        val prefs: SharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        prefs.edit().putString("jwt_token", token).apply()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    data class LoginRequest(val email: String, val password: String)
}

