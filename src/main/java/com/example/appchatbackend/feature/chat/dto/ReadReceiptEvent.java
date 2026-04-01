package com.example.appchatbackend.feature.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class ReadReceiptEvent {

    private String conversationId;

    private String userId;

    private Instant readAt;
}
