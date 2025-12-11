const express = require('express');
const router = express.Router();
const Receipt = require('../models/Receipt');
const Notification = require('../models/Notification');
const ChatMessage = require('../models/ChatMessage');
const ChatSession = require('../models/ChatSession');
const { verifyAuth } = require('../middleware/auth');
const QueryParser = require('../services/QueryParser');

// 모든 라우트에 인증 미들웨어 적용
router.use(verifyAuth);

// ============ 세션 관리 API ============

/**
 * GET /api/chatbot/sessions
 * 사용자의 모든 대화 세션 목록 조회
 */
router.get('/sessions', async (req, res) => {
    try {
        const userId = req.user.userId;
        const { limit = 20, skip = 0 } = req.query;

        console.log(`📋 세션 목록 조회 - 사용자: ${userId}`);

        const sessions = await ChatSession.find({ userId, isActive: true })
            .sort({ updatedAt: -1 })
            .limit(parseInt(limit))
            .skip(parseInt(skip));

        const total = await ChatSession.countDocuments({ userId, isActive: true });

        console.log(`✅ ${sessions.length}개 세션 조회 완료 (전체: ${total}개)`);

        res.json({
            success: true,
            data: {
                sessions,
                total,
                hasMore: total > (parseInt(skip) + sessions.length)
            }
        });

    } catch (error) {
        console.error('❌ 세션 목록 조회 실패:', error);
        res.status(500).json({
            success: false,
            message: '세션 목록 조회에 실패했습니다',
            error: error.message
        });
    }
});

/**
 * POST /api/chatbot/sessions
 * 새 대화 세션 생성
 */
router.post('/sessions', async (req, res) => {
    try {
        const userId = req.user.userId;
        const { title } = req.body;

        console.log(`🆕 새 세션 생성 - 사용자: ${userId}`);

        const session = await ChatSession.create({
            userId,
            title: title || 'New Conversation',
            lastMessage: '',
            messageCount: 0
        });

        console.log(`✅ 세션 생성 완료: ${session._id}`);

        res.json({
            success: true,
            data: { session }
        });

    } catch (error) {
        console.error('❌ 세션 생성 실패:', error);
        res.status(500).json({
            success: false,
            message: '세션 생성에 실패했습니다',
            error: error.message
        });
    }
});

/**
 * GET /api/chatbot/sessions/:sessionId
 * 특정 세션의 메시지 조회
 */
router.get('/sessions/:sessionId', async (req, res) => {
    try {
        const userId = req.user.userId;
        const { sessionId } = req.params;
        const { limit = 100, skip = 0 } = req.query;

        console.log(`📋 세션 메시지 조회 - 세션: ${sessionId}`);

        // 세션 확인
        const session = await ChatSession.findOne({ _id: sessionId, userId });
        if (!session) {
            return res.status(404).json({
                success: false,
                message: '세션을 찾을 수 없습니다'
            });
        }

        const messages = await ChatMessage.find({ sessionId, userId })
            .sort({ createdAt: 1 })
            .limit(parseInt(limit))
            .skip(parseInt(skip));

        const total = await ChatMessage.countDocuments({ sessionId, userId });

        console.log(`✅ ${messages.length}개 메시지 조회 완료`);

        res.json({
            success: true,
            data: {
                session,
                messages,
                total,
                hasMore: total > (parseInt(skip) + messages.length)
            }
        });

    } catch (error) {
        console.error('❌ 세션 메시지 조회 실패:', error);
        res.status(500).json({
            success: false,
            message: '메시지 조회에 실패했습니다',
            error: error.message
        });
    }
});

/**
 * PUT /api/chatbot/sessions/:sessionId
 * 세션 정보 업데이트 (제목 변경)
 */
router.put('/sessions/:sessionId', async (req, res) => {
    try {
        const userId = req.user.userId;
        const { sessionId } = req.params;
        const { title } = req.body;

        console.log(`✏️ 세션 업데이트 - 세션: ${sessionId}`);

        const session = await ChatSession.findOneAndUpdate(
            { _id: sessionId, userId },
            { title },
            { new: true }
        );

        if (!session) {
            return res.status(404).json({
                success: false,
                message: '세션을 찾을 수 없습니다'
            });
        }

        console.log(`✅ 세션 업데이트 완료`);

        res.json({
            success: true,
            data: { session }
        });

    } catch (error) {
        console.error('❌ 세션 업데이트 실패:', error);
        res.status(500).json({
            success: false,
            message: '세션 업데이트에 실패했습니다',
            error: error.message
        });
    }
});

