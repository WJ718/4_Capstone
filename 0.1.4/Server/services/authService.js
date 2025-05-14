const User = require('../models/user');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');

exports.signup = async (email, password) => {
    const exUser = await User.findOne({where: {email}});

    // 해당 아이디로 회원가입 한 사용자 식별
    if(exUser) {
        const error = new Error("이미 존재하는 아이디입니다.");
        error.statusCode = 400;
        throw error;
    }

    // 처음 가입하는 회원이라면 비밀번호 해싱, DB저장
    const hash = await bcrypt.hash(password, 10);

    const newUser = await User.create({
        email,
        password: hash
    });

    console.log("회원가입 성공 : ", email);

    const token = jwt.sign(
        { email: newUser.email }, 
        process.env.JWT_SECRET, 
        { expiresIn: '5h' } // 토큰 유효 기간: 1시간
    );

    return token;
}

exports.login = async (email, password) => {
    const exUser = await User.findOne({where: {email}});
    if(!exUser) {
        const error = new Error("일치하는 사용자가 없습니다.");
        error.statusCode = 400;
        throw error;
    }

    const isMatch = await bcrypt.compare(password, exUser.password);
    if(!isMatch) {
        const error = new Error("비밀번호를 확인해주세요.");
        error.statusCode = 400;
        throw error;
    }

    const token = jwt.sign(
        {email : exUser.email},
        process.env.JWT_SECRET,
        {expiresIn: '5h'},
    );

    return token;
}