const Receipt = require('../models/Receipt');
const Notification = require('../models/Notification');

/**
 * MongoDB Change Streams로 영수증 변경 감지
 */
function watchReceipts() {
    console.log('👀 [WATCHER] Receipt Change Stream 시작...');

    try {
        const changeStream = Receipt.watch();

        changeStream.on('change', async (change) => {
            try {
                // 새 영수증 추가 감지
                if (change.operationType === 'insert') {
                    const receipt = change.fullDocument;

                    console.log(`📝 [WATCHER] 새 영수증 감지: ${receipt._id} (${receipt.totalAmount.toLocaleString()}원)`);

                    // 1. 고액 지출 체크 (50만원 이상)
                    if (receipt.totalAmount >= 500000) {
                        await Notification.create({
                            userId: receipt.userId,
                            type: 'spending_alert',
                            title: '💸 고액 지출 발생!',
                            message: `${getCategoryName(receipt.category)}에서 ${receipt.totalAmount.toLocaleString()}원을 지출하셨습니다. 챗봇에게 절약 팁을 물어보세요!`,
                            category: receipt.category,
                            amount: receipt.totalAmount,
                            priority: 'high',
                            isRead: false,
                            metadata: {
                                receiptId: receipt._id,
                                triggerType: 'high_amount',
                                storeName: receipt.storeName,
                                chatbotSuggested: true
                            },
                            createdAt: new Date(),
                            updatedAt: new Date()
                        });

                        console.log(`⚠️ [WATCHER] 고액 지출 알림 생성: ${receipt.totalAmount.toLocaleString()}원`);
                    }

                    // 2. 월간 예산 초과 체크
                    const monthStart = new Date(receipt.transactionDate);
                    monthStart.setDate(1);
                    monthStart.setHours(0, 0, 0, 0);

                    const monthTotal = await Receipt.aggregate([
                        {
                            $match: {
                                userId: receipt.userId,
                                transactionDate: { $gte: monthStart }
                            }
                        },
                        {
                            $group: {
                                _id: null,
                                total: { $sum: '$totalAmount' }
                            }
                        }
                    ]);

                    const currentMonthTotal = monthTotal[0]?.total || 0;
                    const monthlyBudget = 2000000; // 기본 월 예산

                    if (currentMonthTotal > monthlyBudget) {
                        // 예산 초과 알림 (중복 방지: 오늘 이미 생성되었는지 체크)
                        const today = new Date();
                        today.setHours(0, 0, 0, 0);

                        const existingNotification = await Notification.findOne({
                            userId: receipt.userId,
                            'metadata.triggerType': 'monthly_budget_exceeded',
                            createdAt: { $gte: today }
                        });

                        if (!existingNotification) {
                            await Notification.create({
                                userId: receipt.userId,
                                type: 'budget_warning',
                                title: '⚠️ 월간 예산 초과!',
                                message: `이번 달 ${currentMonthTotal.toLocaleString()}원 지출 (예산: ${monthlyBudget.toLocaleString()}원 초과)`,
                                priority: 'high',
                                isRead: false,
                                metadata: {
                                    triggerType: 'monthly_budget_exceeded',
                                    monthTotal: currentMonthTotal,
                                    monthlyBudget,
                                    overAmount: currentMonthTotal - monthlyBudget
                                },
                                createdAt: new Date(),
                                updatedAt: new Date()
                            });

                            console.log(`⚠️ [WATCHER] 월간 예산 초과 알림 생성: ${currentMonthTotal.toLocaleString()}원`);
                        }
                    }

                    // 3. 카테고리별 예산 초과 체크
                    const categoryBudgets = {
                        food: 500000,
                        transport: 200000,
                        shopping: 400000,
                        entertainment: 300000,
                        utilities: 150000,
                        healthcare: 200000
                    };

                    const categoryBudget = categoryBudgets[receipt.category];

                    if (categoryBudget) {
                        const categoryTotal = await Receipt.aggregate([
                            {
                                $match: {
                                    userId: receipt.userId,
                                    category: receipt.category,
                                    transactionDate: { $gte: monthStart }
                                }
                            },
                            {
                                $group: {
                                    _id: null,
                                    total: { $sum: '$totalAmount' }
                                }
                            }
                        ]);

                        const currentCategoryTotal = categoryTotal[0]?.total || 0;

                        if (currentCategoryTotal > categoryBudget) {
                            // 카테고리 예산 초과 알림 (중복 방지)
                            const today = new Date();
                            today.setHours(0, 0, 0, 0);

                            const existingCategoryNotification = await Notification.findOne({
                                userId: receipt.userId,
                                category: receipt.category,
                                'metadata.triggerType': 'category_budget_exceeded',
                                createdAt: { $gte: today }
                            });

                            if (!existingCategoryNotification) {
                                await Notification.create({
                                    userId: receipt.userId,
                                    type: 'budget_warning',
                                    title: `⚠️ ${getCategoryName(receipt.category)} 예산 초과!`,
                                    message: `이번 달 ${getCategoryName(receipt.category)} ${currentCategoryTotal.toLocaleString()}원 지출 (예산: ${categoryBudget.toLocaleString()}원)`,
                                    category: receipt.category,
                                    priority: 'medium',
                                    isRead: false,
                                    metadata: {
                                        triggerType: 'category_budget_exceeded',
                                        categoryTotal: currentCategoryTotal,
                                        categoryBudget,
                                        overAmount: currentCategoryTotal - categoryBudget
                                    },
                                    createdAt: new Date(),
                                    updatedAt: new Date()
                                });

                                console.log(`⚠️ [WATCHER] ${getCategoryName(receipt.category)} 예산 초과 알림 생성`);
                            }
                        }
                    }

                    // 4. 하루 외식 3회 이상 체크
                    if (receipt.category === 'food') {
                        const today = new Date(receipt.transactionDate);
                        today.setHours(0, 0, 0, 0);
                        const todayEnd = new Date(today);
                        todayEnd.setHours(23, 59, 59, 999);

                        const todayFoodCount = await Receipt.countDocuments({
                            userId: receipt.userId,
                            category: 'food',
                            transactionDate: {
                                $gte: today,
                                $lte: todayEnd
                            }
                        });

                        if (todayFoodCount >= 3) {
                            const existingDiningNotification = await Notification.findOne({
                                userId: receipt.userId,
                                'metadata.triggerType': 'frequent_dining',
                                createdAt: { $gte: today }
                            });

                            if (!existingDiningNotification) {
                                await Notification.create({
                                    userId: receipt.userId,
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
                                    },
                                    createdAt: new Date(),
                                    updatedAt: new Date()
                                });

                                console.log(`🍽️ [WATCHER] 외식 과다 알림 생성: ${todayFoodCount}회`);
                            }
                        }
                    }

                    console.log(`✅ [WATCHER] 영수증 처리 완료: ${receipt._id}`);
                }

                // 영수증 수정 감지
                if (change.operationType === 'update') {
                    console.log(`📝 [WATCHER] 영수증 수정 감지: ${change.documentKey._id}`);
                }

                // 영수증 삭제 감지
                if (change.operationType === 'delete') {
                    console.log(`🗑️ [WATCHER] 영수증 삭제 감지: ${change.documentKey._id}`);
                }

            } catch (error) {
                console.error('❌ [WATCHER] 영수증 처리 중 오류:', error);
            }
        });

        changeStream.on('error', (error) => {
            console.error('❌ [WATCHER] Change Stream 오류:', error);
            // 재연결 시도
            setTimeout(() => {
                console.log('🔄 [WATCHER] Change Stream 재연결 시도...');
                watchReceipts();
            }, 5000);
        });

        console.log('✅ [WATCHER] Receipt Change Stream 활성화 완료');

    } catch (error) {
        console.error('❌ [WATCHER] Change Stream 시작 실패:', error);
        console.error('💡 MongoDB Replica Set이 필요합니다. 로컬 개발 시에는 비활성화할 수 있습니다.');
    }
}

/**
 * 카테고리명 변환
 */
function getCategoryName(category) {
    const names = {
        food: '식비',
        transport: '교통',
        shopping: '쇼핑',
        entertainment: '문화/여가',
        utilities: '공과금',
        healthcare: '의료/건강',
        others: '기타'
    };
    return names[category] || category;
}

module.exports = {
    watchReceipts
};
