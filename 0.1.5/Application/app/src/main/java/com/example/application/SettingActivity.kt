package com.example.application

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.application.databinding.ActivitySettingBinding
import com.example.application.WebSocketManager

class SettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingBinding
    private var serial: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // SharedPreferences에서 serial 불러오기
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        serial = prefs.getString("device_serial", null)

        if (serial == null) {
            Toast.makeText(this, "기기번호가 설정되지 않았습니다.\n학습 시작하기를 먼저 눌러주세요.", Toast.LENGTH_LONG).show()
            Log.e("SettingActivity", "serial is null")
            finish()
            return
        }

        Log.d("SettingActivity", "불러온 serial: $serial")

        // 밝기 조절 SeekBar 리스너
        binding.seekBarBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val ledControlMsg = """
                    {
                        "type": "led-control",
                        "serial": "$serial",
                        "value": "$progress"
                    }
                """.trimIndent()
                Log.d("WebSocket-SEND", "LED → $ledControlMsg")
                WebSocketManager.sendMessage(ledControlMsg)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 소리 설정 Switch 리스너
        binding.switchVolume.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            val soundControlMsg = """
                {
                    "type": "sound-control",
                    "serial": "$serial",
                    "value": "${if (isChecked) "on" else "off"}"
                }
            """.trimIndent()
            Log.d("WebSocket-SEND", "SOUND → $soundControlMsg")
            WebSocketManager.sendMessage(soundControlMsg)
        }

        binding.tabApp.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }
    }
}

