// backend/src/middleware/auth.js
const { auth } = require('../config/firebase-admin');

/**
 * Firebase ID Token 검증 미들웨어
 * Authorization 헤더에서 토큰을 추출하고 검증
 */
const verifyFirebaseToken = async (req, res, next) => {
  try {
    // Authorization 헤더 확인
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({
        success: false,
        message: 'No token provided'
      });
    }

    // Bearer 토큰 추출
    const idToken = authHeader.split('Bearer ')[1];

    // Firebase에서 토큰 검증
    const decodedToken = await auth.verifyIdToken(idToken);

    // 검증된 사용자 정보를 req에 추가
    req.user = {
      uid: decodedToken.uid,
      email: decodedToken.email,
      name: decodedToken.name || decodedToken.email.split('@')[0],
      picture: decodedToken.picture || null,
      emailVerified: decodedToken.email_verified || false
    };

    console.log(`✅ 인증 성공: ${req.user.email} (${req.user.uid})`);

    next();
  } catch (error) {
    console.error('❌ 토큰 검증 실패:', error.message);

    if (error.code === 'auth/id-token-expired') {
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
 * Optional: 토큰 검증 (선택적 인증)
 * 토큰이 있으면 검증, 없어도 통과
 */
const optionalAuth = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;

    if (authHeader && authHeader.startsWith('Bearer ')) {
      const idToken = authHeader.split('Bearer ')[1];
      const decodedToken = await auth.verifyIdToken(idToken);

      req.user = {
        uid: decodedToken.uid,
        email: decodedToken.email,
        name: decodedToken.name || decodedToken.email.split('@')[0],
        picture: decodedToken.picture || null
      };
    }

    next();
  } catch (error) {
    // 토큰이 유효하지 않아도 통과
    next();
  }
};

module.exports = {
  verifyFirebaseToken,
  optionalAuth
};