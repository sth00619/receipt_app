const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
require('dotenv').config();

const app = express();

// Middleware
app.use(cors());
app.use(express.json()); // JSON 요청 본문 파싱
app.use(express.urlencoded({ extended: true })); // URL-encoded 요청 본문 파싱

// Request 로깅
app.use((req, res, next) => {
  console.log(`${new Date().toISOString()} - ${req.method} ${req.path}`);
  next();
});

// MongoDB 연결
mongoose.connect(process.env.MONGODB_URI, {
  useNewUrlParser: true,
  useUnifiedTopology: true
})
.then(() => {
  console.log('✅ MongoDB 연결 성공');
  console.log(`   Database: ${mongoose.connection.name}`);
})
.catch(err => {
  console.error('❌ MongoDB 연결 실패:', err.message);
  process.exit(1);
});

// Firebase Admin 초기화 테스트
// (인증 라우트에서 Firebase Auth 토큰 검증 또는 커스텀 토큰 생성을 위해 필요)
try {
  require('./src/config/firebase-admin');
  console.log('✅ Firebase Admin SDK 초기화 성공');
} catch (error) {
  console.error('❌ Firebase Admin SDK 초기화 실패:', error.message);
  console.error('   서비스 계정 키 파일을 확인하세요.');
}

// Health Check
app.get('/api/health', (req, res) => {
  res.json({
    status: 'OK',
    message: 'Receiptify API Server is running',
    mongodb: mongoose.connection.readyState === 1 ? 'Connected' : 'Disconnected',
    firebase: 'Configured',
    timestamp: new Date().toISOString()
  });
});

// Routes
// auth 라우트를 추가합니다.
const authRoutes = require('./src/routes/auth');
const userRoutes = require('./src/routes/users');
const receiptRoutes = require('./src/routes/receipts');
const transactionRoutes = require('./src/routes/transactions');

app.use('/api/auth', authRoutes); // **인증(Auth) 라우트 추가**
app.use('/api/users', userRoutes);
app.use('/api/receipts', receiptRoutes);
app.use('/api/transactions', transactionRoutes);

// 404 Handler
app.use((req, res) => {
  res.status(404).json({
    success: false,
    message: 'Route not found'
  });
});

// Error Handler
app.use((err, req, res, next) => {
  console.error('Error:', err);
  res.status(500).json({
    success: false,
    message: 'Internal server error',
    error: process.env.NODE_ENV === 'development' ? err.message : undefined
  });
});

// 서버 시작
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log('='.repeat(50));
  console.log(`🚀 Receiptify API Server`);
  console.log(`   Port: ${PORT}`);
  console.log(`   Environment: ${process.env.NODE_ENV}`);
  console.log(`   Health Check: http://localhost:${PORT}/api/health`);
  console.log('='.repeat(50));
});

module.exports = app;