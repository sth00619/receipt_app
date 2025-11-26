const express = require('express');
const router = express.Router();
const Receipt = require('../models/Receipt');
const { verifyAuth } = require('../middleware/auth');

router.use(verifyAuth);

// 챗봇 메시지 처리
router.post('/message', async (req, res) => {
  try {
    const { message } = req.body;

    if (!message) {
      return res.status(400).json({
        success: false,
        message: 'Message is required'
      });
    }

    console.log(`💬 챗봇 메시지: ${message}`);

    // 사용자 소비 데이터 조회
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
          _id: '$category',
          totalAmount: { $sum: '$totalAmount' },
          count: { $sum: 1 }
        }
      },
      {
        $sort: { totalAmount: -1 }
      }
    ]);

    // 메시지 분석 및 응답 생성
    const response = generateChatbotResponse(message, monthlyStats);

    res.json({
      success: true,
      data: {
        message: response,
        stats: monthlyStats
      }
    });

  } catch (error) {
    console.error('❌ 챗봇 오류:', error);
    res.status(500).json({
      success: false,
      message: 'Chatbot error',
      error: error.message
    });
  }
});

// 챗봇 응답 생성
function generateChatbotResponse(message, stats) {
  const lowerMessage = message.toLowerCase();

  // 총 지출 문의
  if (lowerMessage.includes('총') || lowerMessage.includes('전체') || lowerMessage.includes('얼마')) {
    const total = stats.reduce((sum, s) => sum + s.totalAmount, 0);
    return `이번 달 총 지출은 ${Math.round(total).toLocaleString()}원입니다. ${getSpendingAdvice(total, stats)}`;
  }

  // 카테고리별 지출 문의
  if (lowerMessage.includes('식비') || lowerMessage.includes('음식')) {
    const foodStats = stats.find(s => s._id === 'food');
    if (foodStats) {
      return `이번 달 식비는 ${Math.round(foodStats.totalAmount).toLocaleString()}원입니다 (${foodStats.count}건). ${getFoodAdvice(foodStats.totalAmount)}`;
    }
    return '이번 달 식비 지출 내역이 없습니다.';
  }

  if (lowerMessage.includes('교통') || lowerMessage.includes('교통비')) {
    const transportStats = stats.find(s => s._id === 'transport');
    if (transportStats) {
      return `이번 달 교통비는 ${Math.round(transportStats.totalAmount).toLocaleString()}원입니다 (${transportStats.count}건). ${getTransportAdvice(transportStats.totalAmount)}`;
    }
    return '이번 달 교통비 지출 내역이 없습니다.';
  }

  if (lowerMessage.includes('쇼핑')) {
    const shoppingStats = stats.find(s => s._id === 'shopping');
    if (shoppingStats) {
      return `이번 달 쇼핑 지출은 ${Math.round(shoppingStats.totalAmount).toLocaleString()}원입니다 (${shoppingStats.count}건). ${getShoppingAdvice(shoppingStats.totalAmount)}`;
    }
    return '이번 달 쇼핑 지출 내역이 없습니다.';
  }

  // 절약 팁 문의
  if (lowerMessage.includes('절약') || lowerMessage.includes('팁') || lowerMessage.includes('방법')) {
    return getSavingTips(stats);
  }

  // 분석 문의
  if (lowerMessage.includes('분석') || lowerMessage.includes('어때')) {
    return getSpendingAnalysis(stats);
  }

  // 기본 응답
  return `안녕하세요! 저는 Receiptify 소비 관리 도우미입니다. 😊

다음과 같은 질문을 해보세요:
- "이번 달 총 지출은 얼마야?"
- "식비 지출은 어때?"
- "절약 팁 알려줘"
- "소비 분석해줘"

무엇이든 물어보세요!`;
}

// 전체 지출 조언
function getSpendingAdvice(total, stats) {
  if (total > 2000000) {
    return '지출이 많은 편입니다. 필수 지출과 선택 지출을 구분해 관리해보세요.';
  } else if (total > 1000000) {
    return '적정 수준의 지출입니다. 계속 잘 관리하고 계시네요! 👍';
  } else {
    return '절약을 잘 하고 계시네요! 여유 자금은 저축하는 것을 추천드립니다. 💰';
  }
}

