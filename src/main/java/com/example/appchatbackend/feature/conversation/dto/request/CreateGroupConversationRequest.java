package com.example.appchatbackend.feature.conversation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * CreateGroupConversationRequest — DTO cho POST /conversations/group.
 * participantIds la danh sach userId thanh vien (khong tinh nguoi tao — controller tu them vao).
 */
@Data
public class CreateGroupConversationRequest {

    @NotBlank(message = "Ten nhom khong duoc de trong")
    private String name;

    private String description;

    private String avatarUrl;

    @NotEmpty(message = "Danh sach thanh vien khong duoc de trong")
    private List<String> participantIds;
}
