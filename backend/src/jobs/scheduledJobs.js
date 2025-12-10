const cron = require('node-cron');
const User = require('../models/User');
const Receipt = require('../models/Receipt');
const Notification = require('../models/Notification');

/**
 * 매일 자정에 실행 - 일일 소비 분석
 */
function scheduleDailyAnalysis() {
    cron.schedule('0 0 * * *', async () => {
        console.log('📊 [CRON] 일일 소비 분석 시작...', new Date().toISOString());

        try {
            const users = await User.find({});
            let processedCount = 0;
            let notificationCount = 0;

            for (const user of users) {
                const today = new Date();
                const todayStart = new Date(today.setHours(0, 0, 0, 0));
                const todayEnd = new Date(today.setHours(23, 59, 59, 999));

                // 오늘 지출 계산
                const todayReceipts = await Receipt.find({
                    userId: user._id,
                    transactionDate: {
                        $gte: todayStart,
                        $lte: todayEnd
                    }
                });

                const todayTotal = todayReceipts.reduce((sum, r) => sum + r.totalAmount, 0);

                // 일일 예산 체크 (기본값: 100,000원)
                const dailyBudget = user.dailyBudget || 100000;

                if (todayTotal > dailyBudget) {
                    await Notification.create({
                        userId: user._id,
                        type: 'budget_warning',
                        title: '⚠️ 오늘 예산 초과!',
                        message: `오늘 ${todayTotal.toLocaleString()}원 지출 (예산: ${dailyBudget.toLocaleString()}원, 초과: ${(todayTotal - dailyBudget).toLocaleString()}원)`,
                        priority: 'high',
                        isRead: false,
                        metadata: {
                            triggerType: 'daily_budget_exceeded',
                            todayTotal,
                            dailyBudget,
                            overAmount: todayTotal - dailyBudget
                        },
                        createdAt: new Date(),
                        updatedAt: new Date()
                    });
                    notificationCount++;
                }

                // 월간 예산 근접 체크
                const monthStart = new Date(today.getFullYear(), today.getMonth(), 1);
                const monthReceipts = await Receipt.find({
                    userId: user._id,
                    transactionDate: { $gte: monthStart }
                });

                const monthTotal = monthReceipts.reduce((sum, r) => sum + r.totalAmount, 0);
                const monthlyBudget = user.monthlyBudget || 2000000;

                // 월 예산의 90% 도달 시 경고
                if (monthTotal >= monthlyBudget * 0.9 && monthTotal < monthlyBudget) {
                    await Notification.create({
                        userId: user._id,
                        type: 'budget_warning',
                        title: '⚠️ 월간 예산 90% 도달!',
                        message: `이번 달 ${monthTotal.toLocaleString()}원 지출 (예산: ${monthlyBudget.toLocaleString()}원, 남은 예산: ${(monthlyBudget - monthTotal).toLocaleString()}원)`,
                        priority: 'medium',
                        isRead: false,
                        metadata: {
                            triggerType: 'monthly_budget_warning',
                            monthTotal,
                            monthlyBudget,
                            percentage: Math.round((monthTotal / monthlyBudget) * 100)
                        },
                        createdAt: new Date(),
                        updatedAt: new Date()
                    });
                    notificationCount++;
                }

                processedCount++;
            }

            console.log(`✅ [CRON] 일일 소비 분석 완료 - 처리: ${processedCount}명, 알림 생성: ${notificationCount}개`);
        } catch (error) {
            console.error('❌ [CRON] 일일 소비 분석 실패:', error);
        }
    }, {
        timezone: "Asia/Seoul"
    });

    console.log('⏰ [CRON] 일일 소비 분석 스케줄 등록 완료 (매일 00:00 KST)');
}

/**
 * 매주 월요일 오전 9시 - 주간 리포트
 */
