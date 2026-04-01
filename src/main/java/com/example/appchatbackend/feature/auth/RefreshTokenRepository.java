package com.example.appchatbackend.feature.auth;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

    // Tìm token hợp lệ (chưa bị thu hồi)
    Optional<RefreshToken> findByTokenAndIsRevokedFalse(String token);

    // Lấy tất cả phiên đăng nhập của 1 user
    List<RefreshToken> findByUserId(String userId);

    // Thu hồi tất cả token của 1 user (logout tất cả thiết bị)
    void deleteByUserId(String userId);
}
