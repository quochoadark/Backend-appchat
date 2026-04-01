package com.example.appchatbackend.feature.friend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendFriendRequestDto {

    @NotBlank(message = "targetUserId không được để trống")
    private String targetUserId;
}
