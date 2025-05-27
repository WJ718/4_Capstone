const recordService = require('../services/recordService');

exports.start = async (req, res) => {
    try {
        const { email, serial } = req.query;
        await recordService.start(email, serial);

        return res.status(200).json({ success: true, message: 'Start command sent' });
    } catch (error) {
        console.error('[Start Error]', error.message);
        return res.status(error.statusCode || 500).json({ 
            success: false, 
            message: error.message || '서버 오류' });
    }
};

exports.end = async (req, res) => {
    try {
        const { email, serial } = req.query;
        await recordService.end(email, serial);

        return res.status(200).json({ success: true, message: 'End command sent' });
    } catch (error) {
        console.error('[End Error]', error.message);
        return res.status(err.statusCode || 500).json({ 
            success: false, 
            message: error.message || '서버 오류' });
    }
};
