// backend/src/middleware/auth.js
const jwt = require('jsonwebtoken');
const { auth: firebaseAuth } = require('../config/firebase-admin');
const User = require('../models/User');

const JWT_SECRET = process.env.JWT_SECRET || 'receiptify_super_secret_key_change_this_in_production_12345';

/**
 * JWT 토큰 검증 미들웨어 (일반 로그인용)
 */
const verifyJWT = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;

    console.log('🔐 JWT 검증 시작');
    console.log('📍 요청:', req.method, req.path);

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      console.log('❌ Authorization 헤더 없음');
      return res.status(401).json({
        success: false,
        message: 'No token provided'
      });
    }

    const token = authHeader.split('Bearer ')[1];
    console.log('💳 토큰 앞부분:', token.substring(0, 30) + '...');

    // JWT 검증
    const decoded = jwt.verify(token, JWT_SECRET);
    console.log('✅ JWT 디코딩 성공:', decoded);

    // 사용자 조회
    const user = await User.findById(decoded.userId).select('-password');

    if (!user) {
      console.log('❌ 사용자를 찾을 수 없음:', decoded.userId);
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }

    // req.user에 사용자 정보 추가
    req.user = {
      userId: user._id.toString(),
      email: user.email,
      displayName: user.displayName
    };

    console.log(`✅ JWT 인증 성공: ${req.user.email}, userId: ${req.user.userId}`);

    next();
  } catch (error) {
    console.error('❌ JWT 검증 실패:', error.message);

    if (error.name === 'TokenExpiredError') {
      return res.status(401).json({
        success: false,
        message: 'Token expired',
        code: 'TOKEN_EXPIRED'
      });
    }

    return res.status(401).json({
      success: false,
      message: 'Invalid token',
      error: error.message
    });
  }
};

/**
 * Firebase 또는 JWT 토큰 검증 (둘 다 지원)
 */
const verifyAuth = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;

    console.log('🔐 인증 검증 시작');
    console.log('📍 요청:', req.method, req.path);
    console.log('📋 Authorization 헤더:', authHeader ? authHeader.substring(0, 50) + '...' : '없음');

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      console.log('❌ Authorization 헤더 없음 또는 형식 오류');
      return res.status(401).json({
        success: false,
        message: 'No token provided'
      });
    }

    const token = authHeader.split('Bearer ')[1];

    // 먼저 Firebase 토큰인지 확인
    try {
      const decodedToken = await firebaseAuth.verifyIdToken(token);

      req.user = {
        uid: decodedToken.uid,
        userId: decodedToken.uid,  // 호환성을 위해
        email: decodedToken.email,
        name: decodedToken.name || decodedToken.email.split('@')[0],
        picture: decodedToken.picture || null,
        provider: 'firebase'
      };

      console.log(`✅ Firebase 인증 성공: ${req.user.email}`);
      return next();

    } catch (firebaseError) {
      console.log('🔄 Firebase 토큰 아님, JWT 검증 시도...');

      // Firebase 토큰이 아니면 JWT 검증 시도
      try {
        const decoded = jwt.verify(token, JWT_SECRET);
        console.log('✅ JWT 디코딩 성공:', decoded);

        const user = await User.findById(decoded.userId).select('-password');

        if (!user) {
          console.log('❌ 사용자를 찾을 수 없음:', decoded.userId);
          return res.status(404).json({
            success: false,
            message: 'User not found'
          });
        }

        req.user = {
          uid: user._id.toString(),
          userId: user._id.toString(),
          email: user.email,
          displayName: user.displayName,
          provider: 'email'
        };

        console.log(`✅ JWT 인증 성공: ${req.user.email}, userId: ${req.user.userId}`);
        return next();

      } catch (jwtError) {
        console.error('❌ JWT 검증도 실패:', jwtError.message);
        throw jwtError;
      }
    }

  } catch (error) {
    console.error('❌ 인증 실패:', error.message);

    if (error.name === 'TokenExpiredError' || error.code === 'auth/id-token-expired') {
      return res.status(401).json({
        success: false,
        message: 'Token expired',
        code: 'TOKEN_EXPIRED'
      });
    }

    return res.status(401).json({
      success: false,
      message: 'Invalid token',
      error: error.message
    });
  }
};

module.exports = {
  verifyJWT,
  verifyAuth,
  verifyFirebaseToken: verifyAuth  // 하위 호환성
};