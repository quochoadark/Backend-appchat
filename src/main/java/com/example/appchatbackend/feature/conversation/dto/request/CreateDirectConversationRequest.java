package com.example.appchatbackend.feature.conversation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * CreateDirectConversationRequest — DTO cho POST /conversations/direct.
 * Chi can ID cua nguoi muon bat dau chat.
 */
@Data
public class CreateDirectConversationRequest {

    @NotBlank(message = "targetUserId khong duoc de trong")
    private String targetUserId;
}
