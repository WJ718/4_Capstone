package com.example.application

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import okhttp3.*

import org.json.JSONObject

object WebSocketManager {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    // okhttp3.WebSocketListener 라이브러리의 메서드를 사용해서 작성
    fun connect(email: String) {
        val request = Request.Builder()
            .url("ws://10.0.2.2:4141") // 서버 주소는 그대로 유지
            .build()

        // 앱이 서버(ws://10.0.2.2:4141)에 연결 후 ws.send를 통해 이메일정보를 전송 (앱 등록)
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                val json = """
                    {
                        "type": "app",
                        "email": "$email"
                    }
                """.trimIndent()
                ws.send(json)
                Log.d("WebSocket", "앱 등록 완료 (email: $email)")
            }

            // 서버로부터 메시지를 받는 부분
            override fun onMessage(ws: WebSocket, text: String) {
                Log.d("WebSocket", "메시지 수신: $text")

                try {
                    val json = JSONObject(text)
                    val type = json.getString("type")

                    when (type) {
                        // 졸음 감지 신호 작동 시 알림처리
                        /* ws.js 부분
                            const targetApp = connectedClients.apps.get(email);
                            targetApp?.send(JSON.stringify({ type: 'sleepy-alert' }));
                        */
                        "sleepy-alert" -> {
                            showNotification("⚠ 졸음 감지", "사용자가 졸음 상태로 판단되었습니다.")
                        }

                        "co2-alert" -> {
                            showNotification("⚠ 환기 요청", "이산화탄소 농도가 높습니다. 환기해주세요.")
                        }
                    }

                } catch (e: Exception) {
                    Log.e("WebSocket", "JSON 파싱 오류: ${e.message}")
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "연결 실패: ${t.message}")
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "연결 종료: $reason")
            }
        })
    }

    fun sendMessage(message: String) {
        if (webSocket != null) {
            webSocket!!.send(message)
            Log.d("WebSocket", "메시지 전송: $message")
        } else {
            Log.w("WebSocket", "WebSocket이 아직 연결되지 않았습니다.")
        }
    }


    private fun showNotification(title: String, message: String) {
        val context = App.context
        val channelId = "user_alert_channel"

        // 알림 채널 생성 (Android 8 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "알림 채널"
            val descriptionText = "서버로부터 받은 알림 신호 처리"
            val importance = NotificationManager.IMPORTANCE_HIGH

            // 채널 객체 생성
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }

            // NotificationManager에 channel 등록
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // 알림 객체 생성
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}
