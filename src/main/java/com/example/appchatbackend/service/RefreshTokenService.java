package com.example.appchatbackend.service;

import com.example.appchatbackend.model.RefreshToken;
import com.example.appchatbackend.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public RefreshToken createToken(String userId, String deviceInfo, String ipAddress) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(userId)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .isRevoked(false)
                .build();
        return refreshTokenRepository.save(token);
    }

    public Optional<RefreshToken> findValidToken(String token) {
        return refreshTokenRepository.findByTokenAndIsRevokedFalse(token)
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()));
    }

    public List<RefreshToken> getSessionsByUserId(String userId) {
        return refreshTokenRepository.findByUserId(userId);
    }

    // Logout 1 thiết bị — thu hồi token cụ thể
    public boolean revokeToken(String token) {
        return refreshTokenRepository.findByTokenAndIsRevokedFalse(token).map(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
            return true;
        }).orElse(false);
    }

    // Logout tất cả thiết bị — xóa toàn bộ token của user
    public void revokeAllTokens(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
