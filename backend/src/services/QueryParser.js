/**
 * QueryParser - Natural Language Query Parser for Spending Analysis
 * Parses user questions about spending by category and date range
 */

// Category mappings (English & Korean)
const CATEGORY_MAPPINGS = {
    // English mappings
    'food': 'food',
    'foods': 'food',
    'meal': 'food',
    'meals': 'food',
    'restaurant': 'food',
    'restaurants': 'food',
    'dining': 'food',
    'eat': 'food',
    'eating': 'food',
    'grocery': 'food',
    'groceries': 'food',
    'lunch': 'food',
    'dinner': 'food',
    'breakfast': 'food',
    'snack': 'food',
    'snacks': 'food',
    'coffee': 'food',
    'cafe': 'food',
    'delivery': 'food',

    'transport': 'transport',
    'transportation': 'transport',
    'taxi': 'transport',
    'bus': 'transport',
    'subway': 'transport',
    'metro': 'transport',
    'train': 'transport',
    'uber': 'transport',
    'lyft': 'transport',
    'gas': 'transport',
    'fuel': 'transport',
    'parking': 'transport',
    'toll': 'transport',
    'travel': 'transport',

    'shopping': 'shopping',
    'shop': 'shopping',
    'clothes': 'shopping',
    'clothing': 'shopping',
    'fashion': 'shopping',
    'electronics': 'shopping',
    'gadget': 'shopping',
    'gadgets': 'shopping',
    'online': 'shopping',
    'amazon': 'shopping',
    'retail': 'shopping',

    'healthcare': 'healthcare',
    'health': 'healthcare',
    'medical': 'healthcare',
    'medicine': 'healthcare',
    'doctor': 'healthcare',
    'hospital': 'healthcare',
    'pharmacy': 'healthcare',
    'drug': 'healthcare',
    'drugs': 'healthcare',
    'dental': 'healthcare',
    'dentist': 'healthcare',

    'entertainment': 'entertainment',
    'fun': 'entertainment',
    'movie': 'entertainment',
    'movies': 'entertainment',
    'game': 'entertainment',
    'games': 'entertainment',
    'gaming': 'entertainment',
    'concert': 'entertainment',
    'show': 'entertainment',
    'netflix': 'entertainment',
    'spotify': 'entertainment',
    'subscription': 'entertainment',
    'leisure': 'entertainment',

    'utilities': 'utilities',
    'utility': 'utilities',
    'bill': 'utilities',
    'bills': 'utilities',
    'electric': 'utilities',
    'electricity': 'utilities',
    'water': 'utilities',
    'internet': 'utilities',
    'phone': 'utilities',
    'rent': 'utilities',
    'insurance': 'utilities',

    'others': 'others',
    'other': 'others',
    'miscellaneous': 'others',
    'misc': 'others',

    // Korean mappings
    '식비': 'food',
    '음식': 'food',
    '식사': 'food',
    '밥': 'food',
    '외식': 'food',
    '배달': 'food',
    '커피': 'food',
    '카페': 'food',
    '점심': 'food',
    '저녁': 'food',
    '아침': 'food',

    '교통': 'transport',
    '교통비': 'transport',
    '택시': 'transport',
    '버스': 'transport',
    '지하철': 'transport',
    '기차': 'transport',
    '주유': 'transport',
    '주차': 'transport',

    '쇼핑': 'shopping',
    '옷': 'shopping',
    '의류': 'shopping',
    '전자제품': 'shopping',
    '구매': 'shopping',

    '의료': 'healthcare',
    '병원': 'healthcare',
    '약국': 'healthcare',
    '건강': 'healthcare',
    '치과': 'healthcare',

    '문화': 'entertainment',
    '여가': 'entertainment',
    '영화': 'entertainment',
    '게임': 'entertainment',
    '취미': 'entertainment',
    '구독': 'entertainment',

    '공과금': 'utilities',
    '관리비': 'utilities',
    '전기': 'utilities',
    '수도': 'utilities',
    '통신': 'utilities',
    '보험': 'utilities',
    '월세': 'utilities',

    '기타': 'others'
};

// Category display names
const CATEGORY_NAMES = {
    'food': { en: 'Food', ko: '식비' },
    'transport': { en: 'Transport', ko: '교통' },
    'shopping': { en: 'Shopping', ko: '쇼핑' },
    'healthcare': { en: 'Healthcare', ko: '의료/건강' },
    'entertainment': { en: 'Entertainment', ko: '문화/여가' },
    'utilities': { en: 'Utilities', ko: '공과금' },
    'others': { en: 'Others', ko: '기타' }
};

