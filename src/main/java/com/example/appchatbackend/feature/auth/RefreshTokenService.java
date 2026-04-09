package com.example.appchatbackend.feature.auth;

import java.util.List;
import java.util.Optional;

/**
 * RefreshTokenService — interface định nghĩa nghiệp vụ quản lý refresh token.
 * Implementation: RefreshTokenServiceImpl
 */
public interface RefreshTokenService {

    /** Tạo refresh token mới (UUID, hạn 30 ngày) và lưu vào DB */
    RefreshToken createToken(String userId, String deviceInfo, String ipAddress);

    /** Tìm token còn hiệu lực (chưa revoked + chưa hết hạn) */
    Optional<RefreshToken> findValidToken(String token);

    /** Lấy tất cả phiên đăng nhập của user (để hiển thị quản lý thiết bị) */
    List<RefreshToken> getSessionsByUserId(String userId);

    /** Thu hồi 1 token cụ thể — logout 1 thiết bị */
    boolean revokeToken(String token);

    /** Xóa toàn bộ token của user — logout tất cả thiết bị */
    void revokeAllTokens(String userId);
}
