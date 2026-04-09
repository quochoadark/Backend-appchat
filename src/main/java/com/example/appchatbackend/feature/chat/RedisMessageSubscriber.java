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

/**
 * RedisMessageSubscriber — SUBSCRIBE va xu ly message tu Redis Pub/Sub.
 *
 * Day la "nua nhan" cua Pub/Sub. Duoc dang ky trong RedisConfig
 * de lang nghe tat ca channel khop pattern "chat:conversation:*".
 *
 * Khi co message tu Redis:
 * 1. Deserialize JSON → ChatNotification
 * 2. Dung SimpMessagingTemplate broadcast toi STOMP topic
 *    "/topic/conversation/{conversationId}"
 * 3. Tat ca WebSocket client dang subscribe topic do nhan duoc thong bao
 *
 * Implements MessageListener: interface cua Spring Data Redis cho Pub/Sub listener.
 * Duoc goi tu background thread cua RedisMessageListenerContainer.
 */
@Service
public class RedisMessageSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageSubscriber.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public RedisMessageSubscriber(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Duoc goi tu dong moi khi co message den tren bat ky channel khop "chat:conversation:*".
     * @param message  message tu Redis (body la JSON bytes)
     * @param pattern  pattern da match (vd: "chat:conversation:*")
     */
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
