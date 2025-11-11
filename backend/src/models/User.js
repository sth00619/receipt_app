// backend/src/models/User.js
const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
  uid: {
    type: String,
    required: true,
    unique: true,
    index: true
  },
  email: {
    type: String,
    required: true,
    unique: true,
    match: /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  },
  displayName: String,
  photoUrl: String,
  provider: {
    type: String,
    enum: ['firebase', 'google', 'naver'],
    default: 'firebase'
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

module.exports = mongoose.model('User', userSchema);