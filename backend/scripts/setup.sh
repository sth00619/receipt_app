#!/bin/bash
# backend/scripts/setup.sh
# MongoDB 설정 및 초기화 스크립트

echo "=========================================="
echo "Receiptify MongoDB Setup"
echo "=========================================="

# MongoDB 실행 확인
echo ""
echo "[1/3] Checking MongoDB..."
if ! command -v mongosh &> /dev/null; then
    echo "❌ MongoDB Shell (mongosh) is not installed"
    echo "Please install MongoDB from: https://www.mongodb.com/try/download/community"
    exit 1
fi

# MongoDB 연결 테스트
echo "Testing MongoDB connection..."
if ! mongosh --eval "db.version()" > /dev/null 2>&1; then
    echo "❌ Cannot connect to MongoDB"
    echo "Please start MongoDB service:"
    echo "  - macOS: brew services start mongodb-community"
    echo "  - Linux: sudo systemctl start mongod"
    echo "  - Windows: net start MongoDB"
    exit 1
fi

echo "✅ MongoDB is running"

# 데이터베이스 초기화
echo ""
echo "[2/3] Initializing database..."
mongosh receiptify < init_database_v2.js

if [ $? -eq 0 ]; then
    echo "✅ Database initialized successfully"
else
    echo "❌ Database initialization failed"
    exit 1
fi

# Node.js 의존성 설치
echo ""
echo "[3/3] Installing Node.js dependencies..."
cd ..
npm install

if [ $? -eq 0 ]; then
    echo "✅ Dependencies installed successfully"
else
    echo "❌ Failed to install dependencies"
    exit 1
fi

echo ""
echo "=========================================="
echo "✅ Setup completed!"
echo "=========================================="
echo ""
echo "To start the server:"
echo "  npm start"
echo ""
echo "Test account:"
echo "  Email: test@receiptify.com"
echo "  Password: test123456"
echo ""