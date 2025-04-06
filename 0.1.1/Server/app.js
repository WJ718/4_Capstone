// app.js
const express = require('express');
const http = require('http');
const cookieParser = require('cookie-parser');
const morgan = require('morgan');
const path = require('path');
const session = require('express-session');
const nunjucks = require('nunjucks');
const dotenv = require('dotenv');
dotenv.config();

const { sequelize } = require('./models');
const { initializeWebSocket } = require('./ws');

const { connectedClients } = require('./ws');

// 임시 모의 라즈베리파이 연결 (테스트용)
connectedClients.raspberry = {
  readyState: 1,
  send: (data) => console.log('raspberry 모듈로 start 명령 전송!')
};

const app = express();
const server = http.createServer(app); // http 서버 생성

app.set('port', process.env.PORT || 4141);
app.set('view engine', 'html');
nunjucks.configure('views', { express: app, watch: true });

sequelize.sync({ force: false })
  .then(() => console.log('데이터베이스 연결 성공'))
  .catch((err) => console.error(err));

app.use(morgan('dev'));
app.use(express.static(path.join(__dirname, 'public')));
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(cookieParser(process.env.COOKIE_SECRET));
app.use(session({
  resave: false,
  saveUninitialized: false,
  secret: process.env.COOKIE_SECRET,
  cookie: { httpOnly: true, secure: false },
}));

// 라우터 설정
app.use('/', require('./routes/index'));
app.use('/auth', require('./routes/auth'));
app.use('/record', require('./routes/record'));

app.use((req, res, next) => {
  const error = new Error(`${req.method} ${req.url} 라우터가 없습니다.`);
  error.status = 404;
  next(error);
});

app.use((err, req, res, next) => {
  res.locals.message = err.message;
  res.locals.error = process.env.NODE_ENV !== 'production' ? err : {};
  res.status(err.status || 500);
  res.render('error');
});

// WebSocket 서버 초기화
initializeWebSocket(server);

server.listen(app.get('port'), () => {
  console.log(app.get('port'), '번 포트에서 서버+WebSocket 대기중');
});

process.on('SIGINT', () => {
  console.log("서버를 종료합니다.");
  server.close(() => {
    console.log("서버 연결 종료 완료");
    process.exit(); // 모든 리소스를 정리한 후 종료
  });
});