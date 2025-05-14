// controllers/auth.js
const authService = require('../services/authService');

exports.signup = async (req, res, next) => {
  const { email, password } = req.body;
  try {
    console.log('회원가입 성공:', email, hash);

    // JWT 토큰 생성
    const token = await authService.signup(email,password);

    // JSON 응답 
    return res.status(201).json({
      success: true,
      message: `회원가입 성공`,
      token,  // JWT 토큰 반환
    });
  } catch (error) {
    console.error(error);
    return res.status(err.statusCode || 500).json({ 
      success: false, 
      message: err.message || '서버 오류' });
  }
};

exports.login = async (req, res, next) => {
  try {
    const {email, password} = req.body;

    const token = await authService.login(email, password);

    return res.status(201).json({
      success: true,
      message: `로그인 성공`,
      token,  // JWT 토큰 반환
    });
  } catch(err) {
    console.error(err);
    return res.status(err.statusCode || 500).json({ 
      success: false, 
      message: err.message || '서버 오류' });
  }
};
