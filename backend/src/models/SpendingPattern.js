// backend/src/models/SpendingPattern.js
const mongoose = require('mongoose');

// 소비 패턴 분석 결과 저장
const spendingPatternSchema = new mongoose.Schema({
  userId: {
    type: String,
    required: true,
    index: true
  },

  // 분석 기간
  periodType: {
    type: String,
    enum: ['daily', 'weekly', 'monthly', 'yearly'],
    required: true
  },
  year: Number,
  month: Number,
  week: Number,

  // 카테고리별 소비
  categoryBreakdown: [{
    category: String,
    amount: Number,
    count: Number,
    percentage: Number
  }],

  // 요일별 소비 패턴
  dayOfWeekPattern: [{
    day: { type: Number, min: 0, max: 6 },  // 0: 일요일, 6: 토요일
    amount: Number,
    count: Number
  }],

  // 시간대별 소비 패턴
  timeOfDayPattern: [{
    hour: { type: Number, min: 0, max: 23 },
    amount: Number,
    count: Number
  }],

  // 자주 방문하는 장소
  frequentStores: [{
    storeName: String,
    visitCount: Number,
    totalSpent: Number
  }],

  // 통계
  stats: {
    totalAmount: Number,
    averagePerTransaction: Number,
    maxTransaction: Number,
    minTransaction: Number,
    transactionCount: Number
  },

  // 전월 대비 증감
  comparison: {
    previousPeriodAmount: Number,
    changeAmount: Number,
    changePercentage: Number
  }

}, {
  timestamps: true
});

// 복합 인덱스
spendingPatternSchema.index({
  userId: 1,
  periodType: 1,
  year: -1,
  month: -1
});

module.exports = mongoose.model('SpendingPattern', spendingPatternSchema);