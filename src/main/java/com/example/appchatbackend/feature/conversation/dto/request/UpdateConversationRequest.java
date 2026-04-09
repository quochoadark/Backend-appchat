package com.example.appchatbackend.feature.conversation.dto.request;

import lombok.Data;

/**
 * UpdateConversationRequest — DTO cho PUT /conversations/{id}.
 * Tat ca cac field deu optional — chi cap nhat nhung gi client gui len.
 * Chi ap dung cho nhom (GROUP); hoi thoai DIRECT khong co ten/avatar.
 */
@Data
public class UpdateConversationRequest {

    private String name;

    private String avatarUrl;

    private String description;
}
