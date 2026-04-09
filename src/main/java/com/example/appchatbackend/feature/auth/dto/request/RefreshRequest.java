package com.example.appchatbackend.feature.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * RefreshRequest — DTO nhận refresh token từ client.
 * Dùng cho: POST /auth/refresh (lấy access token mới)
 *       và: POST /auth/logout (thu hồi refresh token khi đăng xuất)
 */
@Data
public class RefreshRequest {

    @NotBlank(message = "Refresh token không được để trống")
    private String refreshToken;
}
