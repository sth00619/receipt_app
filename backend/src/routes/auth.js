// backend/src/routes/auth.js
const express = require('express');
const router = express.Router();
const jwt = require('jsonwebtoken');
const User = require('../models/User');

// JWT 시크릿 키 (환경변수로 관리하는 것이 좋음)
const JWT_SECRET = process.env.JWT_SECRET || 'receiptify_secret_key_change_in_production';

/**
 * POST /api/auth/register
 * 일반 회원가입
 */
router.post('/register', async (req, res) => {
  try {
    const { email, password, displayName } = req.body;

    // 입력 검증
    if (!email || !password) {
      return res.status(400).json({
        success: false,
        message: 'Email and password are required'
      });
    }

    // 비밀번호 길이 검증
    if (password.length < 6) {
      return res.status(400).json({
        success: false,
        message: 'Password must be at least 6 characters'
      });
    }

    // 이메일 중복 확인
    const existingUser = await User.findOne({ email });
    if (existingUser) {
      return res.status(400).json({
        success: false,
        message: 'Email already exists'
      });
    }

    // 사용자 생성
    const user = new User({
      email,
      password,  // pre-save hook에서 자동으로 해싱됨
      displayName: displayName || email.split('@')[0],
      provider: 'email',
      preferences: {
        notifications: true,
        darkMode: false,
        language: 'ko'
      },
      stats: {
        totalReceipts: 0,
        totalTransactions: 0,
        totalSpending: 0
      }
    });

    await user.save();

    // JWT 토큰 생성
    const token = jwt.sign(
      {
        userId: user._id.toString(),
        email: user.email
      },
      JWT_SECRET,
      { expiresIn: '7d' }
    );

    // 비밀번호 제외하고 응답
    const userResponse = user.toObject();
    delete userResponse.password;

    res.status(201).json({
      success: true,
      message: 'User registered successfully',
      token,
      data: userResponse
    });

  } catch (error) {
    console.error('Registration error:', error);
    res.status(500).json({
      success: false,
      message: 'Error registering user',
      error: error.message
    });
  }
});

/**
 * POST /api/auth/login
 * 일반 로그인
 */
router.post('/login', async (req, res) => {
  try {
    const { email, password } = req.body;

    // 입력 검증
    if (!email || !password) {
      return res.status(400).json({
        success: false,
        message: 'Email and password are required'
      });
    }

    // 사용자 찾기
    const user = await User.findOne({ email });
    if (!user) {
      return res.status(401).json({
        success: false,
        message: 'Invalid email or password'
      });
    }

    // 이메일 로그인 사용자가 아닌 경우
    if (user.provider !== 'email') {
      return res.status(400).json({
        success: false,
        message: `This account uses ${user.provider} login. Please use ${user.provider} to sign in.`
      });
    }

    // 비밀번호 검증
    const isPasswordValid = await user.comparePassword(password);
    if (!isPasswordValid) {
      return res.status(401).json({
        success: false,
        message: 'Invalid email or password'
      });
    }

    // 마지막 로그인 시간 업데이트
    user.lastLoginAt = new Date();
    await user.save();

    // JWT 토큰 생성
    const token = jwt.sign(
      {
        userId: user._id.toString(),
        email: user.email
      },
      JWT_SECRET,
      { expiresIn: '7d' }
    );

    // 비밀번호 제외하고 응답
    const userResponse = user.toObject();
    delete userResponse.password;

    res.json({
      success: true,
      message: 'Login successful',
      token,
      data: userResponse
    });

  } catch (error) {
    console.error('Login error:', error);
    res.status(500).json({
      success: false,
      message: 'Error logging in',
      error: error.message
    });
  }
});

/**
 * POST /api/auth/verify
 * 토큰 검증
 */
router.post('/verify', async (req, res) => {
  try {
    const { token } = req.body;

    if (!token) {
      return res.status(400).json({
        success: false,
        message: 'Token is required'
      });
    }

    // 토큰 검증
    const decoded = jwt.verify(token, JWT_SECRET);

    // 사용자 조회
    const user = await User.findById(decoded.userId).select('-password');

    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }

    res.json({
      success: true,
      data: user
    });

  } catch (error) {
    if (error.name === 'JsonWebTokenError') {
      return res.status(401).json({
        success: false,
        message: 'Invalid token'
      });
    }

    if (error.name === 'TokenExpiredError') {
      return res.status(401).json({
        success: false,
        message: 'Token expired'
      });
    }

    res.status(500).json({
      success: false,
      message: 'Error verifying token',
      error: error.message
    });
  }
});

/**
 * POST /api/auth/naver
 * 네이버 로그인
 */
router.post('/naver', async (req, res) => {
  try {
    const { accessToken, email, name } = req.body;

    if (!accessToken || !email) {
      return res.status(400).json({
        success: false,
        message: 'Access token and email are required'
      });
    }

    // TODO: accessToken으로 네이버 서버에서 사용자 정보 검증 (보안 강화)

    // 사용자 찾기 또는 생성
    let user = await User.findOne({ email });

    if (!user) {
      // 신규 사용자 생성
      user = new User({
        email,
        displayName: name || email.split('@')[0],
        provider: 'naver',
        password: Math.random().toString(36).slice(-8), // 임시 비밀번호
        stats: {
          totalReceipts: 0,
          totalTransactions: 0,
          totalSpending: 0
        }
      });
      await user.save();
    } else {
      // 기존 사용자 정보 업데이트 (필요 시)
      if (name && !user.displayName) {
        user.displayName = name;
        await user.save();
      }
    }

    // JWT 토큰 생성
    const token = jwt.sign(
      {
        userId: user._id.toString(),
        email: user.email
      },
      JWT_SECRET,
      { expiresIn: '7d' }
    );

    res.json({
      success: true,
      message: 'Naver login successful',
      token,
      data: {
        id: user._id,
        email: user.email,
        displayName: user.displayName
      }
    });

  } catch (error) {
    console.error('Naver login error:', error);
    res.status(500).json({
      success: false,
      message: 'Error logging in with Naver',
      error: error.message
    });
  }
});

/**
 * POST /api/auth/google
 * 구글 로그인
 */
router.post('/google', async (req, res) => {
  try {
    const { idToken, email, name, photoUrl } = req.body;

    if (!idToken || !email) {
      return res.status(400).json({
        success: false,
        message: 'ID token and email are required'
      });
    }

    // TODO: idToken 검증 (보안 강화)

    // 사용자 찾기 또는 생성
    let user = await User.findOne({ email });

    if (!user) {
      // 신규 사용자 생성
      user = new User({
        email,
        displayName: name || email.split('@')[0],
        provider: 'google',
        password: Math.random().toString(36).slice(-8), // 임시 비밀번호
        stats: {
          totalReceipts: 0,
          totalTransactions: 0,
          totalSpending: 0
        }
      });
      await user.save();
    }

    // JWT 토큰 생성
    const token = jwt.sign(
      {
        userId: user._id.toString(),
        email: user.email
      },
      JWT_SECRET,
      { expiresIn: '7d' }
    );

    res.json({
      success: true,
      message: 'Google login successful',
      token,
      data: {
        id: user._id,
        email: user.email,
        displayName: user.displayName
      }
    });

  } catch (error) {
    console.error('Google login error:', error);
    res.status(500).json({
      success: false,
      message: 'Error logging in with Google',
      error: error.message
    });
  }
});

module.exports = router;