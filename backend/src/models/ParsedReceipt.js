// backend/src/models/ParsedReceipt.js
const mongoose = require('mongoose');

// 파싱된 개별 상품 정보
const parsedItemSchema = new mongoose.Schema({
  name: {
    type: String,
    required: true
  },
  quantity: {
    type: Number,
    default: 1,
    min: 0
  },
  unitPrice: {
    type: Number,
    min: 0
  },
  totalPrice: {
    type: Number,
    required: true,
    min: 0
  },
  // 사용자가 수정했는지 여부
  isManuallyEdited: {
    type: Boolean,
    default: false
  }
}, { _id: false });

const parsedReceiptSchema = new mongoose.Schema({
  receiptId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Receipt',
    required: true,
    unique: true
  },
  userId: {
    type: String,
    required: true,
    index: true
  },

  // 원본 OCR 텍스트
  rawOcrText: {
    type: String,
    required: true
  },

  // 파싱된 정보
  storeName: String,
  storeAddress: String,
  storePhone: String,
  businessNumber: String,  // 사업자등록번호

  // 날짜/시간
  transactionDate: Date,
  transactionTime: String,

  // 금액 정보
  subtotal: Number,        // 소계
  taxAmount: Number,       // 세금
  discountAmount: Number,  // 할인
  totalAmount: {           // 총액
    type: Number,
    required: true
  },

  // 결제 정보
  paymentMethod: {
    type: String,
    enum: ['card', 'cash', 'transfer', 'other']
  },
  cardNumber: String,      // 카드번호 (마스킹)
  approvalNumber: String,  // 승인번호

  // 상품 목록
  items: [parsedItemSchema],

  // 카테고리 자동 분류
  suggestedCategory: {
    type: String,
    enum: ['food', 'transport', 'shopping', 'healthcare', 'entertainment', 'utilities', 'others']
  },

  // 파싱 신뢰도
  confidence: {
    storeName: { type: Number, min: 0, max: 1 },
    totalAmount: { type: Number, min: 0, max: 1 },
    items: { type: Number, min: 0, max: 1 }
  },

  // 파싱 상태
  parsingStatus: {
    type: String,
    enum: ['success', 'partial', 'failed'],
    default: 'success'
  },

  // 사용자 수정 여부
  isManuallyVerified: {
    type: Boolean,
    default: false
  }

}, {
  timestamps: true
});

// 복합 인덱스
parsedReceiptSchema.index({ userId: 1, createdAt: -1 });

module.exports = mongoose.model('ParsedReceipt', parsedReceiptSchema);