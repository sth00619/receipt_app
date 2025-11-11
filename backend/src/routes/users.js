// backend/src/routes/users.js
const express = require('express');
const router = express.Router();
const User = require('../models/User');
const { verifyFirebaseToken } = require('../middleware/auth');

/**
 * POST /api/users/sync
 * Firebase 로그인 후 MongoDB에 사용자 생성/업데이트
 *
 * 이 API를 호출하면:
 * 1. Firebase 토큰 검증
 * 2. MongoDB에 사용자 존재 여부 확인
 * 3. 없으면 생성, 있으면 업데이트
 */
router.post('/sync', verifyFirebaseToken, async (req, res) => {
  try {
    const { uid, email, name, picture } = req.user;

    // MongoDB에서 사용자 찾기
    let user = await User.findOne({ uid });

    if (!user) {
      // 신규 사용자 생성
      user = new User({
        uid,
        email,
        displayName: name,
        photoUrl: picture,
        provider: 'google', // 또는 req.body.provider
        preferences: {
          notifications: true,
          darkMode: false,
          language: 'ko'
        },
        stats: {
          totalReceipts: 0,
          totalTransactions: 0,
          totalSpending: 0
        },
        lastLoginAt: new Date()
      });

      await user.save();

      console.log(`✨ 신규 사용자 생성: ${email}`);

      return res.status(201).json({
        success: true,
        message: 'User created successfully',
        isNewUser: true,
        data: user
      });
    } else {
      // 기존 사용자 업데이트
      user.displayName = name || user.displayName;
      user.photoUrl = picture || user.photoUrl;
      user.lastLoginAt = new Date();

      await user.save();

      console.log(`🔄 기존 사용자 업데이트: ${email}`);

      return res.json({
        success: true,
        message: 'User synced successfully',
        isNewUser: false,
        data: user
      });
    }
  } catch (error) {
    console.error('사용자 동기화 오류:', error);
    res.status(500).json({
      success: false,
      message: 'Error syncing user',
      error: error.message
    });
  }
});

/**
 * GET /api/users/me
 * 현재 로그인한 사용자 정보 조회
 */
router.get('/me', verifyFirebaseToken, async (req, res) => {
  try {
    const user = await User.findOne({ uid: req.user.uid });

    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found. Please sync first.'
      });
    }

    res.json({
      success: true,
      data: user
    });
  } catch (error) {
    console.error('사용자 조회 오류:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching user',
      error: error.message
    });
  }
});

/**
 * PUT /api/users/preferences
 * 사용자 설정 업데이트
 */
router.put('/preferences', verifyFirebaseToken, async (req, res) => {
  try {
    const { notifications, darkMode, language } = req.body;

    const user = await User.findOneAndUpdate(
      { uid: req.user.uid },
      {
        $set: {
          'preferences.notifications': notifications,
          'preferences.darkMode': darkMode,
          'preferences.language': language,
          updatedAt: new Date()
        }
      },
      { new: true }
    );

    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }

    res.json({
      success: true,
      message: 'Preferences updated',
      data: user
    });
  } catch (error) {
    console.error('설정 업데이트 오류:', error);
    res.status(500).json({
      success: false,
      message: 'Error updating preferences',
      error: error.message
    });
  }
});

module.exports = router;