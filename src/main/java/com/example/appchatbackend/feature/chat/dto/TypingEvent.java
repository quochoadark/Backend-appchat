package com.example.appchatbackend.feature.chat.dto;

import lombok.Data;

@Data
public class TypingEvent {

    private String conversationId;

    // Được set bởi server từ principal, client không cần gửi
    private String senderId;

    private boolean typing;
}