function scheduleWeeklyReport() {
    cron.schedule('0 9 * * 1', async () => {
        console.log('📈 [CRON] 주간 리포트 생성 시작...', new Date().toISOString());

        try {
            const users = await User.find({});
            let processedCount = 0;

            for (const user of users) {
                const now = new Date();
                const lastWeekStart = new Date(now);
                lastWeekStart.setDate(now.getDate() - 7);
                lastWeekStart.setHours(0, 0, 0, 0);

                const lastWeekEnd = new Date(now);
                lastWeekEnd.setHours(0, 0, 0, 0);

                // 지난 주 영수증 조회
                const weeklyReceipts = await Receipt.find({
                    userId: user._id,
                    transactionDate: {
                        $gte: lastWeekStart,
                        $lt: lastWeekEnd
                    }
                });

                const weeklyTotal = weeklyReceipts.reduce((sum, r) => sum + r.totalAmount, 0);

                // 카테고리별 집계
                const byCategory = {};
                weeklyReceipts.forEach(receipt => {
                    if (!byCategory[receipt.category]) {
                        byCategory[receipt.category] = 0;
                    }
                    byCategory[receipt.category] += receipt.totalAmount;
                });

                // 가장 많이 쓴 카테고리
                let topCategory = null;
                let topAmount = 0;
                for (const [category, amount] of Object.entries(byCategory)) {
                    if (amount > topAmount) {
                        topAmount = amount;
                        topCategory = category;
                    }
                }

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

                // 주간 리포트 알림 생성
                await Notification.create({
                    userId: user._id,
                    type: 'weekly_report',
                    title: '📊 주간 소비 리포트',
                    message: `지난 주 총 ${weeklyTotal.toLocaleString()}원 지출 (${weeklyReceipts.length}건)\n가장 많이 쓴 곳: ${topCategoryName} (${topAmount.toLocaleString()}원)`,
                    priority: 'low',
                    isRead: false,
                    metadata: {
                        triggerType: 'weekly_report',
                        weeklyTotal,
                        receiptCount: weeklyReceipts.length,
                        topCategory,
                        topAmount,
                        byCategory,
                        weekStart: lastWeekStart,
                        weekEnd: lastWeekEnd
                    },
                    createdAt: new Date(),
                    updatedAt: new Date()
                });

                processedCount++;
            }

            console.log(`✅ [CRON] 주간 리포트 생성 완료 - 처리: ${processedCount}명`);
        } catch (error) {
            console.error('❌ [CRON] 주간 리포트 생성 실패:', error);
        }
    }, {
        timezone: "Asia/Seoul"
    });

    console.log('⏰ [CRON] 주간 리포트 스케줄 등록 완료 (매주 월요일 09:00 KST)');
}

/**
 * 매월 1일 오전 10시 - 월간 요약
 */
function scheduleMonthlyReport() {
    cron.schedule('0 10 1 * *', async () => {
        console.log('📅 [CRON] 월간 요약 생성 시작...', new Date().toISOString());

        try {
            const users = await User.find({});
            let processedCount = 0;

            for (const user of users) {
                const now = new Date();
                const lastMonthStart = new Date(now.getFullYear(), now.getMonth() - 1, 1);
                const lastMonthEnd = new Date(now.getFullYear(), now.getMonth(), 0, 23, 59, 59);

                // 지난 달 영수증 조회
                const monthlyReceipts = await Receipt.find({
                    userId: user._id,
                    transactionDate: {
                        $gte: lastMonthStart,
                        $lte: lastMonthEnd
                    }
                });

                const monthlyTotal = monthlyReceipts.reduce((sum, r) => sum + r.totalAmount, 0);
                const monthlyBudget = user.monthlyBudget || 2000000;

                // 월간 요약 알림
                await Notification.create({
                    userId: user._id,
                    type: 'monthly_report',
                    title: '📅 월간 소비 요약',
                    message: `지난 달 총 ${monthlyTotal.toLocaleString()}원 지출 (예산: ${monthlyBudget.toLocaleString()}원)\n${monthlyReceipts.length}건의 영수증 등록`,
                    priority: 'medium',
                    isRead: false,
                    metadata: {
                        triggerType: 'monthly_report',
                        monthlyTotal,
                        monthlyBudget,
                        receiptCount: monthlyReceipts.length,
                        month: lastMonthStart.getMonth() + 1,
                        year: lastMonthStart.getFullYear()
                    },
                    createdAt: new Date(),
                    updatedAt: new Date()
                });

                processedCount++;
            }

            console.log(`✅ [CRON] 월간 요약 생성 완료 - 처리: ${processedCount}명`);
        } catch (error) {
            console.error('❌ [CRON] 월간 요약 생성 실패:', error);
        }
    }, {
        timezone: "Asia/Seoul"
    });

    console.log('⏰ [CRON] 월간 요약 스케줄 등록 완료 (매월 1일 10:00 KST)');
}

/**
 * 모든 스케줄 작업 시작
 */
function startScheduledJobs() {
    console.log('\n🚀 ========== Scheduled Jobs 시작 ==========');
    scheduleDailyAnalysis();
    scheduleWeeklyReport();
    scheduleMonthlyReport();
    console.log('============================================\n');
}

module.exports = {
    startScheduledJobs,
    scheduleDailyAnalysis,
    scheduleWeeklyReport,
    scheduleMonthlyReport
};

