package com.example.application

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs: SharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val token = prefs.getString("jwt_token", null)

        if (token.isNullOrEmpty() || isTokenExpired(token)) {
            // 토큰이 없거나 만료됨 → 로그인 화면
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // 토큰 유효함 → 메뉴 화면
            startActivity(Intent(this, MenuActivity::class.java))
        }

        finish()
    }

    private fun isTokenExpired(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return true

            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val json = JSONObject(payload)

            val exp = json.getLong("exp") // 초 단위
            val now = System.currentTimeMillis() / 1000 // 현재 시간 (초 단위)

            Log.d("JWT", "토큰 만료 시간: $exp, 현재 시간: $now")

            now >= exp // 현재 시간이 만료 시간 이후면 true
        } catch (e: Exception) {
            e.printStackTrace()
            true // 파싱 오류 시 만료된 것으로 간주
        }
    }
}