/**
 * DELETE /api/chatbot/sessions/:sessionId
 * 세션 삭제 (메시지 포함)
 */
router.delete('/sessions/:sessionId', async (req, res) => {
    try {
        const userId = req.user.userId;
        const { sessionId } = req.params;

        console.log(`🗑️ 세션 삭제 - 세션: ${sessionId}`);

        // 세션 확인
        const session = await ChatSession.findOne({ _id: sessionId, userId });
        if (!session) {
            return res.status(404).json({
                success: false,
                message: '세션을 찾을 수 없습니다'
            });
        }

        // 메시지 삭제
        await ChatMessage.deleteMany({ sessionId });

        // 세션 삭제
        await ChatSession.deleteOne({ _id: sessionId });

        console.log(`✅ 세션 및 메시지 삭제 완료`);

        res.json({
            success: true,
            message: '세션이 삭제되었습니다'
        });

    } catch (error) {
        console.error('❌ 세션 삭제 실패:', error);
        res.status(500).json({
            success: false,
            message: '세션 삭제에 실패했습니다',
            error: error.message
        });
    }
});

// ============ 메시지 API ============

/**
 * POST /api/chatbot/sessions/:sessionId/message
 * 특정 세션에 메시지 전송
 */
router.post('/sessions/:sessionId/message', async (req, res) => {
    try {
        const userId = req.user.userId;
        const { sessionId } = req.params;
        const { message } = req.body;

        console.log(`💬 메시지 수신 - 세션: ${sessionId}`);
        console.log(`📝 메시지: ${message}`);

        if (!message) {
            return res.status(400).json({
                success: false,
                message: '메시지를 입력해주세요'
            });
        }

        // 세션 확인
        let session = await ChatSession.findOne({ _id: sessionId, userId });
        if (!session) {
            return res.status(404).json({
                success: false,
                message: '세션을 찾을 수 없습니다'
            });
        }

        // 사용자 메시지 저장
        const userMessage = await ChatMessage.create({
            userId,
            sessionId,
            role: 'user',
            message,
            metadata: {}
        });

        // 쿼리 파싱
        const parsedQuery = QueryParser.parseQuery(message);
        console.log('🔍 파싱된 쿼리:', JSON.stringify(parsedQuery, null, 2));

        // 통계 조회
        const stats = await getSpendingStats(userId, parsedQuery);

        // 챗봇 응답 생성
        const response = generateSmartResponse(message, parsedQuery, stats);

        // 봇 응답 저장
        const botMessage = await ChatMessage.create({
            userId,
            sessionId,
            role: 'bot',
            message: response,
            metadata: {
                parsedQuery,
                stats,
                userMessageId: userMessage._id
            }
        });

        // 세션 업데이트
        const truncatedMessage = message.length > 50 ? message.substring(0, 50) + '...' : message;
        await ChatSession.findByIdAndUpdate(sessionId, {
            lastMessage: truncatedMessage,
            messageCount: session.messageCount + 2,
            // 첫 메시지면 제목 자동 설정
            ...(session.messageCount === 0 && { title: truncatedMessage })
        });

        console.log(`✅ 챗봇 응답 완료`);

        res.json({
            success: true,
            data: {
                userMessage,
                botMessage,
                response,
                stats,
                parsedQuery
            }
        });

    } catch (error) {
        console.error('❌ 메시지 처리 실패:', error);
        res.status(500).json({
            success: false,
            message: '메시지 처리에 실패했습니다',
            error: error.message
        });
    }
});

/**
 * POST /api/chatbot/message
 * 레거시 API - 새 세션 자동 생성
 */
