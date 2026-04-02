package com.example.appchatbackend.feature.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklist(String token, Instant expiredAt) {
        Duration ttl = Duration.between(Instant.now(), expiredAt);
        if (!ttl.isNegative()) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "1", ttl);
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
