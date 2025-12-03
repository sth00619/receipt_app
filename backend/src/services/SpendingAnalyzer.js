const Receipt = require('../models/Receipt');
const Notification = require('../models/Notification');

class SpendingAnalyzer {

  /**
   * 실시간 소비 분석 (영수증 추가 시 자동 호출)
   */
  async analyzeRealtimeSpending(userId, newReceipt) {
    try {
      console.log(`🔍 실시간 소비 분석 시작 - 사용자: ${userId}`);

      const now = new Date();
      const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
      const endOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0);

      // 이번 주 시작일
      const startOfWeek = new Date(now);
      startOfWeek.setDate(now.getDate() - now.getDay());
      startOfWeek.setHours(0, 0, 0, 0);

      // 현재 월 통계
      const monthlyStats = await this.getCategoryStats(userId, startOfMonth, endOfMonth);

      // 이번 주 통계
      const weeklyStats = await this.getCategoryStats(userId, startOfWeek, now);

      // 지난 주 통계 (비교용)
      const lastWeekStart = new Date(startOfWeek);
      lastWeekStart.setDate(lastWeekStart.getDate() - 7);
      const lastWeekEnd = new Date(startOfWeek);
      lastWeekEnd.setDate(lastWeekEnd.getDate() - 1);
      const lastWeekStats = await this.getCategoryStats(userId, lastWeekStart, lastWeekEnd);

      console.log('📊 월간 통계:', monthlyStats);
      console.log('📊 이번 주 통계:', weeklyStats);
      console.log('📊 지난 주 통계:', lastWeekStats);

      // 실시간 알림 생성
      const alerts = await this.generateRealtimeAlerts(
        userId,
        newReceipt,
        monthlyStats,
        weeklyStats,
        lastWeekStats
      );

      console.log(`✅ 실시간 알림 ${alerts.length}개 생성 완료`);

      return {
        alerts,
        monthlyStats,
        weeklyStats
      };

    } catch (error) {
      console.error('❌ 실시간 소비 분석 중 오류:', error);
      throw error;
    }
  }

  /**
   * 실시간 알림 생성
   */
  async generateRealtimeAlerts(userId, newReceipt, monthlyStats, weeklyStats, lastWeekStats) {
    const alerts = [];
    const now = new Date();
    const category = newReceipt.category;
    const amount = newReceipt.totalAmount;

    // 1. 고액 지출 즉시 알림 (50만원 이상)
    if (amount >= 500000) {
      alerts.push({
        userId,
        type: 'spending_alert',
        title: '💸 고액 지출 발생!',
        message: `${this.getCategoryName(category)}에서 ${amount.toLocaleString()}원을 지출하셨습니다. 챗봇에게 절약 팁을 물어보세요!`,
        category,
        amount,
        priority: 'high',
        isRead: false,
        metadata: {
          triggerType: 'high_amount',
          receiptId: newReceipt._id,
          chatbotSuggested: true
        }
      });
    }

    // 2. 카테고리별 월간 한도 체크
    const categoryLimits = {
      food: 500000,
      transport: 200000,
      shopping: 400000,
      entertainment: 300000
    };

    const monthlyCategory = monthlyStats[category];
    const limit = categoryLimits[category];

    if (limit && monthlyCategory && monthlyCategory.totalAmount >= limit) {
      const overAmount = monthlyCategory.totalAmount - limit;

      alerts.push({
        userId,
        type: 'budget_warning',
        title: `⚠️ ${this.getCategoryName(category)} 예산 초과!`,
        message: `이번 달 ${this.getCategoryName(category)} 지출이 ${monthlyCategory.totalAmount.toLocaleString()}원으로 예산을 ${overAmount.toLocaleString()}원 초과했습니다. 챗봇과 상담해보세요!`,
        category,
        amount: monthlyCategory.totalAmount,
        priority: 'high',
        isRead: false,
        metadata: {
          triggerType: 'budget_exceeded',
          limit,
          overAmount,
          chatbotSuggested: true
        }
      });
    }

    // 3. 주간 소비 급증 체크 (지난 주 대비 50% 이상)
    const thisWeekCategory = weeklyStats[category];
    const lastWeekCategory = lastWeekStats[category];

    if (thisWeekCategory && lastWeekCategory &&
        thisWeekCategory.totalAmount > lastWeekCategory.totalAmount * 1.5) {

      const increasePercent = Math.round(
        ((thisWeekCategory.totalAmount - lastWeekCategory.totalAmount) / lastWeekCategory.totalAmount) * 100
      );

      alerts.push({
        userId,
        type: 'spending_alert',
        title: `📈 ${this.getCategoryName(category)} 지출 급증!`,
        message: `이번 주 ${this.getCategoryName(category)} 지출이 지난 주 대비 ${increasePercent}% 증가했습니다. (${thisWeekCategory.totalAmount.toLocaleString()}원) 챗봇이 도와드릴게요!`,
        category,
        amount: thisWeekCategory.totalAmount,
        priority: 'medium',
        isRead: false,
        metadata: {
          triggerType: 'weekly_spike',
          increasePercent,
          lastWeekAmount: lastWeekCategory.totalAmount,
          chatbotSuggested: true
        }
      });
    }

    // 4. 하루 3회 이상 식비 지출 (외식 과다)
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (category === 'food') {
      const todayFoodCount = await Receipt.countDocuments({
        userId,
        category: 'food',
        transactionDate: { $gte: today }
      });

      if (todayFoodCount >= 3) {
        alerts.push({
          userId,
          type: 'tip',
          title: '🍽️ 오늘 외식이 많으시네요!',
          message: `오늘 ${todayFoodCount}번째 외식입니다. 집밥으로 식비를 절약해보는 건 어떨까요? 챗봇에게 팁을 물어보세요!`,
          category: 'food',
          priority: 'low',
          isRead: false,
          metadata: {
            triggerType: 'frequent_dining',
            todayCount: todayFoodCount,
            chatbotSuggested: true
          }
        });
      }
    }

    // 5. 주말 쇼핑 과소비 체크
    const dayOfWeek = now.getDay();
    if ((dayOfWeek === 0 || dayOfWeek === 6) && category === 'shopping' && amount >= 100000) {
      alerts.push({
        userId,
        type: 'tip',
        title: '🛍️ 주말 쇼핑 알림',
        message: `주말에 ${amount.toLocaleString()}원을 쇼핑하셨네요. 충동구매는 아니었나요? 챗봇과 상담해보세요!`,
        category: 'shopping',
        amount,
        priority: 'low',
        isRead: false,
        metadata: {
          triggerType: 'weekend_shopping',
          chatbotSuggested: true
        }
      });
    }

    // 6. 심야 택시/배달 알림 (23시~5시)
    const hour = now.getHours();
    if ((hour >= 23 || hour < 5) && (category === 'transport' || category === 'food')) {
      alerts.push({
        userId,
        type: 'tip',
        title: '🌙 심야 소비 알림',
        message: `심야에 ${this.getCategoryName(category)} ${amount.toLocaleString()}원을 지출하셨습니다. 다음엔 미리 준비해보는 건 어떨까요?`,
        category,
        amount,
        priority: 'low',
        isRead: false,
        metadata: {
          triggerType: 'late_night_spending',
          chatbotSuggested: true
        }
      });
    }

    // 알림 저장
    if (alerts.length > 0) {
      await Notification.insertMany(alerts);
      console.log(`📬 ${alerts.length}개 실시간 알림 저장 완료`);
    }

    return alerts;
  }

  /**
   * 사용자의 소비 패턴 분석 및 알림 생성
   */
  async analyzeUserSpending(userId) {
    try {
      console.log(`📊 사용자 ${userId} 소비 분석 시작`);

      // 이번 달 데이터
      const now = new Date();
      const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
      const endOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59);

      // 지난 달 데이터
      const startOfLastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1);
      const endOfLastMonth = new Date(now.getFullYear(), now.getMonth(), 0, 23, 59, 59);

      // 이번 달 카테고리별 지출
      const currentMonthStats = await this.getCategoryStats(userId, startOfMonth, endOfMonth);

      // 지난 달 카테고리별 지출
      const lastMonthStats = await this.getCategoryStats(userId, startOfLastMonth, endOfLastMonth);

      // 알림 생성
      await this.generateAlerts(userId, currentMonthStats, lastMonthStats);

      console.log(`✅ 소비 분석 완료`);

    } catch (error) {
      console.error('❌ 소비 분석 오류:', error);
      throw error;
    }
  }

  async getCategoryStats(userId, startDate, endDate) {
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

    const statsMap = {};
    stats.forEach(stat => {
      statsMap[stat._id] = {
        totalAmount: stat.totalAmount,
        count: stat.count
      };
    });

    return statsMap;
  }

  /**
   * 알림 생성
   */
  async generateAlerts(userId, currentStats, lastStats) {
    const alerts = [];

    // 카테고리별 비교
    for (const [category, current] of Object.entries(currentStats)) {
      const last = lastStats[category];

      if (last && current.amount > last.amount * 1.3) {
        // 30% 이상 증가
        const increasePercent = Math.round(((current.amount - last.amount) / last.amount) * 100);

        alerts.push({
          userId,
          type: 'category_alert',
          title: `${this.getCategoryName(category)} 지출 증가`,
          message: `이번 달 ${this.getCategoryName(category)} 지출이 지난 달보다 ${increasePercent}% 증가했습니다. (${Math.round(current.amount).toLocaleString()}원)`,
          category,
          amount: current.amount,
          priority: increasePercent > 50 ? 'high' : 'medium',
          metadata: {
            currentAmount: current.amount,
            lastAmount: last.amount,
            increasePercent
          }
        });
      }

      // 특정 카테고리 과다 지출 경고
      if (category === 'food' && current.amount > 500000) {
        alerts.push({
          userId,
          type: 'spending_alert',
          title: '식비 지출 주의',
          message: `이번 달 식비가 ${Math.round(current.amount).toLocaleString()}원입니다. 외식을 줄이고 집밥을 늘려보세요.`,
          category,
          amount: current.amount,
          priority: 'high'
        });
      }

      if (category === 'shopping' && current.amount > 300000) {
        alerts.push({
          userId,
          type: 'spending_alert',
          title: '쇼핑 지출 주의',
          message: `이번 달 쇼핑 지출이 ${Math.round(current.amount).toLocaleString()}원입니다. 필요한 물품만 구매하세요.`,
          category,
          amount: current.amount,
          priority: 'medium'
        });
      }
    }

    // 알림 저장
    if (alerts.length > 0) {
      await Notification.insertMany(alerts);
      console.log(`✅ ${alerts.length}개 알림 생성`);
    }
  }

  getCategoryName(code) {
    const names = {
      'food': '식비',
      'transport': '교통',
      'shopping': '쇼핑',
      'healthcare': '건강/의료',
      'utilities': '공과금',
      'entertainment': '문화/여가',
      'utilities': '공과금',
      'others': '기타'
    };
    return names[code] || '기타';
  }

  /**
   * 소비 팁 생성
   */
  async generateSpendingTips(userId, categoryStats) {
    const tips = [];

    // 식비 팁
    if (categoryStats['food'] && categoryStats['food'].count > 20) {
      tips.push({
        userId,
        type: 'tip',
        title: '💡 식비 절약 팁',
        message: '이번 달 외식 횟수가 많습니다. 주 2-3회는 집에서 요리해보세요. 월 10만원 이상 절약할 수 있습니다.',
        category: 'food',
        priority: 'low'
      });
    }

    // 교통비 팁
    if (categoryStats['transport'] && categoryStats['transport'].amount > 150000) {
      tips.push({
        userId,
        type: 'tip',
        title: '💡 교통비 절약 팁',
        message: '대중교통 정기권이나 카풀 서비스를 이용하면 교통비를 30% 절약할 수 있습니다.',
        category: 'transport',
        priority: 'low'
      });
    }

    if (tips.length > 0) {
      await Notification.insertMany(tips);
    }
  }
}

module.exports = new SpendingAnalyzer();