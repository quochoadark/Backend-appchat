package com.example.appchatbackend.feature.auth;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenService {

    RefreshToken createToken(String userId, String deviceInfo, String ipAddress);

    Optional<RefreshToken> findValidToken(String token);

    List<RefreshToken> getSessionsByUserId(String userId);

    boolean revokeToken(String token);

    void revokeAllTokens(String userId);
}
