# 4_Capstone
스터디 인바이어 팀

### [팀원]
강희준
서치우
윤원준

### 주제: Smart Study Desk App (졸음 방지 · 공기질 감지 · 학습 관리 앱)

[시스템 구성도](./images/configuration diagram.png)
- 본 애플리케이션은 라즈베리파이 기반의 스마트 책상과 연동되어, 졸음 감지 및 CO₂ 농도 알림, 일정 관리 기능을 제공합니다.  

### 시스템 구성
- 📱 Android 앱 (Kotlin)
- 📡 Node.js 서버 (WebSocket 실시간 통신)
- 🍓 Raspberry Pi (OpenCV + CO₂ 센서)
- 💾 MySQL (Sequelize ORM 사용)

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| 🔐 **JWT 인증** | 로그인 시 JWT 발급 후 SharedPreferences에 저장, SplashActivity에서 자동 로그인 처리 |
| 💤 **졸음 감지** | Raspberry Pi에서 dlib 기반 눈 감김 감지 → WebSocket 통해 앱에 알림 표시 |
| 🏭 **공기질 경고** | CO₂ 농도 2000ppm 초과 시 환기 알림 송신 (WebSocket 이용) |
| 🎛 **환경 설정** | 앱에서 밝기(Slider) · 소리(Switch) 제어 → 서버/기기 연동 |
| 📆 **학습 일정 관리** | 캘린더에 메모 기록 → SharedPreferences + 서버 기록 동시 저장 |
| ⏱ **학습 시작/종료 기록** | 시작/종료 시 서버에 기록 전송 (`/record/start`, `/record/end`) |

---
