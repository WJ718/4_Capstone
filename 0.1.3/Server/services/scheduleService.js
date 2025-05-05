const Schedule = require('../models/schedule');

exports.upload = async (email, date, memo) => {
    await Schedule.create({
        email, date, memo
    });

    console.log(`일정 업로드 완료 - email: ${email} memo: ${memo}`);
}

exports.remove = async (email, date) => {
    const content = await Schedule.findOne({where: {email, date}});

    if(!content) {
        console.log("remove 에러 - 존재하지 않는 일정");
        const error = new Error("존재하지 않는 일정입니다.");
        error.statusCode = 400;
        throw error;
    }

    await Schedule.destroy({where: {email, date}});
    console.log("일정 삭제 완료");
}

exports.update = async (email, date, memo) => {
    const content = await Schedule.findOne({where: {email, date}});
    
    if(!content) {
        console.log('update 에러 - 존재하지 않는 일정');
        const error = new Error("존재하지 않는 일정입니다.");
        error.statusCode = 400;
        throw error;
    }

    await Schedule.update({ memo }, { where: { email, date } }); 
    console.log(`일정 업데이트 완료: ${email} >> new: ${memo}`);
}