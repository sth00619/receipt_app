const ChatMessage = require('../models/ChatMessage');

/**
 * 일일 소비 분석 챗봇 메시지 생성
 */
async function createDailyAnalysisMessage(userId, data) {
    const { todayTotal, dailyBudget, monthTotal, monthlyBudget, notificationId } = data;

    let message = `📊 오늘의 소비 분석\n\n`;
    message += `💰 오늘 지출: ${todayTotal.toLocaleString()}원\n`;
    message += `📈 일일 예산: ${dailyBudget.toLocaleString()}원\n`;

    if (todayTotal > dailyBudget) {
        const overAmount = todayTotal - dailyBudget;
        message += `⚠️ 예산 초과: ${overAmount.toLocaleString()}원\n\n`;
        message += `💡 절약 팁:\n`;
        message += `• 외식 대신 집밥으로 전환\n`;
        message += `• 커피는 집에서 만들어 가기\n`;
        message += `• 불필요한 택시 이용 줄이기\n\n`;
        message += `🎯 내일 목표: ${Math.round(dailyBudget * 0.8).toLocaleString()}원 이하로 지출하기`;
    } else {
        message += `✅ 예산 내 지출 성공!\n\n`;
        message += `💡 잘하고 계세요! 이대로 유지하세요.`;
    }

    // 월간 예산 정보 추가
    if (monthTotal >= monthlyBudget * 0.9) {
        const percentage = Math.round((monthTotal / monthlyBudget) * 100);
        message += `\n\n📅 이번 달 예산 ${percentage}% 도달\n`;
        message += `• 이번 달 지출: ${monthTotal.toLocaleString()}원\n`;
        message += `• 월간 예산: ${monthlyBudget.toLocaleString()}원\n`;
        message += `• 남은 예산: ${(monthlyBudget - monthTotal).toLocaleString()}원`;
    }

    await ChatMessage.create({
        userId,
        role: 'system',
        message,
        metadata: {
            type: 'daily_analysis',
            todayTotal,
            dailyBudget,
            monthTotal,
            monthlyBudget
        },
        relatedNotificationId: notificationId
    });

    console.log(`✅ [CHATBOT] 일일 분석 메시지 생성 완료 - 사용자: ${userId}`);
}

/**
 * 주간 리포트 챗봇 메시지 생성
 */
async function createWeeklyReportMessage(userId, data) {
    const { weeklyTotal, receiptCount, topCategory, topAmount, byCategory, weekStart, weekEnd, notificationId } = data;

    const categoryNames = {
        food: '식비',
        transport: '교통',
        shopping: '쇼핑',
        entertainment: '문화/여가',
        utilities: '공과금',
        healthcare: '의료/건강',
        others: '기타'
    };

    const topCategoryName = topCategory ? categoryNames[topCategory] : '없음';

    // 날짜 포맷
    const formatDate = (date) => {
        const d = new Date(date);
        return `${d.getMonth() + 1}/${d.getDate()}`;
    };

    let message = `📈 주간 소비 리포트 (${formatDate(weekStart)} ~ ${formatDate(weekEnd)})\n\n`;
    message += `💰 총 지출: ${weeklyTotal.toLocaleString()}원 (${receiptCount}건)\n`;
    message += `📊 가장 많이 쓴 곳: ${topCategoryName} (${topAmount.toLocaleString()}원)\n\n`;

    // 카테고리별 지출
    if (Object.keys(byCategory).length > 0) {
        message += `📋 카테고리별 지출:\n`;
        for (const [category, amount] of Object.entries(byCategory)) {
            const name = categoryNames[category] || category;
            message += `• ${name}: ${amount.toLocaleString()}원\n`;
        }
        message += `\n`;
    }

    // 다음 주 목표
    const nextWeekGoal = Math.round(weeklyTotal * 0.9);
    message += `💡 다음 주 목표:\n`;
    if (topCategory === 'food') {
        message += `• 식비를 ${nextWeekGoal.toLocaleString()}원 이하로 줄이기\n`;
        message += `• 외식 횟수 줄이기\n`;
        message += `• 도시락 준비하기`;
    } else if (topCategory === 'transport') {
        message += `• 교통비를 ${nextWeekGoal.toLocaleString()}원 이하로 줄이기\n`;
        message += `• 정기권 이용하기\n`;
        message += `• 가까운 거리는 도보로`;
    } else {
        message += `• 총 지출을 ${nextWeekGoal.toLocaleString()}원 이하로 줄이기\n`;
        message += `• 계획적인 소비하기`;
    }

    message += `\n\n✨ 잘하고 있어요! 이번 주도 화이팅!`;

    await ChatMessage.create({
        userId,
        role: 'system',
        message,
        metadata: {
            type: 'weekly_report',
            weeklyTotal,
            receiptCount,
            topCategory,
            topAmount,
            byCategory
        },
        relatedNotificationId: notificationId
    });

    console.log(`✅ [CHATBOT] 주간 리포트 메시지 생성 완료 - 사용자: ${userId}`);
}

/**
 * 월간 요약 챗봇 메시지 생성
 */
async function createMonthlyReportMessage(userId, data) {
    const { monthlyTotal, monthlyBudget, receiptCount, month, year, notificationId } = data;

    const percentage = monthlyBudget > 0 ? Math.round((monthlyTotal / monthlyBudget) * 100) : 0;

    let message = `📅 월간 소비 요약 (${year}년 ${month}월)\n\n`;
    message += `💰 총 지출: ${monthlyTotal.toLocaleString()}원\n`;
    message += `📊 예산 대비: ${percentage}% (예산: ${monthlyBudget.toLocaleString()}원)\n`;
    message += `📝 영수증: ${receiptCount}건\n\n`;

    // 예산 달성 평가
    if (monthlyTotal <= monthlyBudget) {
        message += `🎉 축하합니다! 예산 내에서 잘 관리하셨어요!\n\n`;
    } else {
        const overAmount = monthlyTotal - monthlyBudget;
        message += `⚠️ 예산을 ${overAmount.toLocaleString()}원 초과했습니다.\n\n`;
    }

    // 다음 달 목표
    message += `🎯 다음 달 목표:\n`;
    if (monthlyTotal > monthlyBudget) {
        message += `• 예산 내에서 지출하기\n`;
        message += `• 불필요한 지출 줄이기\n`;
        message += `• 카테고리별 예산 설정하기`;
    } else {
        message += `• 현재 패턴 유지하기\n`;
        message += `• 영수증 꾸준히 등록하기\n`;
        message += `• 절약한 금액 저축하기`;
    }

    message += `\n\n💡 ${month + 1}월에는 더 나은 소비 습관을 만들어봐요!`;

    await ChatMessage.create({
        userId,
        role: 'system',
        message,
        metadata: {
            type: 'monthly_report',
            monthlyTotal,
            monthlyBudget,
            receiptCount,
            month,
            year,
            percentage
        },
        relatedNotificationId: notificationId
    });

    console.log(`✅ [CHATBOT] 월간 요약 메시지 생성 완료 - 사용자: ${userId}`);
}

module.exports = {
    createDailyAnalysisMessage,
    createWeeklyReportMessage,
    createMonthlyReportMessage
};
