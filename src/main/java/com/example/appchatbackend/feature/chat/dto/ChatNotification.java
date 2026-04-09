package com.example.appchatbackend.feature.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ChatNotification — envelope chua tat ca loai thong bao real-time qua WebSocket.
 *
 * Duoc serialize thanh JSON khi publish len Redis va khi gui toi client.
 * Client doc truong "type" de biet cach xu ly payload trong "data":
 * - NEW_MESSAGE  → data la Message object → hien tin nhan moi
 * - TYPING       → data la TypingEvent   → hien "dang go..."
 * - READ_RECEIPT → data la ReadReceiptEvent → cap nhat trang thai da xem
 * - USER_ONLINE / USER_OFFLINE → data la userId (String) → cap nhat dot online
 *
 * data la Object de linhh hoat chua nhieu loai payload khac nhau ma khong can
 * tao nhieu class notification rieng biet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatNotification {

    private NotificationType type;

    private String conversationId;

    // Payload linh hoat: Message, TypingEvent, ReadReceiptEvent, userId (String)...
    private Object data;

    public enum NotificationType {
        NEW_MESSAGE,   // Tin nhan moi
        TYPING,        // Dang go
        READ_RECEIPT,  // Da doc
        USER_ONLINE,   // User vua online
        USER_OFFLINE   // User vua offline
    }
}
