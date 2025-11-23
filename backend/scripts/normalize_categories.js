// backend/scripts/normalize_categories.js
const mongoose = require('mongoose');
require('dotenv').config();

// 한글-영문 매핑
const categoryMapping = {
  '식비': 'food',
  '식료품': 'food',
  '음식': 'food',
  '외식': 'food',
  '카페': 'food',
  '커피': 'food',
  '간식': 'food',

  '교통': 'transport',
  '교통비': 'transport',
  '주유': 'transport',
  '택시': 'transport',
  '버스': 'transport',
  '지하철': 'transport',

  '쇼핑': 'shopping',
  '생활용품': 'shopping',
  '도서': 'shopping',
  '의류': 'shopping',
  '전자기기': 'shopping',
  '가전': 'shopping',

  '의료': 'healthcare',
  '병원': 'healthcare',
  '약국': 'healthcare',
  '건강': 'healthcare',

  '문화': 'entertainment',
  '여가': 'entertainment',
  '영화': 'entertainment',
  '취미': 'entertainment',
  '게임': 'entertainment',

  '공과금': 'utilities',
  '전기': 'utilities',
  '수도': 'utilities',
  '통신': 'utilities',
  '인터넷': 'utilities'
};

// 영문 코드 (변경하지 않음)
const validEnglishCategories = [
  'food', 'transport', 'shopping', 'healthcare',
  'entertainment', 'utilities', 'others'
];

async function normalizeCategories() {
  try {
    await mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/receiptify');
    console.log('✅ MongoDB 연결 성공\n');

    const Receipt = mongoose.model('Receipt', new mongoose.Schema({}, { strict: false }));

    // 전체 사용자의 영수증 조회
    const users = await Receipt.distinct('userId');
    console.log(`📊 처리할 사용자 수: ${users.length}명\n`);

    let totalUpdated = 0;

    for (const userId of users) {
      console.log(`\n👤 사용자 ID: ${userId}`);

      // 현재 카테고리 상태
      const currentCategories = await Receipt.aggregate([
        { $match: { userId: userId } },
        {
          $group: {
            _id: '$category',
            count: { $sum: 1 }
          }
        }
      ]);

      console.log('  📋 현재 카테고리:');
      currentCategories.forEach(cat => {
        console.log(`    - "${cat._id}": ${cat.count}개`);
      });

      // 한글 → 영문 변환
      for (const [korean, english] of Object.entries(categoryMapping)) {
        const result = await Receipt.updateMany(
          { userId: userId, category: korean },
          { $set: { category: english } }
        );

        if (result.modifiedCount > 0) {
          console.log(`  ✅ "${korean}" → "${english}": ${result.modifiedCount}개`);
          totalUpdated += result.modifiedCount;
        }
      }

      // 영문 코드가 아닌 것들을 'others'로
      const invalidResult = await Receipt.updateMany(
        {
          userId: userId,
          category: {
            $nin: validEnglishCategories,
            $ne: null,
            $exists: true
          }
        },
        { $set: { category: 'others' } }
      );

      if (invalidResult.modifiedCount > 0) {
        console.log(`  ✅ 기타 → "others": ${invalidResult.modifiedCount}개`);
        totalUpdated += invalidResult.modifiedCount;
      }

      // null/empty → 'others'
      const nullResult = await Receipt.updateMany(
        {
          userId: userId,
          $or: [
            { category: null },
            { category: { $exists: false } },
            { category: '' }
          ]
        },
        { $set: { category: 'others' } }
      );

      if (nullResult.modifiedCount > 0) {
        console.log(`  ✅ null/empty → "others": ${nullResult.modifiedCount}개`);
        totalUpdated += nullResult.modifiedCount;
      }

      // 변환 후 상태
      const updatedCategories = await Receipt.aggregate([
        { $match: { userId: userId } },
        {
          $group: {
            _id: '$category',
            count: { $sum: 1 }
          }
        },
        {
          $sort: { count: -1 }
        }
      ]);

      console.log('  📋 변환 후:');
      updatedCategories.forEach(cat => {
        console.log(`    - "${cat._id}": ${cat.count}개`);
      });
    }

    console.log(`\n✅ 총 ${totalUpdated}개 영수증의 카테고리를 정규화했습니다.`);

    await mongoose.disconnect();
    process.exit(0);

  } catch (error) {
    console.error('❌ 오류 발생:', error);
    process.exit(1);
  }
}

normalizeCategories();