// backend/src/config/firebase-admin.js
const admin = require('firebase-admin');
const path = require('path');

// 같은 디렉토리(src/config)에 있음
const serviceAccountPath = path.join(__dirname, 'firebase-service-account.json');

try {
  const serviceAccount = require(serviceAccountPath);

  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });

  console.log('✅ Firebase Admin SDK 초기화 성공');
  console.log(`   프로젝트: ${serviceAccount.project_id}`);
  console.log(`   서비스 계정: ${serviceAccount.client_email}`);
} catch (error) {
  console.error('❌ Firebase Admin SDK 초기화 실패:', error.message);
}

const auth = admin.auth();
const firestore = admin.firestore();

module.exports = {
  admin,
  auth,
  firestore
};