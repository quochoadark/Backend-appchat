package com.example.appchatbackend.feature.chat;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * OnlineStatusService — quan ly trang thai online/offline cua user trong Redis.
 *
 * Chien luoc TTL (Time-To-Live):
 * - Khi user ket noi WebSocket → setOnline() → key Redis song 5 phut
 * - Moi lan user gui tin nhan → refreshOnline() → gia han them 5 phut
 * - Khi user disconnect → setOffline() → xoa key ngay lap tuc
 * - Neu user mat mang dot ngot (khong co SessionDisconnectEvent) → key tu het han sau 5 phut
 *   → tranh user bi hien "online mai mai" khi thuc ra da offline
 *
 * Key Redis: "user:online:{userId}" → value "1" (chi can biet ton tai hay khong)
 */
@Service
public class OnlineStatusService {

    private static final String KEY_PREFIX = "user:online:";
    private static final long TTL_SECONDS = 300; // 5 phut

    private final StringRedisTemplate redisTemplate;

    public OnlineStatusService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** Dat user online, key song 5 phut */
    public void setOnline(String userId) {
        redisTemplate.opsForValue().set(KEY_PREFIX + userId, "1", TTL_SECONDS, TimeUnit.SECONDS);
    }

    /** Dat user offline ngay lap tuc (khi disconnect WebSocket) */
    public void setOffline(String userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }

    /** Kiem tra user co dang online khong (key con ton tai trong Redis) */
    public boolean isOnline(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + userId));
    }

    /** Gia han TTL them 5 phut khi user gui tin nhan — giu trang thai online active */
    public void refreshOnline(String userId) {
        redisTemplate.expire(KEY_PREFIX + userId, TTL_SECONDS, TimeUnit.SECONDS);
    }
}
