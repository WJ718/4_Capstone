// routes/record.js
const express = require('express');

const {start} = require('../controllers/record');

const router = express.Router();

/* 
구상 : 
1. 애플리케이션에서 "학습 시작" 눌렀을 때 서버로 요청. 서버는 해당 시점에 라즈베리에게 신호 보냄.
2. 라즈베리는 해당 신호를 받은 시점부터 사용자의 졸음을 인식.
3. 라즈베리파이는 졸음을 인식하면 다시 서버로 isSleep 신호를 보내고, 해당 신호를 받은 서버는 애플리케이션에 신호를 보냄.
4. 애플리케이션은 신호를 받으면 토스트 메시지와 함께 경고.
*/
router.get('/start', start);

module.exports = router;