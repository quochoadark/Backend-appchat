package com.example.appchatbackend.feature.chat;

import com.example.appchatbackend.feature.chat.dto.ChatNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class RedisMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(RedisMessagePublisher.class);
    private static final String CHANNEL_PREFIX = "chat:conversation:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisMessagePublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(String conversationId, ChatNotification notification) {
        try {
            String json = objectMapper.writeValueAsString(notification);
            redisTemplate.convertAndSend(CHANNEL_PREFIX + conversationId, json);
        } catch (Exception e) {
            log.error("Lỗi serialize ChatNotification: {}", e.getMessage());
        }
    }
}
