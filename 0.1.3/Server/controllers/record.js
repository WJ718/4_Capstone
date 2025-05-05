const recordService = require('../services/recordService');

exports.start = async (req, res) => {
    try {
        const { email, serial } = req.query;
        await recordService.start(email, serial);

        return res.status(200).json({ success: true, message: 'Start command sent' });
    } catch (err) {
        console.error('[Start Error]', err.message);
        return res.status(err.statusCode || 500).json({ 
            success: false, 
            message: err.message || '서버 오류' });
    }
};

exports.end = async (req, res) => {
    try {
        const { email, serial } = req.query;
        await recordService.end(email, serial);

        return res.status(200).json({ success: true, message: 'End command sent' });
    } catch (err) {
        console.error('[End Error]', err.message);
        return res.status(err.statusCode || 500).json({ 
            success: false, 
            message: err.message || '서버 오류' });
    }
};
