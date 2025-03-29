[주요 변경/추가된 기능]
!! 코드 구현 O , 테스트 X  !!

-서버-
* 서버에서 라즈베리파이로 졸음 검사 신호 보내는 모듈 추가(controllers/record.js)
예시) /* [앱] → GET /record/start?serial=0000ABCDEF → [서버] → WebSocket → [라즈베리파이] */
* 라즈베리파이 등록 기능 추가(ws.js)
* 앱 등록 기능 추가(ws.js)

-애플리케이션-
* "학습 시작" 버튼 및 라우터 요청 추가 (MenuActivity.kt)

-라즈베리파이 모듈-
* 시리얼 넘버 검색 함수 추가 (ver_윤원준/last_last.py/get_serial_number())
* 라즈베리파이 전원 ON 시 웹 소켓으로 연결 구현 (ver_윤원준/last_last.py/run_websocket())
* 서버로부터 start 명령 받을 시 졸음 감지 시작 기능 추가(ver_윤원준/last_last.py/on_message())
