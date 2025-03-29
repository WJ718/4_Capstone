const WebSocket = require('ws');

const connectedClients = {
    raspberry: new Map(),     // serial → WebSocket
    apps: new Map(),          // email → WebSocket
    deviceToUser: new Map(),  // serial → email  :: 사용자가 앱에서 시리얼 넘버 입력하고 "학습 시작" 누르면 연결됨.
};

function initializeWebSocket(server) {
    const wss = new WebSocket.Server({ server });

    wss.on('connection', (ws) => {
        console.log('WebSocket 연결됨 : 클라이언트 대기 중...');

        ws.on('message', (msg) => {
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

                // 졸음 감지 이벤트 처리
                else if (data.type === 'drowsy' && data.serial) {
                    const email = connectedClients.deviceToUser.get(data.serial);
                    const targetApp = connectedClients.apps.get(email);
                    if (targetApp) {
                        targetApp.send(JSON.stringify({ type: 'drawsy-alert' }));
                        console.log(`졸음 알림 전송 : ${email}`);
                    } else {
                        console.log(`대상 앱을 찾을 수 없습니다 (email: ${email})`);
                    }
                }

            } catch (err) {
                console.error('WebSocket 메시지 처리 오류:', err);
            }
        });

        ws.on('close', () => {
            // 라즈베리파이 연결 해제
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
