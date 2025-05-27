const scheduleService = require('../services/scheduleService');

exports.upload = async (req,res,next) => {
    try {
        const {email, date, memo} = req.body;

        await scheduleService.upload(email, date, memo);

        return res.status(201).json({
            success: true,
            message: `일정 업로드 성공`});
    } catch(err) {
        console.error(err);
        return res.status(err.statusCode || 500).json({ 
            success: false, 
            message: err.message || '서버 오류' });
    }
};

exports.remove = async (req,res,next) => {
    const {email, date} = req.body;

    try {
        await scheduleService.remove(email, date);

        return res.json({
            success: true,
            message: '일정 삭제 완료'});
    } catch(error) {
        console.error(error);
        return res.status(err.statusCode || 500).json({ 
            success: false, 
            message: error.message || '서버 오류' });
    }
}

exports.update = async (req,res,next) => {
    const {email, date, memo} = req.body;
    try {
        await scheduleService.update(email, date, memo);
        return res.json({
            success: true,
            message: '일정 변경 완료'});
    } catch(error) {
        return res.status(error.statusCode || 500).json({ 
            success: false, 
            message: error.message || '서버 오류' });
    }
}