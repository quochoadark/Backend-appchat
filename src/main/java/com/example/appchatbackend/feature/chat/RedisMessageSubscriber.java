package com.example.appchatbackend.feature.chat;

import com.example.appchatbackend.feature.chat.dto.ChatNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Service
public class RedisMessageSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageSubscriber.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public RedisMessageSubscriber(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            ChatNotification notification = objectMapper.readValue(json, ChatNotification.class);
            messagingTemplate.convertAndSend(
                    "/topic/conversation/" + notification.getConversationId(),
                    notification
            );
        } catch (Exception e) {
            log.error("Lỗi xử lý Redis message: {}", e.getMessage());
        }
    }
}
