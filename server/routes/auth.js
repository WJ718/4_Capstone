// routes/auth.js
const express = require('express');
const router = express.Router();

const {signup, login} = require('../controllers/auth');

//POST 요청
router.post('/signup', signup);
router.post('/login', login);

module.exports = router;