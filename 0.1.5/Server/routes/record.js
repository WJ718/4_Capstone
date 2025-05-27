// routes/record.js
const express = require('express');

const {start, end} = require('../controllers/record');

const router = express.Router();

/* 
구상 : 
[라즈베리파이]
   ↓ 졸음 감지 REST 요청
[서버(Node.js)]
   ↓ WebSocket 메시지 전송
[앱 Foreground Service]
   → WebSocket 수신
   → 진동 / 알림음 실행
*/
router.get('/start', start);
router.get('/end', end);

module.exports = router;