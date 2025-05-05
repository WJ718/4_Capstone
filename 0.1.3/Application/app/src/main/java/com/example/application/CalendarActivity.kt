package com.example.application

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.application.databinding.ActivityCalendarBinding
import okhttp3.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCalendarBinding
    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pref = getSharedPreferences("schedule", MODE_PRIVATE)
        for ((timestamp, memo) in pref.all) {
            addScheduleToList(timestamp, memo.toString())
        }

        // 캘린더의 날짜 부분 클릭 시 일정 추가 메서드 호출
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            showMemoDialog()
        }
    }

    private fun showMemoDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_memo, null)
        val memoEditText = dialogView.findViewById<EditText>(R.id.memoEditText)

        // 다이얼로그 설정
        AlertDialog.Builder(this)
            .setTitle("$selectedDate 일정 추가")
            .setView(dialogView)
            .setPositiveButton("저장") { _, _ ->
                val memo = memoEditText.text.toString()
                val timestamp = getCurrentTimestamp()
                val userPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                val email = userPref.getString("email", "") ?: ""

                val request = Request.Builder()
                    .url("http://10.0.2.2:4141/schedule/upload")
                    .post(
                        FormBody.Builder()
                            .add("email", email)
                            .add("date", timestamp)
                            .add("memo", memo)
                            .build()
                    )
                    .build()

                OkHttpClient().newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        runOnUiThread {
                            Toast.makeText(this@CalendarActivity, "서버 전송 실패", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            runOnUiThread {
                                getSharedPreferences("schedule", MODE_PRIVATE)
                                    .edit()
                                    .putString(timestamp, memo)
                                    .apply()
                                addScheduleToList(timestamp, memo)
                                Toast.makeText(this@CalendarActivity, "일정 저장 성공", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@CalendarActivity, "서버 오류: ${response.code}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                })
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // 일정 표 관리 메서드
    private fun addScheduleToList(timestamp: String, memo: String) {
        val container = findViewById<LinearLayout>(R.id.scheduleListContainer)

        val itemView = layoutInflater.inflate(R.layout.item_schedule, container, false)
        itemView.tag = timestamp

        val textView = itemView.findViewById<TextView>(R.id.scheduleText)
        val editBtn = itemView.findViewById<Button>(R.id.editButton)
        val deleteBtn = itemView.findViewById<Button>(R.id.deleteButton)

        textView.text = "$memo"

        // 수정 버튼 이벤트 리스너
        editBtn.setOnClickListener {
            showEditDialog(timestamp, memo)
        }

        // 삭제 버튼 이벤트 리스너
        deleteBtn.setOnClickListener {
            getSharedPreferences("schedule", MODE_PRIVATE).edit().remove(timestamp).apply()
            container.removeView(itemView)

            val email = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("email", "") ?: ""
            val request = Request.Builder()
                .url("http://10.0.2.2:4141/schedule/remove")
                .post(
                    FormBody.Builder()
                        .add("email", email)
                        .add("date", timestamp)
                        .build()
                )
                .build()

            OkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@CalendarActivity, "서버 삭제 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        Toast.makeText(this@CalendarActivity, "삭제 완료 (${response.code})", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }

        container.addView(itemView)
    }

    // 수정메서드
    private fun showEditDialog(timestamp: String, oldMemo: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_memo, null)
        val memoEditText = dialogView.findViewById<EditText>(R.id.memoEditText)
        memoEditText.setText(oldMemo)

        AlertDialog.Builder(this)
            .setTitle("일정 수정")
            .setView(dialogView)
            .setPositiveButton("수정") { _, _ ->
                val newMemo = memoEditText.text.toString()
                val email = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("email", "") ?: ""

                val request = Request.Builder()
                    .url("http://10.0.2.2:4141/schedule/update")
                    .post(
                        FormBody.Builder()
                            .add("email", email)
                            .add("date", timestamp)
                            .add("memo", newMemo)
                            .build()
                    )
                    .build()

                OkHttpClient().newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        runOnUiThread {
                            Toast.makeText(this@CalendarActivity, "서버 수정 실패", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        runOnUiThread {
                            getSharedPreferences("schedule", MODE_PRIVATE)
                                .edit()
                                .putString(timestamp, newMemo)
                                .apply()
                            recreate() // UI 갱신을 위해 액티비티 리로드
                            Toast.makeText(this@CalendarActivity, "수정 완료", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun getCurrentTimestamp(): String {
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(Date(now))
    }
}
