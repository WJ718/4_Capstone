package com.example.application

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.application.databinding.ActivityCalendarBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
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
        for ((key, value) in pref.all) {
            addScheduleToList(key, value.toString())
        }

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = "$year-${month + 1}-$dayOfMonth"
            showMemoDialog(selectedDate)
        }
    }

    private fun showMemoDialog(date: String, existingMemo: String? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_memo, null)
        val memoEditText = dialogView.findViewById<EditText>(R.id.memoEditText)
        memoEditText.setText(existingMemo ?: "")

        AlertDialog.Builder(this)
            .setTitle("$date 일정")
            .setView(dialogView)
            .setPositiveButton("저장") { _, _ ->
                val memo = memoEditText.text.toString()
                val userPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                val email = userPref.getString("email", "") ?: ""

                val request = Request.Builder()
                    .url("http://10.0.2.2:4141/schedule/upload")
                    .post(
                        FormBody.Builder()
                            .add("email", email)
                            .add("date", date)
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
                                val pref = getSharedPreferences("schedule", MODE_PRIVATE)
                                pref.edit().putString(date, memo).apply()
                                addScheduleToList(date, memo)
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

    private fun addScheduleToList(date: String, memo: String) {
        val container = findViewById<LinearLayout>(R.id.scheduleListContainer)

        // 중복 일정 제거
        val existingView = container.findViewWithTag<View>(date)
        if (existingView != null) {
            container.removeView(existingView)
        }

        val itemView = layoutInflater.inflate(R.layout.item_schedule, container, false)
        itemView.tag = date

        val textView = itemView.findViewById<TextView>(R.id.scheduleText)
        val editBtn = itemView.findViewById<Button>(R.id.editButton)
        val deleteBtn = itemView.findViewById<Button>(R.id.deleteButton)

        textView.text = "$date: $memo"

        editBtn.setOnClickListener {
            showMemoDialog(date, memo)
        }

        deleteBtn.setOnClickListener {
            val pref = getSharedPreferences("schedule", MODE_PRIVATE)
            pref.edit().remove(date).apply()
            container.removeView(itemView)

            val email = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("email", "") ?: ""
            val request = Request.Builder()
                .url("http://10.0.2.2:4141/schedule/remove")
                .post(
                    FormBody.Builder()
                        .add("email", email)
                        .add("date", date)
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
                        Toast.makeText(this@CalendarActivity, "서버 삭제 완료 (${response.code})", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }

        container.addView(itemView)
    }
}
