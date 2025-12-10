const mongoose = require('mongoose');

const chatMessageSchema = new mongoose.Schema({
    userId: {
        type: String,
        required: true,
        index: true
    },
    role: {
        type: String,
        enum: ['user', 'bot', 'system'],
        required: true
    },
    message: {
        type: String,
        required: true
    },
    metadata: {
        type: mongoose.Schema.Types.Mixed,
        default: {}
    },
    relatedNotificationId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Notification',
        default: null
    }
}, {
    timestamps: true
});

// 사용자별 최근 메시지 조회용 인덱스
chatMessageSchema.index({ userId: 1, createdAt: -1 });

module.exports = mongoose.model('ChatMessage', chatMessageSchema);
