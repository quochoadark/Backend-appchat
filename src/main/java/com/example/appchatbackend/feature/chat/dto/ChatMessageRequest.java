package com.example.appchatbackend.feature.chat.dto;

import com.example.appchatbackend.feature.message.Message;
import com.example.appchatbackend.feature.message.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

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
