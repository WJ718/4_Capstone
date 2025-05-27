package com.example.application

import android.app.Application
import android.content.Context

/**
 * 앱 전체에서 공통적으로 접근할 수 있는 Context 제공용 Application 클래스
 * WebSocketManager, Notification의 전역 Context 참조에 사용
 */

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }

    companion object {
        const val BASE_URL = "http://43.200.156.9:4141/"
        const val WS_URL = "ws://43.200.156.9:4141"

        lateinit var context: Context
            private set
    }
}
