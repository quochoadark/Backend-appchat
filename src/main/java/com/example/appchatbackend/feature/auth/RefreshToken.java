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
