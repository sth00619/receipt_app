const express = require('express');
const router = express.Router();
const Notification = require('../models/Notification');
const Receipt = require('../models/Receipt');
const { verifyAuth } = require('../middleware/auth');

router.use(verifyAuth);

// 내 알림 목록 조회
router.get('/', async (req, res) => {
  try {
    const { limit = 50, unreadOnly = false } = req.query;

    const query = { userId: req.user.userId };
    if (unreadOnly === 'true') {
      query.isRead = false;
    }

    const notifications = await Notification.find(query)
      .sort({ createdAt: -1 })
      .limit(parseInt(limit));

    // 읽지 않은 알림 개수
    const unreadCount = await Notification.countDocuments({
      userId: req.user.userId,
      isRead: false
    });

    res.json({
      success: true,
      data: {
        notifications,
        unreadCount
      }
    });

  } catch (error) {
    console.error('❌ 알림 조회 오류:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching notifications',
      error: error.message
    });
  }
});

// 알림 읽음 처리
router.put('/:id/read', async (req, res) => {
  try {
    const notification = await Notification.findOneAndUpdate(
      {
        _id: req.params.id,
        userId: req.user.userId
      },
      { isRead: true },
      { new: true }
    );

    if (!notification) {
      return res.status(404).json({
        success: false,
        message: 'Notification not found'
      });
    }

    res.json({
      success: true,
      data: notification
    });

  } catch (error) {
    console.error('❌ 알림 읽음 처리 오류:', error);
    res.status(500).json({
      success: false,
      message: 'Error updating notification',
      error: error.message
    });
  }
});

// 모든 알림 읽음 처리
router.put('/read-all', async (req, res) => {
  try {
    await Notification.updateMany(
      {
        userId: req.user.userId,
        isRead: false
      },
      { isRead: true }
    );

    res.json({
      success: true,
      message: 'All notifications marked as read'
    });

  } catch (error) {
    console.error('❌ 전체 읽음 처리 오류:', error);
    res.status(500).json({
      success: false,
      message: 'Error updating notifications',
      error: error.message
    });
  }
});

// 알림 삭제
router.delete('/:id', async (req, res) => {
  try {
    const notification = await Notification.findOneAndDelete({
      _id: req.params.id,
      userId: req.user.userId
    });

    if (!notification) {
      return res.status(404).json({
        success: false,
        message: 'Notification not found'
      });
    }

    res.json({
      success: true,
      message: 'Notification deleted'
    });

  } catch (error) {
    console.error('❌ 알림 삭제 오류:', error);
    res.status(500).json({
      success: false,
      message: 'Error deleting notification',
      error: error.message
    });
  }
});

module.exports = router;