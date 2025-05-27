const WebSocket = require('ws');
const { Record } = require('./models');
const connectedClients = {
    // ws 연결 객체(WebSocket 객체)를 저장
    raspberry: new Map(),     // serial → WebSocket
    apps: new Map(),          // email → WebSocket
    deviceToUser: new Map(),  // serial → email  :: 사용자가 앱에서 시리얼 넘버 입력하고 "학습 시작" 누르면 연결됨.
};

function initializeWebSocket(server) {
    const wss = new WebSocket.Server({ server });

    // ws: 명시적인 매개변수가 아니라, ws.on 등을 시행하면 자동으로 전달되는 객체
    wss.on('connection', (ws) => {
        console.log('WebSocket 연결됨 : 클라이언트 대기 중...');

        // 기기 등록
        ws.on('message', async (msg) => {
            try {
                const data = JSON.parse(msg);


                // 라즈베리파이 등록
                if (data.type === 'raspberry' && data.serial) {
                    connectedClients.raspberry.set(data.serial, ws);
                    console.log(`라즈베리파이 등록 완료 (Serial: ${data.serial})`);
                }

                // 앱 등록
                else if (data.type === 'app' && data.email) {
                    connectedClients.apps.set(data.email, ws);
                    console.log(`앱 등록 완료: ${data.email}`);
                }

                // 앱이 등록한 시리얼 넘버 알림
                else if (data.type === 'register' && data.serial && data.email) {
                    const email = data.email;
                    const serial = data.serial;
                    connectedClients.deviceToUser.set(serial, email);
                    console.log(`앱: ${email} - serial: ${serial}`);
                }

                // 졸음 감지 수신 처리
                else if (data.type === 'sleepy' && data.serial) {
                    // 해당 신호를 보낸 시리얼넘버에 맞는 이메일 검색
                    const email = connectedClients.deviceToUser.get(data.serial);
                    // 그 이메일에 맞는 웹 소켓 대상 검색
                    const targetApp = connectedClients.apps.get(email);
                    // 발견 시 JSON 메시지 발송
                    if (targetApp) {
                        targetApp.send(JSON.stringify({ type: 'sleepy-alert' }));
                        console.log(`졸음 알림 전송 : ${email}`);

                        if (email) {
                            await Record.create({
                                email,
                                date: new Date(),
                                is_sleep: true
                            });
                            console.log(`졸음 기록 저장 완료 (email: ${email})`);
                        }
                    } else {
                        console.log(`대상 앱을 찾을 수 없습니다 (email: ${email})`);
                    }

                    
                }

                // 졸음 감지 수신 처리
                else if (data.type === 'co2' && data.serial) {
                    // 해당 신호를 보낸 시리얼넘버에 맞는 이메일 검색
                    const email = connectedClients.deviceToUser.get(data.serial);
                    // 그 이메일에 맞는 웹 소켓 대상 검색
                    const targetApp = connectedClients.apps.get(email);
                    // 발견 시 JSON 메시지 발송
                    if (targetApp) {
                        if (email) {
                            await Record.create({
                                email,
                                date: new Date(),
                                co2_high: true
                            });
                            console.log(`졸음 기록 저장 완료 (email: ${email})`);
                        }

                        targetApp.send(JSON.stringify({ type: 'co2-alert' }));
                        console.log(`이산화탐소 감지 알림 전송 : ${email}`);
                    } else {
                        console.log(`대상 앱을 찾을 수 없습니다 (email: ${email})`);
                    }
                }

                // 경고음 소리 조절
                else if (data.type === 'sound-control' && data.serial) {
                    const raspberry = connectedClients.raspberry.get(data.serial);
                    if (raspberry) {
                        raspberry.send(JSON.stringify({
                            command : 'set-sound',
                            value : data.value // 라즈베리로 변화된 소리값 전송
                        }));
                        console.log(`경고음 세기 조절 명령 전송 → Serial: ${data.serial}, Value: ${data.value}`);
                    } else {
                        console.log('해당 라즈베리파이 연결 안됨');
                    }
                }

                // LED 밝기 조절 처리
                /* 
                    애플리케이션에서 바 조절 시 이벤트리스너에 의해 value(숫자형)이 sharedPreferences에 저장, 그 후 웹소켓으로 'led-control' 메시지 전송
                */
                else if (data.type === 'led-control' && data.serial) {
                    const raspberry = connectedClients.raspberry.get(data.serial);
                    if (raspberry) {
                        raspberry.send(JSON.stringify({
                            command : 'set-led',
                            value : data.value // 라즈베리로 변화된 밝기값 전송
                        }));
                        console.log(`LED 밝기 조절 명령 전송 → Serial: ${data.serial}, Value: ${data.value}`);
                    } else {
                        console.log('해당 라즈베리파이 연결 안됨');
                    }
                }
            } catch (err) {
                console.error('WebSocket 메시지 처리 오류:', err);
            }
        });

        // 라즈베리파이 연결 해제
        ws.on('close', () => {
           
            for (const [serial, socket] of connectedClients.raspberry.entries()) {
                if (socket === ws) {
                    connectedClients.raspberry.delete(serial);
                    connectedClients.deviceToUser.delete(serial); // 매핑도 정리
                    console.log(`라즈베리파이 연결 해제 (Serial: ${serial})`);
                    break;
                }
            }

            // 앱 연결 해제
            for (const [email, socket] of connectedClients.apps.entries()) {
                if (socket === ws) {
                    connectedClients.apps.delete(email);
                    // 앱은 종료되더라도 매핑이 끊기지 않음.
                    console.log(`앱 연결 해제: ${email}`);
                    break;
                }
            }
        });
    });
}

module.exports = { initializeWebSocket, connectedClients };
