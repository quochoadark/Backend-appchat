package com.example.appchatbackend.feature.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

/**
 * ReadReceiptEvent — thong bao "da doc" duoc broadcast toi cac thanh vien trong hoi thoai.
 *
 * Khi user goi /app/chat.read, server:
 * 1. Cap nhat readBy trong MongoDB (markAsRead)
 * 2. Broadcast ReadReceiptEvent nay toi /topic/conversation/{id}
 * Client nhan duoc → hien thi "da xem" duoi tin nhan.
 */
@Data
@AllArgsConstructor
public class ReadReceiptEvent {

    private String conversationId;

    private String userId;   // User nao da doc

    private Instant readAt; // Thoi diem doc
}
