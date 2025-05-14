package com.example.application

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.application.WebSocketManager
import org.json.JSONObject
import java.util.Base64

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        // 토큰 검사 및 자동 로그인 흐름
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("jwt_token", null)
        val email = prefs.getString("email", null)

        if (token != null && email != null && isTokenValid(token)) {
            Log.d("SplashActivity", "유효한 토큰. 자동 로그인 진행. email: $email")

            // ✅ WebSocket 자동 연결
            WebSocketManager.connect(email)

            // ✅ MenuActivity로 이동
            Handler().postDelayed({
                startActivity(Intent(this, MenuActivity::class.java))
                finish()
            }, 1000)
        } else {
            Log.w("SplashActivity", "유효한 로그인 정보 없음. 로그인 화면 이동")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    // ✅ JWT 유효성 검사 (exp 시간 비교)
    @RequiresApi(Build.VERSION_CODES.O)
    private fun isTokenValid(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return false

            val payload = String(Base64.getDecoder().decode(parts[1]))
            val payloadJson = JSONObject(payload)
            val exp = payloadJson.getLong("exp")
            val currentTime = System.currentTimeMillis() / 1000

            Log.d("TokenCheck", "현재시간: $currentTime / 만료시간: $exp")
            currentTime < exp
        } catch (e: Exception) {
            Log.e("TokenCheck", "토큰 파싱 오류: ${e.message}")
            false
        }
    }
}
