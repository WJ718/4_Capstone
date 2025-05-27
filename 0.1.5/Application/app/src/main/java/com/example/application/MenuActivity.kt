package com.example.application

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.application.databinding.ActivityMenuBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding
    private lateinit var prefs: SharedPreferences
    private var email: String? = null
    private var serial: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        email = prefs.getString("email", null)
        serial = prefs.getString("device_serial", null)

        if (email == null) {
            Toast.makeText(this, "로그인 정보가 없습니다", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 기기번호가 이미 저장돼 있다면 EditText에 표시
        if (!serial.isNullOrEmpty()) {
            binding.deviceId.setText(serial)
            Log.d("MenuActivity", "SharedPreferences에서 불러온 serial: $serial")
        }

        // 🔹 등록 버튼 → WebSocket 등록
        binding.registerBtn.setOnClickListener {
            val serialInput = binding.deviceId.text.toString().trim()

            if (serialInput.isEmpty()) {
                Toast.makeText(this, "기기번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit().putString("device_serial", serialInput).apply()
            Log.d("MenuActivity", "기기번호 등록됨: $serialInput")

            val wsJson = JSONObject().apply {
                put("type", "register")
                put("email", email)
                put("serial", serialInput)
            }

            WebSocketManager.sendMessage(wsJson.toString())
            Toast.makeText(this, "WebSocket에 기기번호 등록 완료", Toast.LENGTH_SHORT).show()
        }

        // 🔹 학습 시작 버튼 → 서버 HTTP 요청 + 화면 전환
        binding.studyBtn.setOnClickListener {
            val serialInput = binding.deviceId.text.toString().trim()

            if (serialInput.isEmpty()) {
                Toast.makeText(this, "기기번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit().putString("device_serial", serialInput).apply()
            Log.d("MenuActivity", "기기번호 저장됨 (학습 시작 시): $serialInput")

            run(serialInput)
            startActivity(Intent(this, StudyActivity::class.java))
        }

        // 설정 탭
        binding.tabSettings.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        // 캘린더 버튼
        binding.btnCalendar.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }
    }

    private fun run(serial: String) {
        val url = App.BASE_URL  + "record/start?email=$email&serial=$serial"
        val request = Request.Builder().url(url).build()

        OkHttpClient().newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                Log.e("MenuActivity", "HTTP 요청 실패: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                Log.d("MenuActivity", "HTTP 응답: ${response.code}")
            }
        })
    }
}
