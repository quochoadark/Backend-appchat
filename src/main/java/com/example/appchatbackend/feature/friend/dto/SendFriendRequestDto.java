package com.example.appchatbackend.feature.friend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * SendFriendRequestDto — DTO cho POST /friends/requests.
 * Chi can ID nguoi nhan loi moi ket ban.
 * senderId lay tu JWT cua nguoi dang dang nhap.
 */
@Data
public class SendFriendRequestDto {

    @NotBlank(message = "targetUserId khong duoc de trong")
    private String targetUserId;
}
