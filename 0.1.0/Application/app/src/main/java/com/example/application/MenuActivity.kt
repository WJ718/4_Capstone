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

        binding.studyBtn.setOnClickListener({
            start()
        })
    }

    // GET /record/start
    private fun start() {
        val serial = binding.deviceId.text.toString().trim()

        if (email != null) {
            val req = "http://10.0.2.2:4141/record/start?email=$email&serial=$serial"
            // ex http://10.0.2.2:4141/record/start?email=test@example.com&device_id=desk01


            // API 요청 실행
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder()
                .url(req)
                .get()
                .build()

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(this@MenuActivity, "서버 요청 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    val body = response.body?.string()

                    runOnUiThread {
                        if (response.isSuccessful) {
                            Toast.makeText(this@MenuActivity, "서버 요청 성공: $body", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MenuActivity, "요청 실패: ${response.code}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }
    }
}
