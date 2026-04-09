package com.example.appchatbackend.feature.chat;

import com.example.appchatbackend.feature.chat.dto.ChatNotification;
import com.example.appchatbackend.feature.user.User;
import com.example.appchatbackend.feature.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.Instant;
import java.util.Optional;

/**
 * WebSocketEventListener — lang nghe cac su kien vong doi cua WebSocket session.
 *
 * Xu ly 2 su kien chinh:
 * - SessionConnectedEvent: user da ket noi thanh cong → chi log debug
 * - SessionDisconnectEvent: user ngat ket noi → dat offline trong Redis + MongoDB
 *   + broadcast USER_OFFLINE toi /topic/presence de cac client khac cap nhat UI
 *
 * Luu y: WebSocketAuthInterceptor da xu ly setOnline() khi CONNECT.
 * Listener nay chi xu ly DISCONNECT (bao gom ca truong hop mat mang dot ngot).
 * Truong hop mat mang: Redis TTL 5 phut se tu xoa key, nhung MongoDB can duoc cap nhat
 * ngay khi DisconnectEvent duoc fire (Spring WebSocket phat hien qua heartbeat).
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final OnlineStatusService onlineStatusService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    public WebSocketEventListener(OnlineStatusService onlineStatusService,
                                   SimpMessagingTemplate messagingTemplate,
                                   UserRepository userRepository) {
        this.onlineStatusService = onlineStatusService;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        log.debug("WebSocket connected: {}", event.getUser() != null ? event.getUser().getName() : "unknown");
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        if (user == null) return;

        String userId = user.getName();
        onlineStatusService.setOffline(userId);

        // Đồng bộ isOnline + lastSeenAt vào MongoDB
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isPresent()) {
            User u = optUser.get();
            u.setIsOnline(false);
            u.setLastSeenAt(Instant.now());
            userRepository.save(u);
        }

        log.debug("WebSocket disconnected: {}", userId);

        messagingTemplate.convertAndSend("/topic/presence",
                ChatNotification.builder()
                        .type(ChatNotification.NotificationType.USER_OFFLINE)
                        .data(userId)
                        .build()
        );
    }
}
