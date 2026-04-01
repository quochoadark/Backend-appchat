package com.example.appchatbackend.feature.conversation.dto.request;

import lombok.Data;

@Data
public class UpdateConversationRequest {

    private String name;

    private String avatarUrl;

    private String description;
}
