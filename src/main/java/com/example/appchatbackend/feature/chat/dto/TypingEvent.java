package com.example.appchatbackend.feature.chat.dto;

import lombok.Data;

/**
 * TypingEvent — su kien "dang go" hoac "da dung go" trong hoi thoai.
 *
 * Duong di: client → /app/chat.typing → ChatController.typing() → broadcast
 * toi /topic/conversation/{id} → cac client khac hien "X dang go..."
 *
 * typing = true  → bat dau go
 * typing = false → dung go (hoac gui tin xong)
 *
 * Cung duoc dung cho /app/chat.read (ChatController.markAsRead()) de lay conversationId.
 */
@Data
public class TypingEvent {

    private String conversationId;

    // Duoc set boi server tu principal, client khong can gui
    private String senderId;

    private boolean typing;
}
