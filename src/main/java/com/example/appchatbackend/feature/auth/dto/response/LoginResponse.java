package com.example.appchatbackend.feature.auth.dto.response;

/**
 * LoginResponse — DTO trả về cho client sau khi đăng nhập hoặc refresh token thành công.
 *
 * - accessToken: JWT dùng cho các API request (đặt vào header "Authorization: Bearer ...")
 * - tokenType: luôn là "Bearer"
 * - expiresIn: thời gian sống của access token (milliseconds), client dùng để biết lúc cần refresh
 * - refreshToken: UUID dùng để lấy access token mới khi hết hạn (POST /auth/refresh)
 */
public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String refreshToken;

    public LoginResponse(String accessToken, String tokenType, long expiresIn, String refreshToken) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
