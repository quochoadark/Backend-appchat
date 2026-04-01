package com.example.appchatbackend.feature.auth;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
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

    @Override
    public Optional<RefreshToken> findValidToken(String token) {
        Optional<RefreshToken> optToken = refreshTokenRepository.findByTokenAndIsRevokedFalse(token);
        if (optToken.isPresent() && optToken.get().getExpiresAt().isAfter(Instant.now())) {
            return optToken;
        }
        return Optional.empty();
    }

    @Override
    public List<RefreshToken> getSessionsByUserId(String userId) {
        return refreshTokenRepository.findByUserId(userId);
    }

    // Logout 1 thiết bị — thu hồi token cụ thể
    @Override
    public boolean revokeToken(String token) {
        Optional<RefreshToken> optToken = refreshTokenRepository.findByTokenAndIsRevokedFalse(token);
        if (optToken.isPresent()) {
            RefreshToken t = optToken.get();
            t.setRevoked(true);
            refreshTokenRepository.save(t);
            return true;
        }
        return false;
    }

    // Logout tất cả thiết bị — xóa toàn bộ token của user
    @Override
    public void revokeAllTokens(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
