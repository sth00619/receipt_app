// backend/src/routes/receipts.js
const express = require('express');
const router = express.Router();
const Receipt = require('../models/Receipt');
const Transaction = require('../models/Transaction');

// 모든 영수증 조회
router.get('/', async (req, res) => {
  try {
    const { userId, category, startDate, endDate, limit = 50 } = req.query;

    if (!userId) {
      return res.status(400).json({
        success: false,
        message: 'userId is required'
      });
    }

    const query = { userId };
    if (category) query.category = category;
    if (startDate || endDate) {
      query.transactionDate = {};
      if (startDate) query.transactionDate.$gte = new Date(startDate);
      if (endDate) query.transactionDate.$lte = new Date(endDate);
    }

    const receipts = await Receipt.find(query)
      .sort({ transactionDate: -1 })
      .limit(parseInt(limit));

    res.json({
      success: true,
      count: receipts.length,
      data: receipts
    });
  } catch (error) {
    console.error('Error fetching receipts:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching receipts',
      error: error.message
    });
  }
});

// 영수증 생성
router.post('/', async (req, res) => {
  try {
    const receipt = new Receipt(req.body);
    await receipt.save();

    // Transaction도 함께 생성
    const transaction = new Transaction({
      userId: receipt.userId,
      receiptId: receipt._id,
      storeName: receipt.storeName,
      category: receipt.category,
      amount: receipt.totalAmount,
      date: receipt.transactionDate
    });
    await transaction.save();

    res.status(201).json({
      success: true,
      data: receipt
    });
  } catch (error) {
    console.error('Error creating receipt:', error);
    res.status(400).json({
      success: false,
      message: 'Error creating receipt',
      error: error.message
    });
  }
});

// 특정 영수증 조회
router.get('/:id', async (req, res) => {
  try {
    const receipt = await Receipt.findById(req.params.id);

    if (!receipt) {
      return res.status(404).json({
        success: false,
        message: 'Receipt not found'
      });
    }

    res.json({
      success: true,
      data: receipt
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Error fetching receipt',
      error: error.message
    });
  }
});

// 영수증 수정
router.put('/:id', async (req, res) => {
  try {
    const receipt = await Receipt.findByIdAndUpdate(
      req.params.id,
      req.body,
      { new: true, runValidators: true }
    );

    if (!receipt) {
      return res.status(404).json({
        success: false,
        message: 'Receipt not found'
      });
    }

    res.json({
      success: true,
      data: receipt
    });
  } catch (error) {
    res.status(400).json({
      success: false,
      message: 'Error updating receipt',
      error: error.message
    });
  }
});

// 영수증 삭제
router.delete('/:id', async (req, res) => {
  try {
    const receipt = await Receipt.findByIdAndDelete(req.params.id);

    if (!receipt) {
      return res.status(404).json({
        success: false,
        message: 'Receipt not found'
      });
    }

    // 관련 Transaction도 삭제
    await Transaction.deleteOne({ receiptId: receipt._id });

    res.json({
      success: true,
      message: 'Receipt deleted successfully'
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Error deleting receipt',
      error: error.message
    });
  }
});

// 통계 조회
router.get('/stats/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    const { month, year } = req.query;

    const currentYear = year ? parseInt(year) : new Date().getFullYear();
    const currentMonth = month ? parseInt(month) : new Date().getMonth() + 1;

    const startDate = new Date(currentYear, currentMonth - 1, 1);
    const endDate = new Date(currentYear, currentMonth, 0, 23, 59, 59);

    // 카테고리별 통계
    const stats = await Receipt.aggregate([
      {
        $match: {
          userId: userId,
          transactionDate: { $gte: startDate, $lte: endDate }
        }
      },
      {
        $group: {
          _id: '$category',
          totalAmount: { $sum: '$totalAmount' },
          count: { $sum: 1 }
        }
      }
    ]);

    // 전체 통계
    const total = await Receipt.aggregate([
      {
        $match: {
          userId: userId,
          transactionDate: { $gte: startDate, $lte: endDate }
        }
      },
      {
        $group: {
          _id: null,
          totalAmount: { $sum: '$totalAmount' },
          count: { $sum: 1 }
        }
      }
    ]);

    // 일별 통계
    const dailyStats = await Receipt.aggregate([
      {
        $match: {
          userId: userId,
          transactionDate: { $gte: startDate, $lte: endDate }
        }
      },
      {
        $group: {
          _id: { $dayOfMonth: '$transactionDate' },
          amount: { $sum: '$totalAmount' }
        }
      },
      {
        $sort: { _id: 1 }
      }
    ]);

    res.json({
      success: true,
      data: {
        byCategory: stats,
        total: total[0] || { totalAmount: 0, count: 0 },
        dailyStats: dailyStats.map(d => ({ day: d._id, amount: d.amount }))
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Error fetching statistics',
      error: error.message
    });
  }
});

module.exports = router;