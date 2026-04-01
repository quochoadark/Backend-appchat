package com.example.appchatbackend.feature.message.dto.request;

import com.example.appchatbackend.feature.message.Message;
import com.example.appchatbackend.feature.message.MessageType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotNull(message = "Loại tin nhắn không được để trống")
    private MessageType messageType;

    private String content;

    private Message.MediaAttachment media;

    private String replyToMessageId;
}
