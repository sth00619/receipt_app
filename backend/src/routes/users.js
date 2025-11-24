const express = require('express');
const router = express.Router();
const User = require('../models/User');
const Receipt = require('../models/Receipt');
const bcrypt = require('bcryptjs');
const { verifyAuth } = require('../middleware/auth');

// 모든 라우트에 인증 미들웨어 적용
router.use(verifyAuth);

// 내 프로필 조회
router.get('/me', async (req, res) => {
  try {
    const user = await User.findById(req.user.userId).select('-password');

    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }

    // 이번 달 지출 계산
    const now = new Date();
    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    const endOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59);

    const monthlyStats = await Receipt.aggregate([
      {
        $match: {
          userId: req.user.userId,
          transactionDate: {
            $gte: startOfMonth,
            $lte: endOfMonth
          }
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

    // 총 영수증 개수
    const totalReceipts = await Receipt.countDocuments({ userId: req.user.userId });

    const monthlySpending = monthlyStats[0]?.totalAmount || 0;
    const monthlyReceiptCount = monthlyStats[0]?.count || 0;

    res.json({
      success: true,
      data: {
        user: {
          _id: user._id,
          email: user.email,
          displayName: user.displayName,
          photoUrl: user.photoUrl,
          provider: user.provider,
          createdAt: user.createdAt
        },
        stats: {
          monthlySpending: monthlySpending,
          monthlyReceiptCount: monthlyReceiptCount,
          totalReceipts: totalReceipts
        }
      }
    });

  } catch (error) {
    console.error('❌ 프로필 조회 오류:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching profile',
      error: error.message
    });
  }
});

// 비밀번호 변경 (일반 로그인 사용자만)
router.put('/change-password', async (req, res) => {
  try {
    const { currentPassword, newPassword } = req.body;

    if (!currentPassword || !newPassword) {
      return res.status(400).json({
        success: false,
        message: 'Current password and new password are required'
      });
    }

    if (newPassword.length < 6) {
      return res.status(400).json({
        success: false,
        message: 'New password must be at least 6 characters'
      });
    }

    const user = await User.findById(req.user.userId);

    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }

    // OAuth 사용자는 비밀번호 변경 불가
    if (user.provider !== 'email') {
      return res.status(400).json({
        success: false,
        message: 'Password change is not available for OAuth users'
      });
    }

    // 현재 비밀번호 확인
    const isValidPassword = await bcrypt.compare(currentPassword, user.password);

    if (!isValidPassword) {
      return res.status(401).json({
        success: false,
        message: 'Current password is incorrect'
      });
    }

    // 새 비밀번호 해시화
    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash(newPassword, salt);

    // 비밀번호 업데이트
    user.password = hashedPassword;
    await user.save();

    console.log(`✅ 비밀번호 변경 성공: ${user.email}`);

    res.json({
      success: true,
      message: 'Password changed successfully'
    });

  } catch (error) {
    console.error('❌ 비밀번호 변경 오류:', error);
    res.status(500).json({
      success: false,
      message: 'Error changing password',
      error: error.message
    });
  }
});

// 알림 설정 업데이트
router.put('/settings/notifications', async (req, res) => {
  try {
    const { enabled } = req.body;

    const user = await User.findByIdAndUpdate(
      req.user.userId,
      {
        $set: {
          'preferences.notifications': enabled
        }
      },
      { new: true }
    ).select('-password');

    res.json({
      success: true,
      data: user
    });

  } catch (error) {
    console.error('❌ 알림 설정 업데이트 오류:', error);
    res.status(500).json({
      success: false,
      message: 'Error updating notification settings',
      error: error.message
    });
  }
});

// 다크모드 설정 업데이트
router.put('/settings/darkmode', async (req, res) => {
  try {
    const { enabled } = req.body;

    const user = await User.findByIdAndUpdate(
      req.user.userId,
      {
        $set: {
          'preferences.darkMode': enabled
        }
      },
      { new: true }
    ).select('-password');

    res.json({
      success: true,
      data: user
    });

  } catch (error) {
    console.error('❌ 다크모드 설정 업데이트 오류:', error);
    res.status(500).json({
      success: false,
      message: 'Error updating dark mode settings',
      error: error.message
    });
  }
});

module.exports = router;