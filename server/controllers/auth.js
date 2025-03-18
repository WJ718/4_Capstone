const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken'); // JWT 추가
const User = require('../models/user');

exports.signup = async (req, res, next) => {

  const { email, password } = req.body;
  try {
    // 기존 사용자 확인
    const exUser = await User.findOne({ where: { email } });
    if (exUser) {
      return res.status(409).json({ message: '이미 존재하는 아이디입니다.' });
    }

    // 비밀번호 해싱
    const hash = await bcrypt.hash(password, 10);

    // 새로운 사용자 생성
    const newUser = await User.create({
      email,
      password: hash,
    });

    console.log('회원가입 성공:', email, hash);

    // JWT 토큰 생성 (secret key는 .env 파일에 저장)
    const token = jwt.sign(
      { email: newUser.email }, 
      process.env.JWT_SECRET, 
      { expiresIn: '1h' } // 토큰 유효 기간: 1시간
    );

    // JSON 응답
    return res.status(201).json({
      message: '회원가입 성공',
      token,  // JWT 토큰 반환
    });
  } catch (error) {
    console.error(error);
    return res.status(500).json({ message: '서버 오류', error: error.message });
  }
};


exports.login = async (req, res, next) => {

};
