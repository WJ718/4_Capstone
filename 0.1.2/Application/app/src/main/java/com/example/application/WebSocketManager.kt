package com.example.application

import android.util.Log
import okhttp3.*

object WebSocketManager {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    fun connect(email: String) {
        val request = Request.Builder()
            .url("ws://10.0.2.2:4141")
            .build()

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

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "WebSocket 연결 실패: ${t.message}")
            }
        })
    }

    fun sendMessage(message: String) {
        Log.d("WebSocekt_SEND", message);
        webSocket?.send(message)
    }

    fun close() {
        webSocket?.close(1000, "앱 종료")
        webSocket = null
    }
}
