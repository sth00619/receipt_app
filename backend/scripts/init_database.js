// backend/scripts/init_database.js
// Receiptify 데이터베이스 초기화 스크립트

print("=".repeat(60));
print("Receiptify 데이터베이스 초기화 시작");
print("=".repeat(60));

// 데이터베이스 선택
db = db.getSiblingDB('receiptify');

print("\n[1/5] 기존 컬렉션 삭제 중...");
db.users.drop();
db.receipts.drop();
db.transactions.drop();
db.categories.drop();
db.monthly_stats.drop();
print("✅ 기존 컬렉션 삭제 완료");

print("\n[2/5] 컬렉션 생성 중...");

// Users 컬렉션
db.createCollection("users", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["uid", "email", "createdAt"],
      properties: {
        uid: { bsonType: "string" },
        email: {
          bsonType: "string",
          pattern: "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        },
        displayName: { bsonType: "string" },
        provider: { enum: ["firebase", "google", "naver"] }
      }
    }
  }
});

// Receipts 컬렉션
db.createCollection("receipts", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["userId", "totalAmount", "transactionDate"],
      properties: {
        userId: { bsonType: "string" },
        // totalAmount의 스키마 타입이 double이므로, 샘플 데이터에 Double()을 사용합니다.
        totalAmount: { bsonType: "double", minimum: 0 },
        category: { enum: ["food", "transport", "shopping", "others"] }
      }
    }
  }
});

// Transactions 컬렉션
db.createCollection("transactions");

// Categories 컬렉션
db.createCollection("categories");

// Monthly Stats 컬렉션
db.createCollection("monthly_stats");

print("✅ 컬렉션 생성 완료");

print("\n[3/5] 인덱스 생성 중...");

// Users 인덱스
db.users.createIndex({ "uid": 1 }, { unique: true });
db.users.createIndex({ "email": 1 }, { unique: true });
db.users.createIndex({ "createdAt": -1 });

// Receipts 인덱스
db.receipts.createIndex({ "userId": 1, "transactionDate": -1 });
db.receipts.createIndex({ "userId": 1, "category": 1 });
db.receipts.createIndex({ "storeName": "text" });

// Transactions 인덱스
db.transactions.createIndex({ "userId": 1, "date": -1 });

// Monthly Stats 인덱스
db.monthly_stats.createIndex(
  { "userId": 1, "year": 1, "month": 1 },
  { unique: true }
);

print("✅ 인덱스 생성 완료");

print("\n[4/5] 카테고리 마스터 데이터 삽입 중...");

db.categories.insertMany([
  {
    code: "food",
    name: { ko: "식비", en: "Food" },
    icon: "ic_category_food",
    color: "#F59E0B",
    order: 1,
    isActive: true
  },
  {
    code: "transport",
    name: { ko: "교통", en: "Transport" },
    icon: "ic_category_transport",
    color: "#3B82F6",
    order: 2,
    isActive: true
  },
  {
    code: "shopping",
    name: { ko: "쇼핑", en: "Shopping" },
    icon: "ic_category_shopping",
    color: "#EC4899",
    order: 3,
    isActive: true
  },
  {
    code: "others",
    name: { ko: "기타", en: "Others" },
    icon: "ic_category_others",
    color: "#6B7280",
    order: 4,
    isActive: true
  }
]);

print("✅ 카테고리 데이터 삽입 완료");

print("\n[5/5] 샘플 데이터 생성 중...");

// 테스트 사용자
const testUserId = "firebase_test_user_001";

db.users.insertOne({
  uid: testUserId,
  email: "test@receiptify.com",
  displayName: "테스트 사용자",
  photoUrl: "",
  provider: "firebase",
  preferences: {
    notifications: true,
    darkMode: false,
    language: "ko"
  },
  stats: {
    totalReceipts: 5,
    totalTransactions: 5,
    totalSpending: 86900
  },
  createdAt: new Date("2024-11-01"),
  updatedAt: new Date(),
  lastLoginAt: new Date()
});

