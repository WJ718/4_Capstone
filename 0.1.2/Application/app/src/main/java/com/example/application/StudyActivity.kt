package com.example.application

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.application.databinding.ActivityStudyBinding
import com.example.application.WebSocketManager
import okhttp3.Call
import okhttp3.OkHttp
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class StudyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudyBinding
    private var serial: String? = null
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // SharedPreferences에서 serial, email 가져오기
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        serial = prefs.getString("device_serial", null)
        email = prefs.getString("email", null)

        if (serial == null || email == null) {
            Toast.makeText(this, "기기번호 또는 이메일 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 종료 버튼 리스너
        binding.btnEndStudy.setOnClickListener {
            val url = "http://10.0.2.2:4141/record/end?email=$email&serial=$serial"
            val request = Request.Builder().url(url).get().build()

            OkHttpClient().newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    Log.e("StudyActivity", "요청 실패: ${e.message}")
                    runOnUiThread {
                        Toast.makeText(this@StudyActivity, "서버 요청 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    val result = response.body?.string()
                    Log.d("StudyActivity", "서버 응답: $result")

                    runOnUiThread {
                        Toast.makeText(this@StudyActivity, "학습 종료되었습니다.", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@StudyActivity, MenuActivity::class.java))
                        finish()
                    }
                }
            })
        }
    }
}
