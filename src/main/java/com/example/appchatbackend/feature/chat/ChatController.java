package com.example.appchatbackend.feature.chat;

import com.example.appchatbackend.feature.chat.dto.ChatMessageRequest;
import com.example.appchatbackend.feature.chat.dto.ChatNotification;
import com.example.appchatbackend.feature.chat.dto.ReadReceiptEvent;
import com.example.appchatbackend.feature.chat.dto.TypingEvent;
import com.example.appchatbackend.feature.conversation.ConversationService;
import com.example.appchatbackend.feature.message.Message;
import com.example.appchatbackend.feature.message.MessageService;
import com.example.appchatbackend.feature.user.User;
import com.example.appchatbackend.feature.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;

@Controller
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final MessageService messageService;
    private final ConversationService conversationService;
    private final UserRepository userRepository;
    private final RedisMessagePublisher redisPublisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final OnlineStatusService onlineStatusService;

    public ChatController(MessageService messageService,
                          ConversationService conversationService,
                          UserRepository userRepository,
                          RedisMessagePublisher redisPublisher,
                          SimpMessagingTemplate messagingTemplate,
                          OnlineStatusService onlineStatusService) {
        this.messageService = messageService;
        this.conversationService = conversationService;
        this.userRepository = userRepository;
        this.redisPublisher = redisPublisher;
        this.messagingTemplate = messagingTemplate;
        this.onlineStatusService = onlineStatusService;
    }

    /**
     * Client gửi tin nhắn: /app/chat.send
     * Broadcast qua Redis → /topic/conversation/{conversationId}
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        String senderId = principal.getName(); // userId từ WebSocketAuthInterceptor

        if (!conversationService.isParticipant(request.getConversationId(), senderId)) {
            log.warn("User {} cố gửi tin vào conversation {} nhưng không phải thành viên", senderId, request.getConversationId());
            return;
        }

        User sender = userRepository.findById(senderId).orElse(null);
        if (sender == null) return;

        Message message = Message.builder()
                .conversationId(request.getConversationId())
                .senderId(senderId)
                .senderDisplayName(sender.getDisplayName())
                .messageType(request.getMessageType())
                .content(request.getContent())
                .media(request.getMedia())
                .replyToMessageId(request.getReplyToMessageId())
                .build();

        Message saved = messageService.sendMessage(message);
        conversationService.updateLastMessage(request.getConversationId(), saved);
        onlineStatusService.refreshOnline(senderId);

        // Publish lên Redis để tất cả instance broadcast
        redisPublisher.publish(request.getConversationId(),
                ChatNotification.builder()
                        .type(ChatNotification.NotificationType.NEW_MESSAGE)
                        .conversationId(request.getConversationId())
                        .data(saved)
                        .build()
        );
    }

    /**
     * Client gửi sự kiện đang gõ: /app/chat.typing
     * Broadcast thẳng (không lưu DB, không qua Redis)
     */
    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingEvent event, Principal principal) {
        String senderId = principal.getName();

        if (!conversationService.isParticipant(event.getConversationId(), senderId)) return;

        event.setSenderId(senderId);

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + event.getConversationId(),
                ChatNotification.builder()
                        .type(ChatNotification.NotificationType.TYPING)
                        .conversationId(event.getConversationId())
                        .data(event)
                        .build()
        );
    }

    /**
     * Client đánh dấu đã đọc: /app/chat.read
     * Lưu DB và broadcast read receipt
     */
    @MessageMapping("/chat.read")
    public void markAsRead(@Payload TypingEvent event, Principal principal) {
        String userId = principal.getName();
        String conversationId = event.getConversationId();

        if (!conversationService.isParticipant(conversationId, userId)) return;

        messageService.markAsRead(conversationId, userId);

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId,
                ChatNotification.builder()
                        .type(ChatNotification.NotificationType.READ_RECEIPT)
                        .conversationId(conversationId)
                        .data(new ReadReceiptEvent(conversationId, userId, Instant.now()))
                        .build()
        );
    }
}