// 샘플 영수증 5개
const receipts = [
  {
    userId: testUserId,
    storeName: "스타벅스 강남점",
    totalAmount: Double(12500), // Double 타입으로 수정
    transactionDate: new Date("2024-11-01T10:30:00"),
    category: "food",
    items: [
      { name: "아메리카노 Tall", quantity: 2, unitPrice: 4500, amount: 9000 },
      { name: "카페라떼 Grande", quantity: 1, unitPrice: 5500, amount: 5500 }
    ],
    paymentMethod: "card",
    isVerified: true,
    createdAt: new Date("2024-11-01T10:35:00"),
    updatedAt: new Date("2024-11-01T10:35:00")
  },
  {
    userId: testUserId,
    storeName: "GS25 서초점",
    totalAmount: Double(8900), // Double 타입으로 수정
    transactionDate: new Date("2024-11-02T14:20:00"),
    category: "food",
    items: [
      { name: "삼각김밥 참치", quantity: 2, unitPrice: 1500, amount: 3000 },
      { name: "바나나우유", quantity: 1, unitPrice: 1500, amount: 1500 },
      { name: "컵라면", quantity: 1, unitPrice: 1900, amount: 1900 }
    ],
    paymentMethod: "card",
    isVerified: true,
    createdAt: new Date("2024-11-02T14:25:00"),
    updatedAt: new Date("2024-11-02T14:25:00")
  },
  {
    userId: testUserId,
    storeName: "지하철 교통카드",
    totalAmount: Double(1400), // Double 타입으로 수정
    transactionDate: new Date("2024-11-03T08:00:00"),
    category: "transport",
    items: [
      { name: "지하철 요금", quantity: 1, unitPrice: 1400, amount: 1400 }
    ],
    paymentMethod: "card",
    isVerified: true,
    createdAt: new Date("2024-11-03T08:05:00"),
    updatedAt: new Date("2024-11-03T08:05:00")
  },
  {
    userId: testUserId,
    storeName: "쿠팡",
    totalAmount: Double(45600), // Double 타입으로 수정
    transactionDate: new Date("2024-11-04T19:30:00"),
    category: "shopping",
    items: [
      { name: "무선마우스", quantity: 1, unitPrice: 25900, amount: 25900 },
      { name: "USB 케이블", quantity: 2, unitPrice: 9850, amount: 19700 }
    ],
    paymentMethod: "card",
    isVerified: true,
    createdAt: new Date("2024-11-04T19:35:00"),
    updatedAt: new Date("2024-11-04T19:35:00")
  },
  {
    userId: testUserId,
    storeName: "맥도날드 역삼점",
    totalAmount: Double(18500), // Double 타입으로 수정
    transactionDate: new Date("2024-11-05T12:10:00"),
    category: "food",
    items: [
      { name: "빅맥 세트", quantity: 1, unitPrice: 7500, amount: 7500 },
      { name: "맥스파이시 상하이버거 세트", quantity: 1, unitPrice: 8000, amount: 8000 }
    ],
    paymentMethod: "card",
    isVerified: true,
    createdAt: new Date("2024-11-05T12:15:00"),
    updatedAt: new Date("2024-11-05T12:15:00")
  }
];

const insertedReceipts = db.receipts.insertMany(receipts);

// Transactions 생성
const transactions = receipts.map((r, idx) => ({
  userId: testUserId,
  // insertedReceipts가 정상적인 결과를 반환한다고 가정하고 ObjectId를 가져옵니다.
  receiptId: Object.values(insertedReceipts.insertedIds)[idx],
  storeName: r.storeName,
  category: r.category,
  amount: r.totalAmount,
  date: r.transactionDate,
  createdAt: r.createdAt,
  updatedAt: r.updatedAt
}));

db.transactions.insertMany(transactions);

print("✅ 샘플 데이터 생성 완료");

print("\n" + "=".repeat(60));
print("✅ Receiptify 데이터베이스 초기화 완료!");
print("=".repeat(60));

print("\n📊 생성된 데이터:");
print(`  - 사용자: ${db.users.countDocuments()}명`);
print(`  - 영수증: ${db.receipts.countDocuments()}개`);
print(`  - 거래내역: ${db.transactions.countDocuments()}건`);
print(`  - 카테고리: ${db.categories.countDocuments()}개`);

print("\n💡 테스트 계정:");
print(`  - UID: ${testUserId}`);
print(`  - Email: test@receiptify.com`);