router.post('/message', async (req, res) => {
    try {
        const userId = req.user.userId;
        const { message, sessionId } = req.body;

        console.log(`💬 챗봇 메시지 수신 - 사용자: ${userId}`);
        console.log(`📝 메시지: ${message}`);

        if (!message) {
            return res.status(400).json({
                success: false,
                message: '메시지를 입력해주세요'
            });
        }

        // 세션 ID가 없으면 새 세션 생성
        let session;
        if (sessionId) {
            session = await ChatSession.findOne({ _id: sessionId, userId });
        }

        if (!session) {
            session = await ChatSession.create({
                userId,
                title: message.length > 50 ? message.substring(0, 50) + '...' : message,
                lastMessage: '',
                messageCount: 0
            });
            console.log(`🆕 새 세션 자동 생성: ${session._id}`);
        }

        // 사용자 메시지 저장
        const userMessage = await ChatMessage.create({
            userId,
            sessionId: session._id,
            role: 'user',
            message,
            metadata: {}
        });

        // 쿼리 파싱
        const parsedQuery = QueryParser.parseQuery(message);
        console.log('🔍 파싱된 쿼리:', JSON.stringify(parsedQuery, null, 2));

        // 통계 조회
        const stats = await getSpendingStats(userId, parsedQuery);

        // 챗봇 응답 생성
        const response = generateSmartResponse(message, parsedQuery, stats);

        // 봇 응답 저장
        const botMessage = await ChatMessage.create({
            userId,
            sessionId: session._id,
            role: 'bot',
            message: response,
            metadata: {
                parsedQuery,
                stats,
                userMessageId: userMessage._id
            }
        });

        // 세션 업데이트
        await ChatSession.findByIdAndUpdate(session._id, {
            lastMessage: message.length > 50 ? message.substring(0, 50) + '...' : message,
            messageCount: session.messageCount + 2
        });

        console.log(`✅ 챗봇 응답: ${response.substring(0, 100)}...`);

        res.json({
            success: true,
            data: {
                response,
                stats,
                messageId: botMessage._id,
                sessionId: session._id
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
 * GET /api/chatbot/messages
 * 레거시 API - 최근 메시지 조회
 */
router.get('/messages', async (req, res) => {
    try {
        const userId = req.user.userId;
        const { limit = 50, skip = 0 } = req.query;

        console.log(`📋 대화 내역 조회 - 사용자: ${userId}`);

        const messages = await ChatMessage.find({ userId })
            .sort({ createdAt: -1 })
            .limit(parseInt(limit))
            .skip(parseInt(skip));

        const total = await ChatMessage.countDocuments({ userId });

        console.log(`✅ ${messages.length}개 메시지 조회 완료 (전체: ${total}개)`);

        res.json({
            success: true,
            data: {
                messages: messages.reverse(),
                total,
                hasMore: total > (parseInt(skip) + messages.length)
            }
        });

    } catch (error) {
        console.error('❌ 대화 내역 조회 실패:', error);
        res.status(500).json({
            success: false,
            message: '대화 내역 조회에 실패했습니다',
            error: error.message
        });
    }
});

// ============ 알림 기반 조언 API ============

/**
 * POST /api/chatbot/advice/:notificationId
 * 특정 알림에 대한 상세 조언
 */
router.post('/advice/:notificationId', async (req, res) => {
    try {
        const userId = req.user.userId;
        const { notificationId } = req.params;

        console.log(`💬 알림 기반 조언 요청 - 알림 ID: ${notificationId}`);

        const notification = await Notification.findOne({ _id: notificationId, userId });

        if (!notification) {
            return res.status(404).json({
                success: false,
                message: '알림을 찾을 수 없습니다'
            });
        }

        const now = new Date();
        const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
        const endOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0);
        const stats = await getMonthlyStats(userId, startOfMonth, endOfMonth);

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

// ============ 통계 조회 함수 ============

/**
 * 파싱된 쿼리 기반 통계 조회
 */
async function getSpendingStats(userId, parsedQuery) {
    const { categories, dateRange } = parsedQuery;

    // Date 객체로 변환 (문자열일 경우 대비)
    const startDate = new Date(dateRange.startDate);
    const endDate = new Date(dateRange.endDate);

    console.log(`📊 통계 조회 시작`);
    console.log(`   👤 사용자: ${userId}`);
    console.log(`   📅 시작일: ${startDate.toISOString()}`);
    console.log(`   📅 종료일: ${endDate.toISOString()}`);
    console.log(`   📂 카테고리: ${categories.length > 0 ? categories.join(', ') : '전체'}`);

    const matchQuery = {
        userId: userId,
        transactionDate: {
            $gte: startDate,
            $lte: endDate
        }
    };

    // 카테고리 필터
    if (categories.length > 0) {
        matchQuery.category = { $in: categories };
    }

    console.log(`   🔍 쿼리:`, JSON.stringify(matchQuery, null, 2));

    // 카테고리별 통계
    const categoryStats = await Receipt.aggregate([
        { $match: matchQuery },
        {
            $group: {
                _id: '$category',
                totalAmount: { $sum: '$totalAmount' },
                count: { $sum: 1 },
                avgAmount: { $avg: '$totalAmount' }
            }
        },
        { $sort: { totalAmount: -1 } }
    ]);

    console.log(`   📈 카테고리별 결과: ${categoryStats.length}개`);

    // 전체 합계
    const totalStats = await Receipt.aggregate([
        { $match: matchQuery },
        {
            $group: {
                _id: null,
                totalAmount: { $sum: '$totalAmount' },
                count: { $sum: 1 }
            }
        }
    ]);

    console.log(`   💰 총액: ${totalStats[0]?.totalAmount || 0}, 건수: ${totalStats[0]?.count || 0}`);

    // 최근 거래 내역
    const recentTransactions = await Receipt.find(matchQuery)
        .sort({ transactionDate: -1 })
        .limit(5)
        .select('storeName totalAmount category transactionDate');

    const statsMap = {};
    categoryStats.forEach(stat => {
        statsMap[stat._id] = {
            totalAmount: stat.totalAmount,
            count: stat.count,
            avgAmount: Math.round(stat.avgAmount)
        };
    });

    return {
        byCategory: statsMap,
        categoryList: categoryStats,
        total: totalStats[0] || { totalAmount: 0, count: 0 },
        recentTransactions,
        dateRange: {
            start: startDate,
            end: endDate,
            type: dateRange.type
        }
    };
}

/**
 * 월간 통계 조회 (레거시)
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

// ============ 응답 생성 함수 ============

/**
 * 스마트 응답 생성 (파싱 기반)
 */
function generateSmartResponse(message, parsedQuery, stats) {
    const { categories, dateRange, isKorean, intent } = parsedQuery;
    const lowerMessage = message.toLowerCase().trim();

    // 인사 처리
    if (lowerMessage.match(/안녕|하이|hello|hi|hey/)) {
        return isKorean
            ? '안녕하세요! 😊 소비 도우미입니다.\n\n무엇이 궁금하신가요? 예를 들어:\n• "이번 주 식비 얼마야?"\n• "지난달 총 지출"\n• "교통비 분석해줘"'
            : 'Hello! 😊 I\'m your spending assistant.\n\nWhat would you like to know? For example:\n• "What\'s my food spending this week?"\n• "Total expenses last month"\n• "Analyze my transport costs"';
    }

    // 도움말 요청
    if (lowerMessage.match(/help|도움|사용법|뭘 물어|how to/)) {
        return generateHelpResponse(isKorean);
    }

    // 절약 팁 요청
    if (lowerMessage.match(/절약|아끼|팁|방법|줄이|줄일|save|saving|tip/)) {
        return generateSavingTips(stats, isKorean);
    }

    // 카테고리가 지정된 경우
    if (categories.length > 0) {
        return generateCategoryResponse(categories, dateRange, stats, isKorean);
    }

    // 총 지출 문의
    if (lowerMessage.match(/총|전체|얼마|지출|다|total|all|spending|spent|how much/)) {
        return generateTotalResponse(dateRange, stats, isKorean);
    }

    // 분석 요청
    if (lowerMessage.match(/분석|어때|상태|현황|리포트|analyze|analysis|report|status/)) {
        return generateAnalysisResponse(stats, isKorean);
    }

    // 기본 응답
    return generateDefaultResponse(isKorean);
}

/**
 * 카테고리별 응답 생성
 */
function generateCategoryResponse(categories, dateRange, stats, isKorean) {
    const dateDesc = QueryParser.getDateRangeDescription(dateRange, isKorean);

    if (categories.length === 1) {
        const category = categories[0];
        const categoryName = QueryParser.getCategoryDisplayName(category, isKorean);
        const categoryStats = stats.byCategory[category];

        if (!categoryStats || categoryStats.totalAmount === 0) {
            return isKorean
                ? `📊 ${dateDesc} ${categoryName} 지출 내역이 없습니다.`
                : `📊 No ${categoryName.toLowerCase()} expenses found for ${dateDesc}.`;
        }

        const amount = categoryStats.totalAmount.toLocaleString();
        const count = categoryStats.count;
        const avg = categoryStats.avgAmount?.toLocaleString() || '0';
        const advice = getCategoryAdvice(category, categoryStats.totalAmount, isKorean);

        if (isKorean) {
            return `📊 ${dateDesc} ${categoryName} 지출 현황\n\n` +
                   `💰 총 지출: ${amount}원\n` +
                   `📝 거래 횟수: ${count}건\n` +
                   `📈 평균 지출: ${avg}원/건\n\n` +
                   `${advice}`;
        } else {
            return `📊 ${categoryName} Spending for ${dateDesc}\n\n` +
                   `💰 Total: ₩${amount}\n` +
                   `📝 Transactions: ${count}\n` +
                   `📈 Average: ₩${avg}/transaction\n\n` +
                   `${advice}`;
        }
    } else {
        // 여러 카테고리
        let response = isKorean
            ? `📊 ${dateDesc} 카테고리별 지출 현황\n\n`
            : `📊 Spending by Category for ${dateDesc}\n\n`;

        let hasData = false;
        for (const category of categories) {
            const categoryName = QueryParser.getCategoryDisplayName(category, isKorean);
            const categoryStats = stats.byCategory[category];

            if (categoryStats && categoryStats.totalAmount > 0) {
                hasData = true;
                const amount = categoryStats.totalAmount.toLocaleString();
                response += isKorean
                    ? `• ${categoryName}: ${amount}원 (${categoryStats.count}건)\n`
                    : `• ${categoryName}: ₩${amount} (${categoryStats.count} transactions)\n`;
            }
        }

        if (!hasData) {
            return isKorean
                ? `📊 ${dateDesc} 해당 카테고리들의 지출 내역이 없습니다.`
                : `📊 No expenses found for the selected categories during ${dateDesc}.`;
        }

        return response;
    }
}

/**
 * 총 지출 응답 생성
 */
function generateTotalResponse(dateRange, stats, isKorean) {
    const dateDesc = QueryParser.getDateRangeDescription(dateRange, isKorean);
    const total = stats.total.totalAmount;
    const count = stats.total.count;

    if (total === 0) {
        return isKorean
            ? `📊 ${dateDesc} 지출 내역이 없습니다.`
            : `📊 No expenses found for ${dateDesc}.`;
    }

    let response = isKorean
        ? `📊 ${dateDesc} 총 지출 현황\n\n💰 총 지출: ${total.toLocaleString()}원\n📝 거래 횟수: ${count}건\n\n`
        : `📊 Total Spending for ${dateDesc}\n\n💰 Total: ₩${total.toLocaleString()}\n📝 Transactions: ${count}\n\n`;

    // 카테고리별 요약
    if (stats.categoryList && stats.categoryList.length > 0) {
        response += isKorean ? '📈 카테고리별 요약:\n' : '📈 By Category:\n';

        for (const cat of stats.categoryList.slice(0, 5)) {
            const categoryName = QueryParser.getCategoryDisplayName(cat._id, isKorean);
            const percentage = Math.round((cat.totalAmount / total) * 100);
            response += isKorean
                ? `• ${categoryName}: ${cat.totalAmount.toLocaleString()}원 (${percentage}%)\n`
                : `• ${categoryName}: ₩${cat.totalAmount.toLocaleString()} (${percentage}%)\n`;
        }
    }

    return response;
}

/**
 * 분석 응답 생성
 */
function generateAnalysisResponse(stats, isKorean) {
    const total = stats.total.totalAmount;
    const categories = stats.byCategory;

    if (total === 0) {
        return isKorean
            ? '📊 분석할 지출 내역이 없습니다.'
            : '📊 No spending data to analyze.';
    }

    // 가장 많이 쓴 카테고리
    let maxCategory = null;
    let maxAmount = 0;

    for (const [category, data] of Object.entries(categories)) {
        if (data.totalAmount > maxAmount) {
            maxAmount = data.totalAmount;
            maxCategory = category;
        }
    }

    const maxCategoryName = maxCategory ? QueryParser.getCategoryDisplayName(maxCategory, isKorean) : '';
    const percentage = maxCategory ? Math.round((maxAmount / total) * 100) : 0;
    const nextMonthGoal = Math.round(total * 0.9);

    if (isKorean) {
        let analysis = `📊 소비 분석 리포트\n\n`;
        analysis += `💰 총 지출: ${total.toLocaleString()}원 (${stats.total.count}건)\n`;
        analysis += `📈 가장 많이 쓴 곳: ${maxCategoryName} (${maxAmount.toLocaleString()}원, ${percentage}%)\n\n`;
        analysis += `🎯 다음 달 목표: ${nextMonthGoal.toLocaleString()}원\n`;
        analysis += `(현재보다 10% 절약하기)`;
        return analysis;
    } else {
        let analysis = `📊 Spending Analysis Report\n\n`;
        analysis += `💰 Total: ₩${total.toLocaleString()} (${stats.total.count} transactions)\n`;
        analysis += `📈 Top Category: ${maxCategoryName} (₩${maxAmount.toLocaleString()}, ${percentage}%)\n\n`;
        analysis += `🎯 Next Month Goal: ₩${nextMonthGoal.toLocaleString()}\n`;
        analysis += `(Save 10% from current spending)`;
        return analysis;
    }
}

/**
 * 절약 팁 생성
 */
function generateSavingTips(stats, isKorean) {
    const categories = stats.byCategory;
    const tips = isKorean ? ['💡 맞춤 절약 팁을 알려드릴게요!\n'] : ['💡 Here are some personalized saving tips!\n'];

    if (categories.food && categories.food.totalAmount > 400000) {
        tips.push(isKorean
            ? '🍚 식비: 주말에 식재료를 미리 준비하고, 도시락을 싸가면 월 10만원 이상 절약 가능해요.'
            : '🍚 Food: Prep meals on weekends and pack lunch - you could save over ₩100,000/month.');
    }

    if (categories.transport && categories.transport.totalAmount > 150000) {
        tips.push(isKorean
            ? '🚇 교통: 정기권으로 바꾸면 30% 절약! 자전거나 도보도 고려해보세요.'
            : '🚇 Transport: Switch to a monthly pass to save 30%! Consider biking or walking.');
    }

    if (categories.shopping && categories.shopping.totalAmount > 300000) {
        tips.push(isKorean
            ? '🛍️ 쇼핑: 장바구니에 담고 24시간 뒤 재검토하세요. 충동구매를 50% 줄일 수 있어요.'
            : '🛍️ Shopping: Wait 24 hours before buying - this can reduce impulse purchases by 50%.');
    }

    if (categories.entertainment && categories.entertainment.totalAmount > 200000) {
        tips.push(isKorean
            ? '🎮 문화/여가: 구독 서비스를 점검하세요. 사용하지 않는 구독이 있나요?'
            : '🎮 Entertainment: Review your subscriptions - are there any you\'re not using?');
    }

    if (tips.length === 1) {
        return isKorean
            ? '✅ 지출을 잘 관리하고 계시네요! 현재 패턴을 유지하세요.'
            : '✅ Great job managing your spending! Keep up the good work.';
    }

    return tips.join('\n\n');
}

/**
 * 카테고리별 조언 생성
 */
function getCategoryAdvice(category, amount, isKorean) {
    const advices = {
        food: {
            high: { ko: '⚠️ 식비가 많이 나왔네요. 외식을 줄이고 집에서 요리해보는 건 어떨까요?', en: '⚠️ High food expenses! Try cooking at home more often.' },
            medium: { ko: '💡 적정 수준이지만, 배달음식을 줄이면 더 절약할 수 있어요.', en: '💡 Moderate spending. Consider reducing delivery orders.' },
            low: { ko: '✅ 식비를 잘 관리하고 계시네요!', en: '✅ Great job managing food expenses!' }
        },
        transport: {
            high: { ko: '⚠️ 교통비가 많이 나왔네요. 정기권이나 월정액 서비스를 고려해보세요.', en: '⚠️ High transport costs! Consider a monthly pass.' },
            medium: { ko: '💡 대중교통 정기권을 이용하면 30% 정도 절약할 수 있어요.', en: '💡 A transit pass could save you about 30%.' },
            low: { ko: '✅ 교통비를 효율적으로 사용하고 계시네요!', en: '✅ Efficient transport spending!' }
        },
        shopping: {
            high: { ko: '⚠️ 쇼핑을 많이 하셨네요. 필요한 물건만 구매하도록 노력해보세요.', en: '⚠️ High shopping expenses! Try to buy only what you need.' },
            medium: { ko: '💡 구매 전 24시간 고민하는 습관을 들이면 충동구매를 줄일 수 있어요.', en: '💡 Wait 24 hours before purchases to reduce impulse buying.' },
            low: { ko: '✅ 계획적인 쇼핑을 하고 계시네요!', en: '✅ Great job with planned shopping!' }
        },
        healthcare: {
            high: { ko: '💊 의료비 지출이 많습니다. 정기 검진으로 큰 비용을 예방하세요.', en: '💊 High healthcare costs. Regular checkups can prevent bigger expenses.' },
            medium: { ko: '💡 건강 관리 잘 하고 계시네요.', en: '💡 Good job taking care of your health.' },
            low: { ko: '✅ 건강하게 지내고 계시네요!', en: '✅ Staying healthy!' }
        },
        entertainment: {
            high: { ko: '🎮 문화/여가 비용이 많습니다. 무료 활동도 찾아보세요!', en: '🎮 High entertainment costs! Look for free activities.' },
            medium: { ko: '💡 즐거운 시간 보내고 계시네요. 할인 혜택을 활용해보세요.', en: '💡 Having fun! Try to use discounts and deals.' },
            low: { ko: '✅ 적절한 여가 생활을 하고 계시네요!', en: '✅ Balanced leisure spending!' }
        },
        utilities: {
            high: { ko: '💡 공과금이 높네요. 에너지 절약 방법을 찾아보세요.', en: '💡 High utility bills! Look for ways to save energy.' },
            medium: { ko: '💡 평균 수준의 공과금입니다.', en: '💡 Average utility expenses.' },
            low: { ko: '✅ 공과금을 잘 관리하고 계시네요!', en: '✅ Great utility management!' }
        }
    };

    const thresholds = {
        food: { high: 500000, medium: 300000 },
        transport: { high: 200000, medium: 100000 },
        shopping: { high: 500000, medium: 200000 },
        healthcare: { high: 300000, medium: 100000 },
        entertainment: { high: 300000, medium: 100000 },
        utilities: { high: 300000, medium: 150000 }
    };

    const categoryAdvice = advices[category];
    const threshold = thresholds[category];

    if (!categoryAdvice || !threshold) {
        return isKorean ? '💡 지출을 꾸준히 모니터링하세요.' : '💡 Keep monitoring your expenses.';
    }

    let level = 'low';
    if (amount > threshold.high) level = 'high';
    else if (amount > threshold.medium) level = 'medium';

    return isKorean ? categoryAdvice[level].ko : categoryAdvice[level].en;
}

/**
 * 도움말 응답 생성
 */
function generateHelpResponse(isKorean) {
    if (isKorean) {
        return `📖 사용 가이드\n\n` +
               `저에게 이런 질문을 해보세요:\n\n` +
               `💰 지출 조회:\n` +
               `• "이번 달 총 지출 얼마야?"\n` +
               `• "지난주 식비"\n` +
               `• "오늘 쇼핑 지출"\n\n` +
               `📊 분석:\n` +
               `• "이번 달 소비 분석해줘"\n` +
               `• "카테고리별 지출 보여줘"\n\n` +
               `💡 조언:\n` +
               `• "절약 팁 알려줘"\n` +
               `• "교통비 줄이는 방법"\n\n` +
               `🗓️ 기간 지정:\n` +
               `• 오늘, 어제, 이번 주, 지난 주\n` +
               `• 이번 달, 지난 달, 올해`;
    } else {
        return `📖 How to Use\n\n` +
               `Try asking me questions like:\n\n` +
               `💰 Check Spending:\n` +
               `• "What's my total spending this month?"\n` +
               `• "Food expenses last week"\n` +
               `• "Shopping today"\n\n` +
               `📊 Analysis:\n` +
               `• "Analyze my spending"\n` +
               `• "Show spending by category"\n\n` +
               `💡 Advice:\n` +
               `• "Give me saving tips"\n` +
               `• "How to reduce transport costs"\n\n` +
               `🗓️ Time Periods:\n` +
               `• today, yesterday, this week, last week\n` +
               `• this month, last month, this year`;
    }
}

/**
 * 기본 응답 생성
 */
function generateDefaultResponse(isKorean) {
    if (isKorean) {
        return '죄송해요, 잘 이해하지 못했어요. 😅\n\n' +
               '다음과 같이 물어보세요:\n' +
               '• "총 지출 얼마야?"\n' +
               '• "이번 주 식비"\n' +
               '• "지난달 쇼핑 지출"\n' +
               '• "절약 팁 알려줘"\n\n' +
               '"도움말"을 입력하시면 더 자세한 사용법을 알려드릴게요!';
    } else {
        return 'Sorry, I didn\'t quite understand that. 😅\n\n' +
               'Try asking:\n' +
               '• "What\'s my total spending?"\n' +
               '• "Food expenses this week"\n' +
               '• "Shopping last month"\n' +
               '• "Give me saving tips"\n\n' +
               'Type "help" for more detailed usage guide!';
    }
}

/**
 * 알림 기반 조언 생성
 */
function generateAdviceForNotification(notification, stats) {
    const { type, category, amount, metadata } = notification;

    let advice = '';

    switch (metadata?.triggerType) {
        case 'high_amount':
            advice = `💸 ${amount.toLocaleString()}원의 고액 지출이 발생했습니다.\n\n`;
            advice += `📊 이번 달 ${QueryParser.getCategoryDisplayName(category, true)} 총 지출: ${stats.byCategory[category]?.totalAmount.toLocaleString() || 0}원\n\n`;
            advice += `💡 조언:\n`;
            advice += `• 이 지출이 계획된 것이었나요?\n`;
            advice += `• 같은 금액으로 할 수 있는 대안이 있었나요?\n`;
            advice += `• 다음엔 여러 업체를 비교해보세요\n`;
            advice += `• 할인이나 쿠폰을 활용하면 10-20% 절약 가능합니다`;
            break;

        case 'budget_exceeded':
            const overAmount = metadata.overAmount;
            advice = `⚠️ ${QueryParser.getCategoryDisplayName(category, true)} 예산을 ${overAmount.toLocaleString()}원 초과했습니다!\n\n`;
            advice += `📊 현재 상황:\n`;
            advice += `• 이번 달 지출: ${amount.toLocaleString()}원\n`;
            advice += `• 목표 예산: ${metadata.limit.toLocaleString()}원\n`;
            advice += `• 초과 금액: ${overAmount.toLocaleString()}원\n\n`;
            advice += `💡 남은 기간 절약 방법:\n`;
            advice += getCategorySavingTips(category);
            break;

        default:
            advice = `📊 ${notification.title}\n\n`;
            advice += `${notification.message}\n\n`;
            advice += `💡 일반 조언:\n`;
            advice += `• 지출 내역을 정기적으로 확인하세요\n`;
            advice += `• 예산을 설정하고 지키세요\n`;
            advice += `• 불필요한 구독 서비스를 정리하세요`;
    }

    return advice;
}

/**
 * 카테고리별 절약 팁
 */
function getCategorySavingTips(category) {
    const tips = {
        food: '• 외식 대신 집밥으로 전환\n• 도시락 준비하기\n• 커피는 집에서 만들어 가기',
        transport: '• 정기권으로 전환\n• 가까운 거리는 도보나 자전거\n• 카풀 서비스 이용',
        shopping: '• 필수품만 구매\n• 장바구니에 담고 24시간 후 재검토\n• 중고 거래 플랫폼 활용',
        entertainment: '• 무료 이벤트 찾아보기\n• 구독 서비스 정리\n• 할인 혜택 활용',
        utilities: '• 에너지 절약 습관\n• 불필요한 조명 끄기\n• 절전 가전제품 사용'
    };
    return tips[category] || '• 지출 내역 점검\n• 예산 재조정\n• 필수 지출만 유지';
}

module.exports = router;
