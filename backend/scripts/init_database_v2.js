// backend/scripts/init_database_v2.js
// Receiptify 데이터베이스 초기화 스크립트 v2
// 일반 로그인 지원 + 파싱된 영수증 데이터

print("=".repeat(60));
print("Receiptify 데이터베이스 초기화 v2 시작");
print("=".repeat(60));

// 데이터베이스 선택
db = db.getSiblingDB('receiptify');

print("\n[1/6] 기존 컬렉션 삭제 중...");
db.users.drop();
db.receipts.drop();
db.parsed_receipts.drop();
db.transactions.drop();
db.categories.drop();
db.spending_patterns.drop();
print("✅ 기존 컬렉션 삭제 완료");

print("\n[2/6] 컬렉션 생성 중...");

// Users 컬렉션
db.createCollection("users", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["email", "createdAt"],
      properties: {
        uid: { bsonType: "string" },
        email: {
          bsonType: "string",
          pattern: "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        },
        password: { bsonType: "string" },
        displayName: { bsonType: "string" },
        provider: { enum: ["email", "firebase", "google", "naver"] }
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
        totalAmount: { bsonType: "double", minimum: 0 },
        category: { enum: ["food", "transport", "shopping", "healthcare", "entertainment", "utilities", "others"] }
      }
    }
  }
});

// Parsed Receipts 컬렉션
db.createCollection("parsed_receipts", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["receiptId", "userId", "rawOcrText", "totalAmount"],
      properties: {
        userId: { bsonType: "string" },
        totalAmount: { bsonType: "double", minimum: 0 },
        parsingStatus: { enum: ["success", "partial", "failed"] }
      }
    }
  }
});

// Transactions 컬렉션
db.createCollection("transactions");

// Categories 컬렉션
db.createCollection("categories");

// Spending Patterns 컬렉션
db.createCollection("spending_patterns");

print("✅ 컬렉션 생성 완료");

print("\n[3/6] 인덱스 생성 중...");

// Users 인덱스
db.users.createIndex({ "email": 1 }, { unique: true });
db.users.createIndex({ "uid": 1 }, { sparse: true });
db.users.createIndex({ "createdAt": -1 });

// Receipts 인덱스
db.receipts.createIndex({ "userId": 1, "transactionDate": -1 });
db.receipts.createIndex({ "userId": 1, "category": 1 });
db.receipts.createIndex({ "storeName": "text" });

// Parsed Receipts 인덱스
db.parsed_receipts.createIndex({ "receiptId": 1 }, { unique: true });
db.parsed_receipts.createIndex({ "userId": 1, "createdAt": -1 });

// Transactions 인덱스
db.transactions.createIndex({ "userId": 1, "date": -1 });

// Spending Patterns 인덱스
db.spending_patterns.createIndex(
  { "userId": 1, "periodType": 1, "year": -1, "month": -1 }
);

print("✅ 인덱스 생성 완료");

print("\n[4/6] 카테고리 마스터 데이터 삽입 중...");

db.categories.insertMany([
  {
    code: "food",
    name: { ko: "식비", en: "Food" },
    icon: "ic_category_food",
    color: "#F59E0B",
    keywords: ["스타벅스", "카페", "coffee", "gs25", "cu", "편의점", "맥도날드", "버거", "치킨", "피자"],
    order: 1,
    isActive: true
  },
  {
    code: "transport",
    name: { ko: "교통", en: "Transport" },
    icon: "ic_category_transport",
    color: "#3B82F6",
    keywords: ["지하철", "버스", "택시", "주유", "oil", "gas", "parking", "주차"],
    order: 2,
    isActive: true
  },
  {
    code: "shopping",
    name: { ko: "쇼핑", en: "Shopping" },
    icon: "ic_category_shopping",
    color: "#EC4899",
    keywords: ["쿠팡", "마켓", "mart", "이마트", "홈플러스", "다이소", "올리브영"],
    order: 3,
    isActive: true
  },
  {
    code: "healthcare",
    name: { ko: "건강/의료", en: "Healthcare" },
    icon: "ic_category_healthcare",
    color: "#10B981",
    keywords: ["병원", "약국", "pharmacy", "hospital", "clinic", "헬스", "gym"],
    order: 4,
    isActive: true
  },
  {
    code: "entertainment",
    name: { ko: "문화/여가", en: "Entertainment" },
    icon: "ic_category_entertainment",
    color: "#8B5CF6",
    keywords: ["영화", "cgv", "롯데시네마", "메가박스", "노래방", "pc방", "볼링"],
    order: 5,
    isActive: true
  },
  {
    code: "utilities",
    name: { ko: "공과금", en: "Utilities" },
    icon: "ic_category_utilities",
    color: "#6B7280",
    keywords: ["전기", "가스", "수도", "통신", "인터넷", "핸드폰"],
    order: 6,
    isActive: true
  },
  {
    code: "others",
    name: { ko: "기타", en: "Others" },
    icon: "ic_category_others",
    color: "#9CA3AF",
    keywords: [],
    order: 7,
    isActive: true
  }
]);

