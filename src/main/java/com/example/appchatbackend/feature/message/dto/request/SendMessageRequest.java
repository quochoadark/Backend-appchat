package com.example.appchatbackend.feature.message.dto.request;

import com.example.appchatbackend.feature.message.Message;
import com.example.appchatbackend.feature.message.MessageType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * SendMessageRequest — DTO cho POST /conversations/{id}/messages (REST API).
 *
 * Quy tac:
 * - TEXT: bat buoc co content, media = null
 * - IMAGE/FILE: bat buoc co media, content = null (hoac ten file)
 * - replyToMessageId: tuy chon, null neu khong phai reply
 */
@Data
public class SendMessageRequest {

    @NotNull(message = "Loai tin nhan khong duoc de trong")
    private MessageType messageType;

    private String content;

    private Message.MediaAttachment media;

    private String replyToMessageId;
}
