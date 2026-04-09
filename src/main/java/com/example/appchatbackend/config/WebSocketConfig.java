package com.example.appchatbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocketConfig — cấu hình WebSocket + STOMP cho ứng dụng chat real-time.
 *
 * @EnableWebSocketMessageBroker: bật tính năng WebSocket với message broker,
 * cho phép server và client trao đổi message theo mô hình Pub/Sub qua giao thức STOMP.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    public WebSocketConfig(WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    /**
     * Cấu hình message broker — xác định các "địa chỉ" mà client có thể subscribe hoặc gửi message.
     *
     * - enableSimpleBroker("/topic", "/queue", "/presence"):
     *     Kích hoạt broker nội bộ (in-memory). Client subscribe vào các prefix này để nhận message.
     *     /topic  → broadcast (1 server gửi → nhiều client nhận, vd: tin nhắn nhóm)
     *     /queue  → point-to-point (server gửi tới 1 client cụ thể)
     *     /presence → trạng thái online/offline
     *
     * - setApplicationDestinationPrefixes("/app"):
     *     Message từ client gửi lên server phải có prefix /app (vd: /app/chat/send).
     *     Server xử lý bằng @MessageMapping.
     *
     * - setUserDestinationPrefix("/user"):
     *     Cho phép server gửi message tới 1 user cụ thể qua /user/{userId}/queue/...
     *     (dùng với SimpMessagingTemplate.convertAndSendToUser)
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue", "/presence");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Đăng ký endpoint WebSocket mà client kết nối vào.
     *
     * - addEndpoint("/ws"): client kết nối tới ws://server/ws
     * - setAllowedOriginPatterns("*"): cho phép mọi origin (tránh lỗi CORS khi dùng WebSocket)
     * - withSockJS(): bật SockJS fallback — nếu client không hỗ trợ WebSocket thuần,
     *   tự động dùng long-polling hoặc các transport khác
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * Gắn WebSocketAuthInterceptor vào channel inbound (message từ client → server).
     *
     * Mọi message gửi từ client đều đi qua interceptor này trước khi được xử lý.
     * Interceptor sẽ xác thực JWT khi client gửi lệnh CONNECT.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
