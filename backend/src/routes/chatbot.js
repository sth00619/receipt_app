const express = require('express');
const router = express.Router();
const Receipt = require('../models/Receipt');
const { verifyAuth } = require('../middleware/auth');

// 모든 라우트에 인증 미들웨어 적용
router.use(verifyAuth);

/**
 * POST /api/chatbot/message
 * 챗봇 메시지 처리
 */
router.post('/message', async (req, res) => {
  try {
    const userId = req.user.userId;
    const { message } = req.body;

    console.log(`💬 챗봇 메시지 수신 - 사용자: ${userId}`);
    console.log(`📝 메시지: ${message}`);

    if (!message) {
      return res.status(400).json({
        success: false,
        message: '메시지를 입력해주세요'
      });
    }

    // 현재 월 통계 조회
    const now = new Date();
    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    const endOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0);

    const stats = await getMonthlyStats(userId, startOfMonth, endOfMonth);

    // 챗봇 응답 생성
    const response = generateChatbotResponse(message, stats);

    console.log(`✅ 챗봇 응답: ${response}`);

    res.json({
      success: true,
      data: {
        response,
        stats
      }
    });

  } catch (error) {
    console.error('❌ 챗봇 처리 실패:', error);
    res.status(500).json({
      success: false,
      message: '챗봇 처리에 실패했습니다',
      error: error.message
    });
  }
});


/**
 * 월간 통계 조회
 */
