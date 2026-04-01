package com.example.appchatbackend.feature.conversation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateDirectConversationRequest {

    @NotBlank(message = "targetUserId không được để trống")
    private String targetUserId;
}