// Time period patterns
const TIME_PATTERNS = {
    // English patterns
    today: { type: 'today' },
    yesterday: { type: 'yesterday' },
    'this week': { type: 'thisWeek' },
    'last week': { type: 'lastWeek' },
    'this month': { type: 'thisMonth' },
    'last month': { type: 'lastMonth' },
    'this year': { type: 'thisYear' },
    'last year': { type: 'lastYear' },

    // Korean patterns
    '오늘': { type: 'today' },
    '어제': { type: 'yesterday' },
    '이번 주': { type: 'thisWeek' },
    '이번주': { type: 'thisWeek' },
    '지난 주': { type: 'lastWeek' },
    '지난주': { type: 'lastWeek' },
    '이번 달': { type: 'thisMonth' },
    '이번달': { type: 'thisMonth' },
    '지난 달': { type: 'lastMonth' },
    '지난달': { type: 'lastMonth' },
    '올해': { type: 'thisYear' },
    '작년': { type: 'lastYear' },
    '금주': { type: 'thisWeek' },
    '금월': { type: 'thisMonth' }
};

// Query intent patterns
const INTENT_PATTERNS = {
    total: [
        /total\s*(spending|spent|expense|cost)/i,
        /how\s*much\s*(did\s*i\s*)?(spend|spent|cost)/i,
        /what\s*(is|was|are)\s*(the\s*)?(total|my)/i,
        /spending\s*(on|for|of)/i,
        /얼마/,
        /총/,
        /전체/,
        /합계/
    ],
    compare: [
        /compare/i,
        /vs/i,
        /versus/i,
        /비교/
    ],
    breakdown: [
        /breakdown/i,
        /분류/,
        /카테고리별/
    ],
    trend: [
        /trend/i,
        /over\s*time/i,
        /추세/,
        /변화/
    ]
};

/**
 * Parse a natural language query about spending
 * @param {string} query - The user's question
 * @returns {Object} Parsed query with intent, categories, and date range
 */
function parseQuery(query) {
    const lowerQuery = query.toLowerCase();

    // Detect intent
    let intent = 'total'; // Default intent
    for (const [intentType, patterns] of Object.entries(INTENT_PATTERNS)) {
        for (const pattern of patterns) {
            if (pattern.test(query)) {
                intent = intentType;
                break;
            }
        }
    }

    // Extract categories
    const categories = extractCategories(lowerQuery);

    // Extract date range
    const dateRange = extractDateRange(query);

    // Detect language
    const isKorean = /[ㄱ-ㅎ|ㅏ-ㅣ|가-힣]/.test(query);

    return {
        originalQuery: query,
        intent,
        categories,
        dateRange,
        isKorean,
        isValidQuery: categories.length > 0 || dateRange.type !== null || intent !== 'total'
    };
}

/**
 * Extract categories from query
 * @param {string} query - Lowercase query string
 * @returns {Array} Array of category identifiers
 */
function extractCategories(query) {
    const found = new Set();

    for (const [keyword, category] of Object.entries(CATEGORY_MAPPINGS)) {
        // Use word boundary for English, simple includes for Korean
        if (/[a-z]/.test(keyword)) {
            const regex = new RegExp(`\\b${keyword}\\b`, 'i');
            if (regex.test(query)) {
                found.add(category);
            }
        } else {
            if (query.includes(keyword)) {
                found.add(category);
            }
        }
    }

    return Array.from(found);
}

/**
 * Extract date range from query
 * @param {string} query - Original query string
 * @returns {Object} Date range object with start and end dates
 */
function extractDateRange(query) {
    const lowerQuery = query.toLowerCase();
    const now = new Date();

    // Check for time patterns
    for (const [pattern, config] of Object.entries(TIME_PATTERNS)) {
        if (lowerQuery.includes(pattern)) {
            return calculateDateRange(config.type, now);
        }
    }

    // Check for relative day patterns
    const daysMatch = query.match(/last\s*(\d+)\s*days?/i) ||
                      query.match(/past\s*(\d+)\s*days?/i) ||
                      query.match(/지난\s*(\d+)\s*일/);
    if (daysMatch) {
        const days = parseInt(daysMatch[1]);
        const startDate = new Date(now);
        startDate.setDate(startDate.getDate() - days);
        startDate.setHours(0, 0, 0, 0);
        return {
            type: 'lastNDays',
            days,
            startDate,
            endDate: new Date(now)
        };
    }

    // Check for specific month patterns
    const monthMatch = query.match(/(\d{1,2})월/) ||
                       query.match(/(january|february|march|april|may|june|july|august|september|october|november|december)/i);
    if (monthMatch) {
        let month;
        if (/\d/.test(monthMatch[1])) {
            month = parseInt(monthMatch[1]) - 1;
        } else {
            const months = ['january', 'february', 'march', 'april', 'may', 'june',
                          'july', 'august', 'september', 'october', 'november', 'december'];
            month = months.indexOf(monthMatch[1].toLowerCase());
        }

        const year = now.getFullYear();
        const startDate = new Date(year, month, 1);
        const endDate = new Date(year, month + 1, 0, 23, 59, 59);

        return {
            type: 'specificMonth',
            month: month + 1,
            year,
            startDate,
            endDate
        };
    }

    // Default to this month if no time pattern found
    return {
        type: null,
        startDate: new Date(now.getFullYear(), now.getMonth(), 1),
        endDate: new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59)
    };
}

