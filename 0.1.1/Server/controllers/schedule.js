const Schedule = require('../models/schedule');

exports.upload = async (req,res,next) => {
    try {
        const {email, date, memo} = req.body;

        await Schedule.create({
            email, date, memo
        });
        console.log('일정 업로드 :', email, memo);

        return res.status(201).json({message: `일정 업로드 성공`});
    } catch(err) {
        console.error(err);
        next(err);
    }
};

exports.remove = async (req,res,next) => {
    const {email, date} = req.body;

    try {
        const content = await Schedule.findOne({where: {email, date}});

        if(!content) {
            console.log('에러 : 존재하지 않는 일정.');
            return res.status(404).json({ error: '존재하지 않는 일정' });
        }

        await Schedule.destroy({where: {email, date, memo}});
        console.log('일정 삭제 완료');
        return res.json({message: '일정 삭제 완료'});
    } catch(err) {
        console.error(err);
        next(err);
    }
}

exports.update = async (req,res,next) => {
    const {email, date, memo} = req.body;
    try {
        
        const content = await Schedule.findOne({where: {email, date}});
        if(!content) {
            console.log('에러: 존재하지 않는 일정');
            return res.status(404).json({error: '존재하지 않는 일정'});
        }

        await Schedule.update ({memo}, {where: email, date});

        console.log(`일정 업데이트 완료: ${email} >> new: ${memo}`);
        return res.json({message: '일정 변경 완료'});
    } catch(err) {
        console.error(err);
        next(err);
    }
}