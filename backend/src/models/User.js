// backend/src/models/User.js
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

const userSchema = new mongoose.Schema({
  // Firebase uid는 선택적으로 변경
  uid: {
    type: String,
    sparse: true,  // unique but can be null
    index: true
  },
  email: {
    type: String,
    required: true,
    unique: true,
    lowercase: true,  // ✅ 추가: 소문자로 자동 변환
    trim: true,       // ✅ 추가: 공백 제거
    // ✅ 정규식 제거 또는 더 관대하게 변경
    // match: /^[^\s@]+@[^\s@]+\.[^\s@]+$/  // 제거
  },
  // 일반 로그인용 비밀번호 (해시됨)
  password: {
    type: String,
    required: function() {
      return this.provider === 'email';  // 이메일 로그인인 경우만 필수
    }
  },
  displayName: String,
  photoUrl: String,
  provider: {
    type: String,
    enum: ['email', 'firebase', 'google', 'naver'],
    default: 'email'
  },

  preferences: {
    notifications: {
      type: Boolean,
      default: true
    },
    darkMode: {
      type: Boolean,
      default: false
    },
    language: {
      type: String,
      default: 'ko'
    }
  },

  stats: {
    totalReceipts: {
      type: Number,
      default: 0
    },
    totalTransactions: {
      type: Number,
      default: 0
    },
    totalSpending: {
      type: Number,
      default: 0
    }
  },

  lastLoginAt: Date
}, {
  timestamps: true
});

// 비밀번호 해싱 미들웨어
userSchema.pre('save', async function(next) {
  if (!this.isModified('password')) return next();

  try {
    const salt = await bcrypt.genSalt(10);
    this.password = await bcrypt.hash(this.password, salt);
    next();
  } catch (error) {
    next(error);
  }
});

// 비밀번호 검증 메서드
userSchema.methods.comparePassword = async function(candidatePassword) {
  return await bcrypt.compare(candidatePassword, this.password);
};

module.exports = mongoose.model('User', userSchema);