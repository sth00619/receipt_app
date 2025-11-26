const Receipt = require('../models/Receipt');
const Notification = require('../models/Notification');

class SpendingAnalyzer {

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

  /**
   * 카테고리별 통계 조회
   */
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

    const result = {};
    stats.forEach(stat => {
      result[stat._id] = {
        amount: stat.totalAmount,
        count: stat.count
      };
    });

    return result;
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