async function getMonthlyStats(userId, startDate, endDate) {
  const stats = await Receipt.aggregate([
    {
      $match: {
        userId: userId,
        transactionDate: {
          $gte: startDate,
          $lte: endDate
        }
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

  // 전체 합계
  const total = await Receipt.aggregate([
    {
      $match: {
        userId: userId,
        transactionDate: {
          $gte: startDate,
          $lte: endDate
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

  const statsMap = {};
  stats.forEach(stat => {
    statsMap[stat._id] = {
      totalAmount: stat.totalAmount,
      count: stat.count
    };
  });

  return {
    byCategory: statsMap,
    total: total[0] || { totalAmount: 0, count: 0 }
  };
}

/**
 * 챗봇 응답 생성
 */
function generateChatbotResponse(message, stats) {
  const lowerMessage = message.toLowerCase().trim();

  // 인사
  if (lowerMessage.match(/안녕|하이|hello|hi/)) {
    return '안녕하세요! 😊 소비 도우미입니다. 이번 달 지출에 대해 궁금한 점이 있으시면 물어보세요!';
  }

  // 총 지출 문의
  if (lowerMessage.match(/총|전체|얼마|지출|다/)) {
    const total = stats.total.totalAmount;
    const advice = getSpendingAdvice(total, stats.byCategory);
    return `이번 달 총 지출은 ${total.toLocaleString()}원입니다. (영수증 ${stats.total.count}개)\n\n${advice}`;
  }

  // 식비 문의
  if (lowerMessage.match(/식비|음식|먹|밥|외식/)) {
    const food = stats.byCategory.food;
    if (food) {
      const advice = getFoodAdvice(food.totalAmount);
      return `이번 달 식비는 ${food.totalAmount.toLocaleString()}원입니다. (${food.count}회)\n\n${advice}`;
    } else {
      return '이번 달 식비 지출 내역이 없습니다.';
    }
  }

  // 교통비 문의
  if (lowerMessage.match(/교통|택시|버스|지하철|카카오|우버/)) {
    const transport = stats.byCategory.transport;
    if (transport) {
      const advice = getTransportAdvice(transport.totalAmount);
      return `이번 달 교통비는 ${transport.totalAmount.toLocaleString()}원입니다. (${transport.count}회)\n\n${advice}`;
    } else {
      return '이번 달 교통비 지출 내역이 없습니다.';
    }
  }

  // 쇼핑 문의
  if (lowerMessage.match(/쇼핑|구매|샀|산/)) {
    const shopping = stats.byCategory.shopping;
    if (shopping) {
      const advice = getShoppingAdvice(shopping.totalAmount);
      return `이번 달 쇼핑 지출은 ${shopping.totalAmount.toLocaleString()}원입니다. (${shopping.count}회)\n\n${advice}`;
    } else {
      return '이번 달 쇼핑 지출 내역이 없습니다.';
    }
  }

  // 절약 팁 요청
  if (lowerMessage.match(/절약|아끼|팁|방법|줄이|줄일/)) {
    return getSavingTips(stats.byCategory);
  }

  // 분석 요청
  if (lowerMessage.match(/분석|어때|상태|현황|리포트/)) {
    return getSpendingAnalysis(stats);
  }

  // 기본 응답
  return '죄송해요, 잘 이해하지 못했어요. 😅\n\n다음과 같이 물어보세요:\n• "총 지출 얼마야?"\n• "식비 분석해줘"\n• "절약 방법 알려줘"\n• "이번 달 어때?"';
}

/**
 * 총 지출 기반 조언
 */
function getSpendingAdvice(total, categories) {
  if (total > 2000000) {
    return '⚠️ 이번 달 지출이 많습니다! 불필요한 소비를 줄여보세요.';
  } else if (total > 1000000) {
    return '💡 평균 수준의 지출입니다. 카테고리별로 확인해보세요.';
  } else {
    return '✅ 잘 관리하고 계시네요! 이대로 유지하세요.';
  }
}

/**
 * 식비 조언
 */
function getFoodAdvice(amount) {
  if (amount > 500000) {
    return '⚠️ 식비가 많이 나왔네요. 외식을 줄이고 집에서 요리해보는 건 어떨까요?';
  } else if (amount > 300000) {
    return '💡 적정 수준이지만, 배달음식을 줄이면 더 절약할 수 있어요.';
  } else {
    return '✅ 식비를 잘 관리하고 계시네요!';
  }
}

/**
 * 교통비 조언
 */
function getTransportAdvice(amount) {
  if (amount > 200000) {
    return '⚠️ 교통비가 많이 나왔네요. 정기권이나 월정액 서비스를 고려해보세요.';
  } else if (amount > 100000) {
    return '💡 대중교통 정기권을 이용하면 30% 정도 절약할 수 있어요.';
  } else {
    return '✅ 교통비를 효율적으로 사용하고 계시네요!';
  }
}

/**
 * 쇼핑 조언
 */
function getShoppingAdvice(amount) {
  if (amount > 500000) {
    return '⚠️ 쇼핑을 많이 하셨네요. 필요한 물건만 구매하도록 노력해보세요.';
  } else if (amount > 200000) {
    return '💡 구매 전 24시간 고민하는 습관을 들이면 충동구매를 줄일 수 있어요.';
  } else {
    return '✅ 계획적인 쇼핑을 하고 계시네요!';
  }
}

/**
 * 절약 팁 제공
 */
function getSavingTips(categories) {
  const tips = ['💡 맞춤 절약 팁을 알려드릴게요!\n'];

  // 식비가 많으면
  if (categories.food && categories.food.totalAmount > 400000) {
    tips.push('🍚 식비: 주말에 식재료를 미리 준비하고, 도시락을 싸가면 월 10만원 이상 절약 가능해요.');
  }

  // 교통비가 많으면
  if (categories.transport && categories.transport.totalAmount > 150000) {
    tips.push('🚇 교통: 정기권으로 바꾸면 30% 절약! 자전거나 도보도 고려해보세요.');
  }

  // 쇼핑이 많으면
  if (categories.shopping && categories.shopping.totalAmount > 300000) {
    tips.push('🛍️ 쇼핑: 장바구니에 담고 24시간 뒤 재검토하세요. 충동구매를 50% 줄일 수 있어요.');
  }

  if (tips.length === 1) {
    return '✅ 지출을 잘 관리하고 계시네요! 현재 패턴을 유지하세요.';
  }

  return tips.join('\n\n');
}

/**
 * 카테고리명 변환 (재사용)
 */
function getCategoryName(category) {
  const names = {
    food: '식비',
    transport: '교통',
    shopping: '쇼핑',
    entertainment: '문화/여가',
    utilities: '공과금',
    healthcare: '의료/건강',
    education: '교육',
    others: '기타'
  };
  return names[category] || category;
}

/**
 * 소비 분석 리포트
 */
function getSpendingAnalysis(stats) {
  const total = stats.total.totalAmount;
  const categories = stats.byCategory;

  // 가장 많이 쓴 카테고리
  let maxCategory = null;
  let maxAmount = 0;

  for (const [category, data] of Object.entries(categories)) {
    if (data.totalAmount > maxAmount) {
      maxAmount = data.totalAmount;
      maxCategory = category;
    }
  }

  const categoryNames = {
    food: '식비',
    transport: '교통',
    shopping: '쇼핑',
    entertainment: '문화/여가',
    utilities: '공과금',
    healthcare: '의료/건강',
    education: '교육',
    others: '기타'
  };

  const categoryName = maxCategory ? categoryNames[maxCategory] : '없음';
  const percentage = maxCategory ? Math.round((maxAmount / total) * 100) : 0;

  let analysis = `📊 이번 달 소비 분석\n\n`;
  analysis += `💰 총 지출: ${total.toLocaleString()}원 (${stats.total.count}건)\n`;
  analysis += `📈 가장 많이 쓴 곳: ${categoryName} (${maxAmount.toLocaleString()}원, ${percentage}%)\n\n`;

  // 다음 달 목표
  const nextMonthGoal = Math.round(total * 0.9);
  analysis += `🎯 다음 달 목표: ${nextMonthGoal.toLocaleString()}원\n`;
  analysis += `(현재보다 10% 절약하기)`;

  return analysis;
}

/**
 * POST /api/chatbot/advice/:notificationId
 * 특정 알림에 대한 상세 조언
 */
router.post('/advice/:notificationId', async (req, res) => {
  try {
    const userId = req.user.userId;
    const { notificationId } = req.params;

    console.log(`💬 알림 기반 조언 요청 - 알림 ID: ${notificationId}`);

    // 알림 조회
    const notification = await Notification.findOne({ _id: notificationId, userId });

    if (!notification) {
      return res.status(404).json({
        success: false,
        message: '알림을 찾을 수 없습니다'
      });
    }

    // 현재 월 통계
    const now = new Date();
    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    const endOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    const stats = await getMonthlyStats(userId, startOfMonth, endOfMonth);

    // 알림 유형에 따른 맞춤 조언 생성
    const advice = generateAdviceForNotification(notification, stats);

    console.log(`✅ 알림 기반 조언 생성 완료`);

    res.json({
      success: true,
      data: {
        notification,
        advice,
        stats
      }
    });

  } catch (error) {
    console.error('❌ 알림 조언 생성 실패:', error);
    res.status(500).json({
      success: false,
      message: '조언 생성에 실패했습니다',
      error: error.message
    });
  }
});

/**
 * 알림 유형에 따른 맞춤 조언 생성
 */
function generateAdviceForNotification(notification, stats) {
  const { type, category, amount, metadata } = notification;

  let advice = '';

  switch (metadata?.triggerType) {
    case 'high_amount':
      advice = `💸 ${amount.toLocaleString()}원의 고액 지출이 발생했습니다.\n\n`;
      advice += `📊 이번 달 ${getCategoryName(category)} 총 지출: ${stats.byCategory[category]?.totalAmount.toLocaleString() || 0}원\n\n`;
      advice += `💡 조언:\n`;
      advice += `• 이 지출이 계획된 것이었나요?\n`;
      advice += `• 같은 금액으로 할 수 있는 대안이 있었나요?\n`;
      advice += `• 다음엔 여러 업체를 비교해보세요\n`;
      advice += `• 할인이나 쿠폰을 활용하면 10-20% 절약 가능합니다`;
      break;

    case 'budget_exceeded':
      const overAmount = metadata.overAmount;
      advice = `⚠️ ${getCategoryName(category)} 예산을 ${overAmount.toLocaleString()}원 초과했습니다!\n\n`;
      advice += `📊 현재 상황:\n`;
      advice += `• 이번 달 지출: ${amount.toLocaleString()}원\n`;
      advice += `• 목표 예산: ${metadata.limit.toLocaleString()}원\n`;
      advice += `• 초과 금액: ${overAmount.toLocaleString()}원\n\n`;
      advice += `💡 남은 기간 절약 방법:\n`;

      if (category === 'food') {
        advice += `• 외식 대신 집밥으로 전환\n`;
        advice += `• 도시락 준비하기\n`;
        advice += `• 커피는 집에서 만들어 가기`;
      } else if (category === 'transport') {
        advice += `• 정기권으로 전환\n`;
        advice += `• 가까운 거리는 도보나 자전거\n`;
        advice += `• 카풀 서비스 이용`;
      } else if (category === 'shopping') {
        advice += `• 필수품만 구매\n`;
        advice += `• 장바구니에 담고 24시간 후 재검토\n`;
        advice += `• 중고 거래 플랫폼 활용`;
      }
      break;

    case 'weekly_spike':
      const increasePercent = metadata.increasePercent;
      advice = `📈 이번 주 ${getCategoryName(category)} 지출이 ${increasePercent}% 급증했습니다!\n\n`;
      advice += `📊 비교:\n`;
      advice += `• 지난 주: ${metadata.lastWeekAmount.toLocaleString()}원\n`;
      advice += `• 이번 주: ${amount.toLocaleString()}원\n`;
      advice += `• 증가액: ${(amount - metadata.lastWeekAmount).toLocaleString()}원\n\n`;
      advice += `🔍 체크리스트:\n`;
      advice += `• 특별한 이벤트나 행사가 있었나요?\n`;
      advice += `• 충동구매가 있었나요?\n`;
      advice += `• 다음 주는 지출을 줄여보는 건 어떨까요?`;
      break;

    case 'frequent_dining':
      const todayCount = metadata.todayCount;
      advice = `🍽️ 오늘 ${todayCount}번째 외식입니다!\n\n`;
      advice += `💡 식비 절약 팁:\n`;
      advice += `• 주말에 식재료 준비하기\n`;
      advice += `• 도시락 싸가기 (월 10만원 절약)\n`;
      advice += `• 간단한 요리 레시피 배우기\n`;
      advice += `• 외식은 주 2-3회로 제한\n\n`;
      advice += `📊 예상 절약액:\n`;
      advice += `• 도시락 주 5회: 월 10만원 절약\n`;
      advice += `• 커피 집에서: 월 3만원 절약`;
      break;

    case 'weekend_shopping':
          advice = `🛍️ 주말 쇼핑 ${amount.toLocaleString()}원!\n\n`;
          advice += `💡 충동구매 방지 팁:\n`;
          advice += `• 쇼핑 목록 미리 작성하기\n`;
          advice += `• 장바구니에 담고 24시간 기다리기\n`;
          advice += `• "정말 필요한가?" 3번 자문하기\n`;
          advice += `• 중고 거래 먼저 확인하기\n\n`;
          advice += `📊 주말 쇼핑 통계:\n`;
          advice += `• 충동구매 확률: 70%\n`;
          advice += `• 24시간 후 재검토 시 구매 취소율: 50%`;
          break;

        case 'late_night_spending':
          advice = `🌙 심야 ${getCategoryName(category)} 지출 ${amount.toLocaleString()}원!\n\n`;
          advice += `💡 심야 소비 줄이기:\n`;

          if (category === 'food') {
            advice += `• 저녁 식사 미리 준비하기\n`;
            advice += `• 간식 미리 구비하기\n`;
            advice += `• 배달 앱 삭제 고려\n`;
            advice += `• 심야 배달비는 2-3배 비쌉니다`;
          } else if (category === 'transport') {
            advice += `• 대중교통 막차 시간 체크\n`;
            advice += `• 숙박 시설 이용 고려\n`;
            advice += `• 심야 택시는 할증료 부과됩니다\n`;
            advice += `• 카풀 서비스 활용`;
          }
          break;

        default:
          // 기본 조언
          advice = `📊 ${notification.title}\n\n`;
          advice += `${notification.message}\n\n`;
          advice += `💡 일반 조언:\n`;
          advice += `• 지출 내역을 정기적으로 확인하세요\n`;
          advice += `• 예산을 설정하고 지키세요\n`;
          advice += `• 불필요한 구독 서비스를 정리하세요\n`;
          advice += `• 고정 지출과 변동 지출을 구분하세요`;
      }

      return advice;
    }

module.exports = router;