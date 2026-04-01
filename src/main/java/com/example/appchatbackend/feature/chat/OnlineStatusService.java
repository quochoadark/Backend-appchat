package com.example.appchatbackend.feature.chat;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class OnlineStatusService {

    private static final String KEY_PREFIX = "user:online:";
    private static final long TTL_SECONDS = 300; // 5 phút

    private final StringRedisTemplate redisTemplate;

    public OnlineStatusService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setOnline(String userId) {
        redisTemplate.opsForValue().set(KEY_PREFIX + userId, "1", TTL_SECONDS, TimeUnit.SECONDS);
    }

    public void setOffline(String userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }

    public boolean isOnline(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + userId));
    }

    // Gọi khi user gửi tin nhắn để làm mới TTL
    public void refreshOnline(String userId) {
        redisTemplate.expire(KEY_PREFIX + userId, TTL_SECONDS, TimeUnit.SECONDS);
    }
}
