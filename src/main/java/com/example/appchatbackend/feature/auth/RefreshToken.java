package com.example.appchatbackend.feature.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * RefreshToken — document lưu refresh token vào MongoDB collection "refresh_tokens".
 *
 * Mục đích: cho phép user lấy access token mới mà không cần đăng nhập lại.
 * Mỗi thiết bị / phiên đăng nhập sẽ có 1 refresh token riêng.
 *
 * - TTL tự động: MongoDB đọc field expiresAt và tự xóa document khi quá hạn (@Indexed expireAfter = "0s")
 * - isRevoked: cho phép thu hồi sớm (logout 1 thiết bị) mà không cần đợi hết TTL
 * - deviceInfo + ipAddress: cho phép hiển thị "Quản lý phiên đăng nhập" cho user
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "refresh_tokens")
public class RefreshToken {

    @Id
    private String id;

    // Token ngẫu nhiên (UUID) — dùng để tra cứu khi refresh
    @Indexed(unique = true)
    @Field("token")
    private String token;

    @Field("user_id")
    private String userId;

    // Thông tin thiết bị: "Chrome / Windows"
    @Field("device_info")
    private String deviceInfo;

    @Field("ip_address")
    private String ipAddress;

    // MongoDB TTL index: tự động xóa document khi quá thời hạn
    @Indexed(expireAfter = "0s")
    @Field("expires_at")
    private Instant expiresAt;

    // Đánh dấu đã thu hồi (logout trước khi hết hạn)
    @Builder.Default
    @Field("is_revoked")
    private boolean isRevoked = false;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;
}
