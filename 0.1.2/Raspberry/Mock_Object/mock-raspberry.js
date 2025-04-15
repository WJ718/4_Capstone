const WebSocket = require('ws');

const serial = 'desk01'; // 테스트용 시리얼 넘버, 앱에서도 동일하게 입력할 것
const ws = new WebSocket('ws://localhost:4141'); // 서버 주소 (필요시 IP 변경 가능)

ws.on('open', () => {
    console.log('✅ WebSocket 연결 성공');

    // 서버에 라즈베리파이 등록 메시지 전송
    const registerMessage = {
        type: 'raspberry',
        serial: serial
    };
    ws.send(JSON.stringify(registerMessage));
    console.log(`📡 등록 메시지 전송: serial = ${serial}`);
});

ws.on('message', (msg) => {
    try {
        const data = JSON.parse(msg);
        console.log('📥 서버로부터 메시지 수신:', data);

        switch (data.command) {
            case 'start':
                console.log('🟢 학습 시작 명령 수신');
                break;
            case 'end':
                console.log('🔴 학습 종료 명령 수신');
                break;
            case 'set-sound':
                console.log(`🔊 경고음 조절 수신: ${data.value}`);
                break;
            case 'set-led':
                console.log(`💡 LED 밝기 조절 수신: ${data.value}`);
                break;
            default:
                console.log('❓ 알 수 없는 명령:', data);
        }
    } catch (err) {
        console.error('❌ 메시지 파싱 오류:', err);
    }
});

ws.on('close', () => {
    console.log('❌ WebSocket 연결 종료됨');
});
