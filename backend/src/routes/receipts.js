// backend/src/routes/receipts.js
const express = require('express');
const router = express.Router();
const Receipt = require('../models/Receipt');
const SpendingAnalyzer = require('../services/SpendingAnalyzer');

// 내 통계 조회
router.get('/stats', async (req, res) => {
  try {
    const { month, year, startDate, endDate } = req.query;

    let matchQuery = {
      userId: req.user.userId
    };

    // 날짜 필터 적용
    if (startDate || endDate) {
      matchQuery.transactionDate = {};
      if (startDate) {
        matchQuery.transactionDate.$gte = new Date(startDate);
        console.log(`📅 시작 날짜: ${startDate}`);
      }
      if (endDate) {
        matchQuery.transactionDate.$lte = new Date(endDate);
        console.log(`📅 종료 날짜: ${endDate}`);
      }
    } else if (year || month) {
      const currentYear = year ? parseInt(year) : new Date().getFullYear();
      const currentMonth = month ? parseInt(month) : new Date().getMonth() + 1;

      const startDateCalc = new Date(currentYear, currentMonth - 1, 1);
      const endDateCalc = new Date(currentYear, currentMonth, 0, 23, 59, 59);

      matchQuery.transactionDate = { $gte: startDateCalc, $lte: endDateCalc };

      console.log(`📅 연/월 필터: ${currentYear}년 ${currentMonth}월`);
    }

    console.log(`🔍 Match Query:`, JSON.stringify(matchQuery, null, 2));

    // 카테고리별 통계
    const stats = await Receipt.aggregate([
      {
        $match: matchQuery
      },
      {
        $group: {
          _id: '$category',
          totalAmount: { $sum: '$totalAmount' },
          count: { $sum: 1 }
        }
      }
    ]);

    // ✅ null 카테고리를 'others'로 변환
    const statsByCategory = stats.map(s => ({
      category: s._id || 'others',  // null이면 'others'
      totalAmount: s.totalAmount,
      count: s.count
    }));

    console.log(`📊 카테고리별 통계 (${statsByCategory.length}개):`,
      JSON.stringify(statsByCategory, null, 2));

    // 전체 통계
    const total = await Receipt.aggregate([
      {
        $match: matchQuery
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
        $match: matchQuery
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

    // ✅ 날짜 필터가 없을 때만 현재 월, 지난 달, 오늘 통계 계산
    let currentMonthTotal = 0;
    let lastMonthTotal = 0;
    let todayTotal = 0;
    let monthlyChangePercent = 0;

    if (!startDate && !endDate && !year && !month) {
      const now = new Date();
      const currentYear = now.getFullYear();
      const currentMonth = now.getMonth();

      // 현재 월 시작/종료
      const currentMonthStart = new Date(currentYear, currentMonth, 1);
      const currentMonthEnd = new Date(currentYear, currentMonth + 1, 0, 23, 59, 59);

      // 지난 달 시작/종료
      const lastMonthStart = new Date(currentYear, currentMonth - 1, 1);
      const lastMonthEnd = new Date(currentYear, currentMonth, 0, 23, 59, 59);

      // 오늘 시작/종료
      const todayStart = new Date(currentYear, currentMonth, now.getDate(), 0, 0, 0);
      const todayEnd = new Date(currentYear, currentMonth, now.getDate(), 23, 59, 59);

      console.log(`📅 현재 월: ${currentMonthStart.toISOString()} ~ ${currentMonthEnd.toISOString()}`);
      console.log(`📅 지난 달: ${lastMonthStart.toISOString()} ~ ${lastMonthEnd.toISOString()}`);
      console.log(`📅 오늘: ${todayStart.toISOString()} ~ ${todayEnd.toISOString()}`);

      // 현재 월 통계
      const currentMonthStats = await Receipt.aggregate([
        {
          $match: {
            userId: req.user.userId,
            transactionDate: { $gte: currentMonthStart, $lte: currentMonthEnd }
          }
        },
        {
          $group: {
            _id: null,
            totalAmount: { $sum: '$totalAmount' }
          }
        }
      ]);
      currentMonthTotal = currentMonthStats[0]?.totalAmount || 0;

      // 지난 달 통계
      const lastMonthStats = await Receipt.aggregate([
        {
          $match: {
            userId: req.user.userId,
            transactionDate: { $gte: lastMonthStart, $lte: lastMonthEnd }
          }
        },
        {
          $group: {
            _id: null,
            totalAmount: { $sum: '$totalAmount' }
          }
        }
      ]);
      lastMonthTotal = lastMonthStats[0]?.totalAmount || 0;

      // 오늘 통계
      const todayStats = await Receipt.aggregate([
        {
          $match: {
            userId: req.user.userId,
            transactionDate: { $gte: todayStart, $lte: todayEnd }
          }
        },
        {
          $group: {
            _id: null,
            totalAmount: { $sum: '$totalAmount' }
          }
        }
      ]);
      todayTotal = todayStats[0]?.totalAmount || 0;

      // 월별 변화율 계산
      if (lastMonthTotal > 0) {
        monthlyChangePercent = Math.round(((currentMonthTotal - lastMonthTotal) / lastMonthTotal) * 100);
      } else if (currentMonthTotal > 0) {
        monthlyChangePercent = 100; // 지난 달 0원, 이번 달 지출 있으면 100% 증가
      }

      console.log(`💰 현재 월 총액: ${currentMonthTotal}`);
      console.log(`💰 지난 달 총액: ${lastMonthTotal}`);
      console.log(`💰 오늘 총액: ${todayTotal}`);
      console.log(`📈 월별 변화율: ${monthlyChangePercent}%`);
    }

    console.log(`✅ 통계 조회 완료: 총액 ${total[0]?.totalAmount || 0}, 개수 ${total[0]?.count || 0}`);

    res.json({
      success: true,
      data: {
        byCategory: statsByCategory,
        total: total[0] || { totalAmount: 0, count: 0 },
        dailyStats: dailyStats.map(d => ({ day: d._id, amount: d.amount })),
        // ✅ 추가 통계 (날짜 필터 없을 때만)
        currentMonthTotal,
        lastMonthTotal,
        todayTotal,
        monthlyChangePercent
      }
    });
  } catch (error) {
    console.error('❌ 통계 조회 오류:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching statistics',
      error: error.message
    });
  }
});

// 내 영수증 목록 조회
router.get('/', async (req, res) => {
  try {
    const { category, startDate, endDate, limit = 50 } = req.query;

    let query = {
      userId: req.user.userId
    };

    // 카테고리 필터
    if (category) {
      query.category = category;
    }

    // 날짜 필터
    if (startDate || endDate) {
      query.transactionDate = {};
      if (startDate) {
        query.transactionDate.$gte = new Date(startDate);
      }
      if (endDate) {
        query.transactionDate.$lte = new Date(endDate);
      }
    }

    console.log('📋 영수증 조회 쿼리:', JSON.stringify(query, null, 2));

    const receipts = await Receipt.find(query)
      .sort({ transactionDate: -1 })
      .limit(parseInt(limit));

    console.log(`✅ ${receipts.length}개 영수증 조회 완료`);

    res.json({
      success: true,
      data: receipts
    });
  } catch (error) {
    console.error('❌ 영수증 조회 오류:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching receipts',
      error: error.message
    });
  }
});

/**
 * POST /api/receipts
 * 영수증 생성
 */
router.post('/', async (req, res) => {
  try {
    const userId = req.user.userId;
    const receiptData = {
      ...req.body,
      userId: userId
    };

    console.log('📝 영수증 생성 요청:', receiptData);

    const receipt = new Receipt(receiptData);
    await receipt.save();

    console.log('✅ 영수증 저장 완료:', receipt._id);

    // ✅✅✅ 실시간 소비 분석 및 알림 생성
    try {
      const analysis = await SpendingAnalyzer.analyzeRealtimeSpending(userId, receipt);

      if (analysis.alerts.length > 0) {
        console.log(`🔔 ${analysis.alerts.length}개 실시간 알림 생성됨`);
      }
    } catch (analysisError) {
      console.error('⚠️ 실시간 분석 실패 (영수증은 저장됨):', analysisError);
      // 분석 실패해도 영수증 저장은 성공
    }

    res.status(201).json({
      success: true,
      message: '영수증이 생성되었습니다',
      data: receipt
    });

  } catch (error) {
    console.error('❌ 영수증 생성 실패:', error);
    res.status(500).json({
      success: false,
      message: '영수증 생성에 실패했습니다',
      error: error.message
    });
  }
})

// 특정 영수증 조회
router.get('/:id', async (req, res) => {
  try {
    const receipt = await Receipt.findOne({
      _id: req.params.id,
      userId: req.user.userId
    });

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
    console.error('❌ 영수증 조회 오류:', error);
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
    const receipt = await Receipt.findOneAndUpdate(
      {
        _id: req.params.id,
        userId: req.user.userId
      },
      req.body,
      { new: true, runValidators: true }
    );

    if (!receipt) {
      return res.status(404).json({
        success: false,
        message: 'Receipt not found'
      });
    }

    console.log(`✅ 영수증 수정 완료: ${receipt._id}`);

    res.json({
      success: true,
      data: receipt,
      message: 'Receipt updated successfully'
    });
  } catch (error) {
    console.error('❌ 영수증 수정 오류:', error);
    res.status(400).json({
      success: false,
      message: 'Error updating receipt',
      error: error.message
    });
  }
});

/**
 * POST /api/receipts
 * 영수증 생성
 */
router.post('/', async (req, res) => {
  try {
    const userId = req.user.userId;
    const receiptData = {
      ...req.body,
      userId: userId
    };

    console.log('📝 영수증 생성 요청:', receiptData);

    const receipt = new Receipt(receiptData);
    await receipt.save();

    console.log('✅ 영수증 저장 완료:', receipt._id);

    // ✅✅✅ 실시간 소비 분석 및 알림 생성
    try {
      const analysis = await SpendingAnalyzer.analyzeRealtimeSpending(userId, receipt);

      if (analysis.alerts.length > 0) {
        console.log(`🔔 ${analysis.alerts.length}개 실시간 알림 생성됨`);
      }
    } catch (analysisError) {
      console.error('⚠️ 실시간 분석 실패 (영수증은 저장됨):', analysisError);
      // 분석 실패해도 영수증 저장은 성공
    }

    res.status(201).json({
      success: true,
      message: '영수증이 생성되었습니다',
      data: receipt
    });

  } catch (error) {
    console.error('❌ 영수증 생성 실패:', error);
    res.status(500).json({
      success: false,
      message: '영수증 생성에 실패했습니다',
      error: error.message
    });
  }
});

// 영수증 삭제
router.delete('/:id', async (req, res) => {
  try {
    const receipt = await Receipt.findOneAndDelete({
      _id: req.params.id,
      userId: req.user.userId
    });

    if (!receipt) {
      return res.status(404).json({
        success: false,
        message: 'Receipt not found'
      });
    }

    console.log(`✅ 영수증 삭제 완료: ${receipt._id}`);

    res.json({
      success: true,
      message: 'Receipt deleted successfully'
    });
  } catch (error) {
    console.error('❌ 영수증 삭제 오류:', error);
    res.status(500).json({
      success: false,
      message: 'Error deleting receipt',
      error: error.message
    });
  }
});

module.exports = router;