/**
 * Calculate date range based on type
 * @param {string} type - Type of date range
 * @param {Date} now - Current date
 * @returns {Object} Date range with start and end dates
 */
function calculateDateRange(type, now) {
    const startDate = new Date(now);
    const endDate = new Date(now);

    switch (type) {
        case 'today':
            startDate.setHours(0, 0, 0, 0);
            endDate.setHours(23, 59, 59, 999);
            break;

        case 'yesterday':
            startDate.setDate(startDate.getDate() - 1);
            startDate.setHours(0, 0, 0, 0);
            endDate.setDate(endDate.getDate() - 1);
            endDate.setHours(23, 59, 59, 999);
            break;

        case 'thisWeek':
            const dayOfWeek = startDate.getDay();
            const diffToMonday = dayOfWeek === 0 ? 6 : dayOfWeek - 1;
            startDate.setDate(startDate.getDate() - diffToMonday);
            startDate.setHours(0, 0, 0, 0);
            endDate.setHours(23, 59, 59, 999);
            break;

        case 'lastWeek':
            const currentDay = startDate.getDay();
            const diffToLastMonday = currentDay === 0 ? 13 : currentDay + 6;
            startDate.setDate(startDate.getDate() - diffToLastMonday);
            startDate.setHours(0, 0, 0, 0);
            endDate.setDate(startDate.getDate() + 6);
            endDate.setHours(23, 59, 59, 999);
            break;

        case 'thisMonth':
            startDate.setDate(1);
            startDate.setHours(0, 0, 0, 0);
            endDate.setMonth(endDate.getMonth() + 1, 0);
            endDate.setHours(23, 59, 59, 999);
            break;

        case 'lastMonth':
            startDate.setMonth(startDate.getMonth() - 1, 1);
            startDate.setHours(0, 0, 0, 0);
            endDate.setDate(0);
            endDate.setHours(23, 59, 59, 999);
            break;

        case 'thisYear':
            startDate.setMonth(0, 1);
            startDate.setHours(0, 0, 0, 0);
            endDate.setMonth(11, 31);
            endDate.setHours(23, 59, 59, 999);
            break;

        case 'lastYear':
            startDate.setFullYear(startDate.getFullYear() - 1, 0, 1);
            startDate.setHours(0, 0, 0, 0);
            endDate.setFullYear(endDate.getFullYear() - 1, 11, 31);
            endDate.setHours(23, 59, 59, 999);
            break;

        default:
            startDate.setDate(1);
            startDate.setHours(0, 0, 0, 0);
            endDate.setMonth(endDate.getMonth() + 1, 0);
            endDate.setHours(23, 59, 59, 999);
    }

    return {
        type,
        startDate,
        endDate
    };
}

/**
 * Get category display name
 * @param {string} category - Category identifier
 * @param {boolean} isKorean - Whether to return Korean name
 * @returns {string} Display name
 */
function getCategoryDisplayName(category, isKorean = false) {
    const names = CATEGORY_NAMES[category];
    if (!names) return category;
    return isKorean ? names.ko : names.en;
}

/**
 * Get date range description
 * @param {Object} dateRange - Date range object
 * @param {boolean} isKorean - Whether to return Korean description
 * @returns {string} Date range description
 */
function getDateRangeDescription(dateRange, isKorean = false) {
    const descriptions = {
        today: { en: 'today', ko: '오늘' },
        yesterday: { en: 'yesterday', ko: '어제' },
        thisWeek: { en: 'this week', ko: '이번 주' },
        lastWeek: { en: 'last week', ko: '지난 주' },
        thisMonth: { en: 'this month', ko: '이번 달' },
        lastMonth: { en: 'last month', ko: '지난 달' },
        thisYear: { en: 'this year', ko: '올해' },
        lastYear: { en: 'last year', ko: '작년' },
        lastNDays: { en: `last ${dateRange.days} days`, ko: `지난 ${dateRange.days}일` },
        specificMonth: {
            en: new Date(dateRange.year, dateRange.month - 1).toLocaleString('en', { month: 'long' }),
            ko: `${dateRange.month}월`
        }
    };

    const desc = descriptions[dateRange.type];
    if (!desc) {
        // Default to this month
        return isKorean ? '이번 달' : 'this month';
    }
    return isKorean ? desc.ko : desc.en;
}

/**
 * Get all available categories
 * @returns {Array} Array of category objects
 */
function getAllCategories() {
    return Object.entries(CATEGORY_NAMES).map(([key, names]) => ({
        id: key,
        nameEn: names.en,
        nameKo: names.ko
    }));
}

module.exports = {
    parseQuery,
    extractCategories,
    extractDateRange,
    getCategoryDisplayName,
    getDateRangeDescription,
    getAllCategories,
    CATEGORY_MAPPINGS,
    CATEGORY_NAMES
};
