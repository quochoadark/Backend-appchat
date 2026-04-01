package com.example.appchatbackend.feature.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatNotification {

    private NotificationType type;

    private String conversationId;

    // Payload linh hoạt: Message, TypingEvent, ReadReceiptEvent, v.v.
    private Object data;

    public enum NotificationType {
        NEW_MESSAGE,
        TYPING,
        READ_RECEIPT,
        USER_ONLINE,
        USER_OFFLINE
    }
}
