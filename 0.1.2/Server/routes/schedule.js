const express = require('express')
const router = express.Router();
const {upload, update, remove} = require('../controllers/schedule');

/* 
앱에서 캘린더의 날짜 터치 --> 메모 작성 --> 등록 버튼 클릭 시 시간(ms단위)와 함께 memo 데이터가 라우터로 전송
*/

router.post('/upload', upload);
router.post('/update', update);
router.post('/remove', remove);

module.exports = router;