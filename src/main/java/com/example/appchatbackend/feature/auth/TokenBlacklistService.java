package com.example.appchatbackend.feature.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * TokenBlacklistService — lưu trữ và kiểm tra JWT bị vô hiệu hóa (blacklist) trong Redis.
 *
 * Vấn đề: JWT stateless nên server không thể "thu hồi" token khi user logout.
 * Giải pháp: khi logout, lưu token vào Redis với TTL = thời gian còn lại của token.
 * Sau khi token hết hạn tự nhiên, Redis cũng tự xóa key → không tốn bộ nhớ mãi mãi.
 *
 * Key Redis: "blacklist:token:{jwt_string}" → value "1"
 */
@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Đưa token vào blacklist với TTL = thời gian còn lại đến khi token hết hạn.
     * Nếu token đã hết hạn rồi (ttl âm) thì bỏ qua — không cần blacklist.
     */
    public void blacklist(String token, Instant expiredAt) {
        Duration ttl = Duration.between(Instant.now(), expiredAt);
        if (!ttl.isNegative()) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "1", ttl);
        }
    }

    /**
     * Kiểm tra token có trong blacklist không.
     * Được JwtBlacklistFilter gọi trước mỗi request có Bearer token.
     */
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
