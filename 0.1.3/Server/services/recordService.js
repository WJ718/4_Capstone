const {connectedClients} = require('../ws');

exports.start = async (email, serial) => {
    if(!email || !serial) {
        const error = new Error("이메일과 시리얼 넘버가 필요합니다.");
        error.statusCode = 400;
        throw error;
    }

    const raspberrySocket = connectedClients.raspberry.get(serial);

    if(raspberrySocket && raspberrySocket.readyState === 1) {
        // email ↔ serial 매핑 저장
        connectedClients.deviceToUser.set(serial, email);

        // start 명령 전송
        raspberrySocket.send(JSON.stringify({
            command: 'start',
            serial: serial
        }));

        console.log(`start 명령 전송 → 라즈베리파이 (Serial: ${serial}, Email: ${email})`);
    }
}

exports.end = async (email, serial) => {
    if(!email || !serial) {
        const error = new Error("이메일과 시리얼 넘버가 필요합니다.");
        error.statusCode = 400;
        throw error;
    }

    const raspberrySocket = connectedClients.raspberry.get(serial);

    if (raspberrySocket && raspberrySocket.readyState === 1) {
        // email ↔ serial 매핑 저장
        connectedClients.deviceToUser.set(serial, email);

        // end 명령 전송
        raspberrySocket.send(JSON.stringify({
            command: 'end',
            serial: serial
        }));

        console.log(`end 명령 전송 → 라즈베리파이 (Serial: ${serial}, Email: ${email})`);
    }
}