package com.example.appchatbackend.config;

// --- CÁC IMPORT LIÊN QUAN ĐẾN SPRING WEBSOCKET ---
import org.springframework.context.annotation.Configuration; // Đánh dấu đây là file cấu hình của Spring Boot
import org.springframework.messaging.simp.config.ChannelRegistration; // Cấu hình các kênh truyền tin nhắn (inbound/outbound)
import org.springframework.messaging.simp.config.MessageBrokerRegistry; // Đăng ký trung tâm xử lý tin nhắn (Message Broker)
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker; // Kích hoạt tính năng WebSocket
import org.springframework.web.socket.config.annotation.StompEndpointRegistry; // Đăng ký đường dẫn kết nối WebSocket (Endpoint)
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer; // Interface chứa các hàm cấu hình WebSocket

/**
 * WebSocketConfig — File cấu hình giao thức WebSocket và STOMP cho ứng dụng chat thời gian thực (real-time).
 *
 * STOMP (Simple Text Oriented Messaging Protocol) là một giao thức định dạng tin nhắn chạy trên nền WebSocket.
 * Nó biến WebSocket (vốn chỉ là một đường ống rỗng) thành một hệ thống có kênh (topic), có hàng đợi (queue) giống như gửi thư.
 */
@Configuration // Nói cho Spring biết đây là class cấu hình để nó tự động load khi khởi động
@EnableWebSocketMessageBroker // Bật tính năng Message Broker (người môi giới tin nhắn) dựa trên WebSocket
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // Khai báo Interceptor dùng để chặn luồng kết nối WebSocket lại và kiểm tra Token (JWT)
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    // Constructor injection: Tự động tiêm WebSocketAuthInterceptor vào đây
    public WebSocketConfig(WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    /**
     * Cấu hình Message Broker — Xác định các "địa chỉ" (prefix) mà client có thể đăng ký theo dõi (subscribe)
     * hoặc gửi tin nhắn lên.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 1. Kích hoạt broker nội bộ (in-memory) để xử lý các tin nhắn có tiền tố là "/topic", "/queue", "/presence"
        // - "/topic"  : Dùng để phát sóng (broadcast) cho nhiều người (Ví dụ: chat nhóm).
        // - "/queue"  : Dùng để gửi tin nhắn riêng lẻ 1-1 (Point-to-point).
        // - "/presence": Dùng để báo trạng thái online/offline.
        config.enableSimpleBroker("/topic", "/queue", "/presence");
        
        // 2. Tiền tố cho các tin nhắn được client gửi LÊN server
        // Ví dụ: Client muốn gọi hàm @MessageMapping("/chat.send") trong Controller, 
        // thì phải gửi tin nhắn tới đường dẫn "/app/chat.send"
        config.setApplicationDestinationPrefixes("/app");
        
        // 3. Tiền tố dùng để gửi tin nhắn cụ thể cho MỘT user thông qua hàm SimpMessagingTemplate
        // Ví dụ: gửi vào "/user/123/queue/messages" thì chỉ user có id=123 mới nhận được
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Đăng ký đường dẫn (Endpoint) để client bắt đầu mở kết nối WebSocket với Server.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Định nghĩa đường dẫn kết nối là "/ws". 
        // Trong React/React Native, URL kết nối sẽ là: ws://domain.com/ws
        registry.addEndpoint("/ws")
                // Cho phép mọi tên miền (CORS) đều có thể kết nối vào WebSocket này
                .setAllowedOriginPatterns("*")
                // Kích hoạt SockJS. Nếu trình duyệt/mạng của client chặn WebSocket thuần, 
                // SockJS sẽ tự động lùi về các cách kết nối khác (như Long-Polling) để đảm bảo vẫn chat được.
                .withSockJS();
    }

    /**
     * Cấu hình kênh đầu vào (Inbound Channel) — nơi tiếp nhận mọi tin nhắn từ Client gửi lên Server.
     * 
     * Gắn thêm WebSocketAuthInterceptor vào luồng này. Mọi tin nhắn (bao gồm cả lệnh CONNECT lúc mới mở)
     * đều phải đi qua Interceptor này để kiểm tra xem mã JWT có hợp lệ hay không.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Thêm màng lọc xác thực vào kênh nhận tin
        registration.interceptors(webSocketAuthInterceptor);
    }
}
