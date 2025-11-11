// backend/src/models/Transaction.js
const mongoose = require('mongoose');

const transactionSchema = new mongoose.Schema({
  userId: {
    type: String,
    required: true,
    index: true
  },
  receiptId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Receipt'
  },
  storeName: {
    type: String,
    required: true
  },
  category: {
    type: String,
    enum: ['food', 'transport', 'shopping', 'others'],
    required: true
  },
  amount: {
    type: Number,
    required: true,
    min: 0
  },
  date: {
    type: Date,
    required: true,
    default: Date.now
  },
  description: String,
  imageUrl: String
}, {
  timestamps: true
});

// 복합 인덱스
transactionSchema.index({ userId: 1, date: -1 });

module.exports = mongoose.model('Transaction', transactionSchema);