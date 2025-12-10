/**
 * Cron 작업 테스트 스크립트
 *
 * 사용법:
 * node backend/scripts/test_cron.js [작업명]
 *
 * 작업명:
 * - daily: 일일 소비 분석
 * - weekly: 주간 리포트
 * - monthly: 월간 요약
 * - all: 모든 작업 실행
 */

require('dotenv').config();
const mongoose = require('mongoose');
const User = require('../src/models/User');
const Receipt = require('../src/models/Receipt');
const Notification = require('../src/models/Notification');

// MongoDB 연결
async function connectDB() {
    try {
        await mongoose.connect(process.env.MONGODB_URI);
        console.log('✅ MongoDB 연결 성공');
    } catch (error) {
        console.error('❌ MongoDB 연결 실패:', error);
        process.exit(1);
    }
}

// 일일 소비 분석 (scheduledJobs.js의 로직 복사)
async function testDailyAnalysis() {
    console.log('\n📊 [TEST] 일일 소비 분석 시작...', new Date().toISOString());

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
            const dailyBudget = user.dailyBudget || 100000;

            console.log(`  👤 ${user.email}: 오늘 ${todayTotal.toLocaleString()}원 (예산: ${dailyBudget.toLocaleString()}원)`);

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
                console.log(`    ⚠️  예산 초과 알림 생성`);
            }

            // 월간 예산 근접 체크
            const monthStart = new Date(today.getFullYear(), today.getMonth(), 1);
            const monthReceipts = await Receipt.find({
                userId: user._id,
                transactionDate: { $gte: monthStart }
            });

            const monthTotal = monthReceipts.reduce((sum, r) => sum + r.totalAmount, 0);
            const monthlyBudget = user.monthlyBudget || 2000000;

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
                console.log(`    📊 월간 예산 90% 도달 알림 생성`);
            }

            processedCount++;
        }

        console.log(`✅ [TEST] 일일 소비 분석 완료 - 처리: ${processedCount}명, 알림 생성: ${notificationCount}개\n`);
    } catch (error) {
        console.error('❌ [TEST] 일일 소비 분석 실패:', error);
    }
}

// 주간 리포트
async function testWeeklyReport() {
    console.log('\n📈 [TEST] 주간 리포트 생성 시작...', new Date().toISOString());

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

            console.log(`  👤 ${user.email}: 지난 주 ${weeklyTotal.toLocaleString()}원 (${weeklyReceipts.length}건), 최다: ${topCategoryName}`);

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

        console.log(`✅ [TEST] 주간 리포트 생성 완료 - 처리: ${processedCount}명\n`);
    } catch (error) {
        console.error('❌ [TEST] 주간 리포트 생성 실패:', error);
    }
}

// 월간 요약
async function testMonthlyReport() {
    console.log('\n📅 [TEST] 월간 요약 생성 시작...', new Date().toISOString());

    try {
        const users = await User.find({});
        let processedCount = 0;

        for (const user of users) {
            const now = new Date();
            const lastMonthStart = new Date(now.getFullYear(), now.getMonth() - 1, 1);
            const lastMonthEnd = new Date(now.getFullYear(), now.getMonth(), 0, 23, 59, 59);

            const monthlyReceipts = await Receipt.find({
                userId: user._id,
                transactionDate: {
                    $gte: lastMonthStart,
                    $lte: lastMonthEnd
                }
            });

            const monthlyTotal = monthlyReceipts.reduce((sum, r) => sum + r.totalAmount, 0);
            const monthlyBudget = user.monthlyBudget || 2000000;

            console.log(`  👤 ${user.email}: 지난 달 ${monthlyTotal.toLocaleString()}원 (${monthlyReceipts.length}건)`);

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

        console.log(`✅ [TEST] 월간 요약 생성 완료 - 처리: ${processedCount}명\n`);
    } catch (error) {
        console.error('❌ [TEST] 월간 요약 생성 실패:', error);
    }
}

// 메인 함수
async function main() {
    const testType = process.argv[2] || 'all';

    await connectDB();

    console.log('🧪 ========== Cron 작업 테스트 ==========');
    console.log(`테스트 유형: ${testType}`);
    console.log('==========================================');

    try {
        switch (testType) {
            case 'daily':
                await testDailyAnalysis();
                break;
            case 'weekly':
                await testWeeklyReport();
                break;
            case 'monthly':
                await testMonthlyReport();
                break;
            case 'all':
                await testDailyAnalysis();
                await testWeeklyReport();
                await testMonthlyReport();
                break;
            default:
                console.log('❌ 알 수 없는 테스트 유형:', testType);
                console.log('사용 가능한 옵션: daily, weekly, monthly, all');
        }
    } catch (error) {
        console.error('❌ 테스트 실행 중 오류:', error);
    } finally {
        await mongoose.connection.close();
        console.log('\n✅ MongoDB 연결 종료');
        process.exit(0);
    }
}

main();