// 식비 조언
function getFoodAdvice(amount) {
  if (amount > 500000) {
    return '식비가 많은 편입니다. 외식을 줄이고 집에서 요리해보세요. 월 10만원 이상 절약할 수 있습니다! 🍳';
  } else if (amount > 300000) {
    return '적정한 식비입니다. 가끔은 집밥으로 건강도 챙기세요! 🥗';
  } else {
    return '식비 관리를 잘하고 계시네요! 👏';
  }
}

// 교통비 조언
function getTransportAdvice(amount) {
  if (amount > 200000) {
    return '교통비가 많네요. 대중교통 정기권이나 카풀을 이용하면 30% 절약할 수 있습니다! 🚇';
  } else if (amount > 100000) {
    return '적정한 교통비입니다. 대중교통을 잘 활용하고 계시네요! 🚌';
  } else {
    return '교통비를 효율적으로 관리하고 계시네요! 👍';
  }
}

// 쇼핑 조언
function getShoppingAdvice(amount) {
  if (amount > 500000) {
    return '쇼핑 지출이 많습니다. 필요한 물품만 구매하고, 세일 기간을 활용해보세요! 🛍️';
  } else if (amount > 200000) {
    return '쇼핑을 적당히 즐기고 계시네요. 충동구매는 자제하세요! 😊';
  } else {
    return '합리적인 쇼핑을 하고 계시네요! 💯';
  }
}

// 절약 팁
function getSavingTips(stats) {
  const tips = [];

  const foodStats = stats.find(s => s._id === 'food');
  if (foodStats && foodStats.totalAmount > 400000) {
    tips.push('🍳 식비 절약: 주 2-3회 집밥으로 월 10만원 절약');
  }

  const transportStats = stats.find(s => s._id === 'transport');
  if (transportStats && transportStats.totalAmount > 150000) {
    tips.push('🚇 교통비 절약: 대중교통 정기권 구매로 30% 절약');
  }

  const shoppingStats = stats.find(s => s._id === 'shopping');
  if (shoppingStats && shoppingStats.totalAmount > 300000) {
    tips.push('🛍️ 쇼핑 절약: 필요한 물품 리스트 작성 후 구매');
  }

  if (tips.length === 0) {
    return '현재 지출이 적절합니다! 계속 이렇게 관리하세요. 💪\n\n추가 팁:\n• 고정 지출 자동이체 활용\n• 남는 돈은 즉시 저축\n• 월별 예산 설정하기';
  }

  return '맞춤 절약 팁입니다:\n\n' + tips.join('\n') + '\n\n이 방법들로 월 20-30만원 절약 가능합니다! 💰';
}

// 소비 분석
function getSpendingAnalysis(stats) {
  if (stats.length === 0) {
    return '아직 지출 내역이 없습니다. 영수증을 등록해보세요!';
  }

  const total = stats.reduce((sum, s) => sum + s.totalAmount, 0);
  const topCategory = stats[0];
  const categoryName = getCategoryName(topCategory._id);

  const analysis = `📊 이번 달 소비 분석

- 총 지출: ${Math.round(total).toLocaleString()}원
- 가장 많이 쓴 카테고리: ${categoryName} (${Math.round(topCategory.totalAmount).toLocaleString()}원)
- 전체의 ${Math.round((topCategory.totalAmount / total) * 100)}%를 차지합니다

${getSpendingAdvice(total, stats)}

💡 다음 달 목표:
- ${categoryName} 지출 10% 줄이기
- 예산 내에서 소비하기
- 충동구매 자제하기`;

  return analysis;
}

function getCategoryName(code) {
  const names = {
    'food': '식비',
    'transport': '교통',
    'shopping': '쇼핑',
    'healthcare': '건강/의료',
    'entertainment': '문화/여가',
    'utilities': '공과금',
    'others': '기타'
  };
  return names[code] || '기타';
}

module.exports = router;