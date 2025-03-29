// controllers/record.js
const Record = require('../models/record');

const {connectedClients} = require('../ws');


// 예시 ) 앱 --> 서버 start 요청 시 : GET /record/start?serial=00000000abcdef12
/* [앱] → GET /record/start?serial=0000ABCDEF → [서버] → WebSocket → [라즈베리파이] */

exports.start = (req, res) => {
    const { email, serial } = req.query;

    if (!email || !serial) {
        return res.status(400).json({ error: '이메일과 시리얼 넘버가 필요합니다.' });
    }

    const raspberrySocket = connectedClients.raspberry.get(serial);

    if (raspberrySocket && raspberrySocket.readyState === 1) {
        // email ↔ serial 매핑 저장
        connectedClients.deviceToUser.set(serial, email);

        // start 명령 전송
        raspberrySocket.send(JSON.stringify({
            command: 'start',
            serial: serial
        }));

        console.log(`start 명령 전송 → 라즈베리파이 (Serial: ${serial}, Email: ${email})`);
        return res.status(200).json({ message: 'Start command sent' });
    } else {
        return res.status(500).json({ error: '해당 라즈베리파이가 연결되어 있지 않습니다.' });
    }
};