print("✅ 카테고리 데이터 삽입 완료");

print("\n[5/6] 샘플 데이터 생성 중...");

// 테스트 사용자 (일반 로그인)
const testUserId = "test_user_001";

// bcrypt 해시는 실제로는 백엔드에서 생성됨
// 여기서는 플레인텍스트로 저장 (실제 운영에서는 절대 안 됨!)
db.users.insertOne({
  _id: ObjectId(),
  email: "test@receiptify.com",
  password: "$2a$10$xYzAbC123...hashed_password_here",  // 실제로는 해시된 값
  displayName: "테스트 사용자",
  photoUrl: "",
  provider: "email",
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

const userId = testUserId;

// 샘플 영수증 5개
const receipt1 = {
  userId: userId,
  storeName: "스타벅스 강남점",
  storeAddress: "서울시 강남구 테헤란로 123",
  storePhone: "02-1234-5678",
  totalAmount: Double(12500),
  taxAmount: Double(1136),
  discountAmount: Double(0),
  transactionDate: new Date("2024-11-15T10:30:00"),
  paymentMethod: "card",
  category: "food",
  items: [
    { name: "아메리카노 Tall", quantity: 2, unitPrice: 4500, amount: 9000 },
    { name: "카페라떼 Grande", quantity: 1, unitPrice: 5500, amount: 5500 }
  ],
  isVerified: true,
  createdAt: new Date("2024-11-15T10:35:00"),
  updatedAt: new Date("2024-11-15T10:35:00")
};

const receipt2 = {
  userId: userId,
  storeName: "GS25 서초점",
  storeAddress: "서울시 서초구 서초대로 456",
  storePhone: "02-9876-5432",
  totalAmount: Double(8900),
  taxAmount: Double(809),
  transactionDate: new Date("2024-11-16T14:20:00"),
  paymentMethod: "card",
  category: "food",
  items: [
    { name: "삼각김밥 참치", quantity: 2, unitPrice: 1500, amount: 3000 },
    { name: "바나나우유", quantity: 1, unitPrice: 1500, amount: 1500 },
    { name: "컵라면", quantity: 1, unitPrice: 1900, amount: 1900 }
  ],
  isVerified: true,
  createdAt: new Date("2024-11-16T14:25:00"),
  updatedAt: new Date("2024-11-16T14:25:00")
};

const receipt3 = {
  userId: userId,
  storeName: "서울교통공사",
  totalAmount: Double(1400),
  transactionDate: new Date("2024-11-17T08:00:00"),
  paymentMethod: "card",
  category: "transport",
  items: [
    { name: "지하철 요금", quantity: 1, unitPrice: 1400, amount: 1400 }
  ],
  isVerified: true,
  createdAt: new Date("2024-11-17T08:05:00"),
  updatedAt: new Date("2024-11-17T08:05:00")
};

const receipt4 = {
  userId: userId,
  storeName: "쿠팡",
  totalAmount: Double(45600),
  transactionDate: new Date("2024-11-18T19:30:00"),
  paymentMethod: "card",
  category: "shopping",
  items: [
    { name: "무선마우스", quantity: 1, unitPrice: 25900, amount: 25900 },
    { name: "USB 케이블", quantity: 2, unitPrice: 9850, amount: 19700 }
  ],
  isVerified: true,
  createdAt: new Date("2024-11-18T19:35:00"),
  updatedAt: new Date("2024-11-18T19:35:00")
};

const receipt5 = {
  userId: userId,
  storeName: "맥도날드 역삼점",
  storeAddress: "서울시 강남구 역삼동 789",
  storePhone: "02-5555-6666",
  totalAmount: Double(18500),
  taxAmount: Double(1682),
  transactionDate: new Date("2024-11-19T12:10:00"),
  paymentMethod: "card",
  category: "food",
  items: [
    { name: "빅맥 세트", quantity: 1, unitPrice: 7500, amount: 7500 },
    { name: "맥스파이시 상하이버거 세트", quantity: 1, unitPrice: 8000, amount: 8000 },
    { name: "치즈스틱", quantity: 1, unitPrice: 3000, amount: 3000 }
  ],
  isVerified: true,
  createdAt: new Date("2024-11-19T12:15:00"),
  updatedAt: new Date("2024-11-19T12:15:00")
};

const receipts = [receipt1, receipt2, receipt3, receipt4, receipt5];
const insertedReceipts = db.receipts.insertMany(receipts);

// Parsed Receipts 생성 (OCR 결과)
const parsedReceipts = receipts.map((r, idx) => {
  const receiptId = Object.values(insertedReceipts.insertedIds)[idx];

  return {
    receiptId: receiptId,
    userId: userId,
    rawOcrText: `${r.storeName}\n${r.storeAddress || ""}\n${r.storePhone || ""}\n합계: ${r.totalAmount}\n${r.items.map(i => `${i.name} ${i.quantity} ${i.amount}`).join("\n")}`,
    storeName: r.storeName,
    storeAddress: r.storeAddress || null,
    storePhone: r.storePhone || null,
    businessNumber: null,
    transactionDate: r.transactionDate,
    transactionTime: null,
    subtotal: r.totalAmount - (r.taxAmount || 0),
    taxAmount: r.taxAmount || 0,
    discountAmount: r.discountAmount || 0,
    totalAmount: r.totalAmount,
    paymentMethod: r.paymentMethod,
    cardNumber: null,
    approvalNumber: null,
    items: r.items.map(item => ({
      name: item.name,
      quantity: item.quantity,
      unitPrice: item.unitPrice,
      totalPrice: item.amount,
      isManuallyEdited: false
    })),
    suggestedCategory: r.category,
    confidence: {
      storeName: 0.95,
      totalAmount: 0.98,
      items: 0.92
    },
    parsingStatus: "success",
    isManuallyVerified: false,
    createdAt: r.createdAt,
    updatedAt: r.updatedAt
  };
});

db.parsed_receipts.insertMany(parsedReceipts);

// Transactions 생성
const transactions = receipts.map((r, idx) => ({
  userId: userId,
  receiptId: Object.values(insertedReceipts.insertedIds)[idx],
  storeName: r.storeName,
  category: r.category,
  amount: r.totalAmount,
  date: r.transactionDate,
  createdAt: r.createdAt,
  updatedAt: r.updatedAt
}));

db.transactions.insertMany(transactions);

// Spending Pattern 생성 (11월)
const currentDate = new Date();
const currentYear = currentDate.getFullYear();
const currentMonth = currentDate.getMonth() + 1;

const categoryBreakdown = [
  { category: "food", amount: 39900, count: 3, percentage: 45.9 },
  { category: "transport", amount: 1400, count: 1, percentage: 1.6 },
  { category: "shopping", amount: 45600, count: 1, percentage: 52.5 }
];

const dayOfWeekPattern = [
  { day: 1, amount: 12500, count: 1 },  // 월요일
  { day: 2, amount: 8900, count: 1 },   // 화요일
  { day: 3, amount: 1400, count: 1 },   // 수요일
  { day: 4, amount: 45600, count: 1 },  // 목요일
  { day: 5, amount: 18500, count: 1 }   // 금요일
];

const timeOfDayPattern = [
  { hour: 8, amount: 1400, count: 1 },
  { hour: 10, amount: 12500, count: 1 },
  { hour: 12, amount: 18500, count: 1 },
  { hour: 14, amount: 8900, count: 1 },
  { hour: 19, amount: 45600, count: 1 }
];

const frequentStores = [
  { storeName: "스타벅스 강남점", visitCount: 1, totalSpent: 12500 },
  { storeName: "GS25 서초점", visitCount: 1, totalSpent: 8900 },
  { storeName: "쿠팡", visitCount: 1, totalSpent: 45600 }
];

db.spending_patterns.insertOne({
  userId: userId,
  periodType: "monthly",
  year: currentYear,
  month: currentMonth,
  categoryBreakdown: categoryBreakdown,
  dayOfWeekPattern: dayOfWeekPattern,
  timeOfDayPattern: timeOfDayPattern,
  frequentStores: frequentStores,
  stats: {
    totalAmount: 86900,
    averagePerTransaction: 17380,
    maxTransaction: 45600,
    minTransaction: 1400,
    transactionCount: 5
  },
  comparison: {
    previousPeriodAmount: 75000,
    changeAmount: 11900,
    changePercentage: 15.87
  },
  createdAt: new Date(),
  updatedAt: new Date()
});

print("✅ 샘플 데이터 생성 완료");

print("\n[6/6] 데이터 검증 중...");

const userCount = db.users.countDocuments();
const receiptCount = db.receipts.countDocuments();
const parsedReceiptCount = db.parsed_receipts.countDocuments();
const transactionCount = db.transactions.countDocuments();
const categoryCount = db.categories.countDocuments();
const patternCount = db.spending_patterns.countDocuments();

print("✅ 데이터 검증 완료");

print("\n" + "=".repeat(60));
print("✅ Receiptify 데이터베이스 초기화 완료!");
print("=".repeat(60));

print("\n📊 생성된 데이터:");
print(`  - 사용자: ${userCount}명`);
print(`  - 영수증: ${receiptCount}개`);
print(`  - 파싱된 영수증: ${parsedReceiptCount}개`);
print(`  - 거래내역: ${transactionCount}건`);
print(`  - 카테고리: ${categoryCount}개`);
print(`  - 소비 패턴: ${patternCount}개`);

print("\n💡 테스트 계정:");
print(`  - Email: test@receiptify.com`);
print(`  - Password: test123456`);
print(`  - 주의: 실제 사용시 백엔드에서 회원가입해야 합니다!`);

print("\n🚀 다음 단계:");
print("  1. 백엔드 서버 실행: cd backend && npm start");
print("  2. Android 앱에서 회원가입/로그인");
print("  3. 영수증 스캔 테스트");