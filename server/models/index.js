const Sequelize = require('sequelize');
const env = process.env.NODE_ENV || 'development';
const config = require('../config/config')[env];
const fs = require('fs');
const path = require('path');

const db = {};
const sequelize = new Sequelize(
  config.database, config.username, config.password, config,
);

db.sequelize = sequelize;

// basename == index.js
const basename = path.basename(__filename);
fs
  .readdirSync(__dirname)
  // 숨김파일 제외, index.js, .js가 아닌 파일을 제외하고 이 디렉토리의 모든 파일 조회
  .filter(file => {
    return (file.indexOf('.') !== 0) && (file !== basename) && (file.slice(-3) === '.js');
  })
  // 해당되는 js파일 모델 가져와서 init
  .forEach(file => {
    const model = require(path.join(__dirname, file));
    db[model.name] = model;
    model.init(sequelize);
  });

  // 관계설정
  Object.keys(db).forEach(modelName => {
    if (db[modelName].associate) {
      db[modelName].associate(db);
    }
  });

module.exports = db;