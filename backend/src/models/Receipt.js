// backend/src/models/Receipt.js
const mongoose = require('mongoose');

const receiptItemSchema = new mongoose.Schema({
  name: {
    type: String,
    required: true
  },
  quantity: {
    type: Number,
    default: 1
  },
  unitPrice: {
    type: Number
  },
  amount: {
    type: Number,
    required: true
  }
}, { _id: false });

const receiptSchema = new mongoose.Schema({
  userId: {
    type: String,
    required: true,
    index: true
  },
  storeName: {
    type: String,
    required: true
  },
  storeAddress: String,
  storePhone: String,

  totalAmount: {
    type: Number,
    required: true,
    min: 0
  },
  taxAmount: Number,
  discountAmount: Number,

  transactionDate: {
    type: Date,
    required: true,
    default: Date.now
  },
  paymentMethod: {
    type: String,
    enum: ['card', 'cash', 'transfer'],
    default: 'card'
  },

  category: {
    type: String,
    enum: ['food', 'transport', 'shopping', 'healthcare', 'entertainment', 'utilities', 'others'],
    required: true,
    index: true
  },
  subcategory: String,

  items: [receiptItemSchema],

  ocrText: String,
  imageUrl: String,
  imagePath: String,

  tags: [String],
  notes: String,

  isVerified: {
    type: Boolean,
    default: false
  }
}, {
  timestamps: true
});

// 복합 인덱스
receiptSchema.index({ userId: 1, transactionDate: -1 });
receiptSchema.index({ userId: 1, category: 1 });

module.exports = mongoose.model('Receipt', receiptSchema);