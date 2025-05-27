// routes/index.js
const express = require('express');

const { renderMain } = require('../controllers');

const router = express.Router();

router.use((req, res, next) => {
  res.locals.user = req.user;
  next();
});

router.get('/', renderMain);

module.exports = router;