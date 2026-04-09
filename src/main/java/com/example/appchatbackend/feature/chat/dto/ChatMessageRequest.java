package com.example.appchatbackend.feature.chat.dto;

import com.example.appchatbackend.feature.message.Message;
import com.example.appchatbackend.feature.message.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * ChatMessageRequest — DTO nhan tu client khi gui tin nhan qua WebSocket.
 * STOMP endpoint: /app/chat.send → ChatController.sendMessage()
 *
 * Tuong tu SendMessageRequest (REST) nhung dung cho WebSocket.
 * senderId khong co trong DTO — server lay tu Principal (WebSocketAuthInterceptor da set).
 */
@Data
public class ChatMessageRequest {

    @NotBlank
    private String conversationId;

    @NotNull
    private MessageType messageType;

    private String content;

    private Message.MediaAttachment media;

    private String replyToMessageId;
}
