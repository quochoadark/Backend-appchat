package com.example.appchatbackend.feature.conversation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateGroupConversationRequest {

    @NotBlank(message = "Tên nhóm không được để trống")
    private String name;

    private String description;

    private String avatarUrl;

    @NotEmpty(message = "Danh sách thành viên không được để trống")
    private List<String> participantIds;
}
