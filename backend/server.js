// backend/server.js
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

const notificationsRouter = require('./src/routes/notifications');
const chatbotRouter = require('./src/routes/chatbot');

// 미들웨어
app.use(cors());
app.use(express.json());

// MongoDB 연결
mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/receiptify')
  .then(() => {
    console.log('✅ MongoDB connected');
  })
  .catch((err) => {
    console.error('❌ MongoDB connection error:', err);
    process.exit(1);
  });

// Health Check (인증 불필요)
app.get('/health', (req, res) => {
  res.json({
    status: 'OK',
    message: 'Receiptify Backend is running',
    timestamp: new Date().toISOString()
  });
});

// ✅ 인증 미들웨어 import (경로 수정: ./src/middleware/auth)
const { verifyAuth } = require('./src/middleware/auth');

// API 라우트
app.use('/api/auth', require('./src/routes/auth'));  // 인증 라우트는 미들웨어 없이

// ✅ 보호된 라우트 (verifyAuth 미들웨어 사용)
app.use('/api/users', verifyAuth, require('./src/routes/users'));
app.use('/api/receipts', verifyAuth, require('./src/routes/receipts'));

app.use('/api/notifications', notificationsRouter);
app.use('/api/chatbot', chatbotRouter);

// 404 핸들러
app.use((req, res) => {
  res.status(404).json({
    success: false,
    message: 'Route not found'
  });
});

// 에러 핸들러
app.use((err, req, res, next) => {
  console.error('❌ Error:', err);
  res.status(err.status || 500).json({
    success: false,
    message: err.message || 'Internal server error',
    error: process.env.NODE_ENV === 'development' ? err : {}
  });
});

// 서버 시작
app.listen(PORT, () => {
  console.log(`🚀 Server running on port ${PORT}`);
  console.log(`📍 API endpoint: http://localhost:${PORT}/api`);
  console.log(`🏥 Health check: http://localhost:${PORT}/health`);
});

module.exports = app;