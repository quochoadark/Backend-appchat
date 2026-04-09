package com.example.appchatbackend.config;

import com.example.appchatbackend.feature.chat.OnlineStatusService;
import com.example.appchatbackend.feature.user.User;
import com.example.appchatbackend.feature.user.UserRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WebSocketAuthInterceptor — interceptor xác thực JWT khi client kết nối WebSocket.
 *
 * Implements ChannelInterceptor: được gọi trước mỗi message đi vào inbound channel.
 * Nhiệm vụ chính: chặn lệnh CONNECT của STOMP, kiểm tra JWT trong header,
 * nếu hợp lệ thì gắn thông tin user vào session WebSocket.
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;
    private final OnlineStatusService onlineStatusService;

    public WebSocketAuthInterceptor(JwtDecoder jwtDecoder, UserRepository userRepository, OnlineStatusService onlineStatusService) {
        this.jwtDecoder = jwtDecoder;
        this.userRepository = userRepository;
        this.onlineStatusService = onlineStatusService;
    }

    /**
     * Chặn message trước khi gửi vào channel — đây là nơi xác thực diễn ra.
     *
     * Luồng xử lý khi client gửi lệnh STOMP CONNECT:
     * 1. Lấy header "Authorization" từ STOMP frame.
     * 2. Giải mã JWT → lấy email (subject).
     * 3. Tìm user trong DB theo email.
     * 4. Tạo Authentication object với userId làm principal
     *    → gắn vào WebSocket session để sau này dùng /user/{userId}/queue/...
     * 5. Đánh dấu user online trong Redis và MongoDB.
     *
     * Nếu token thiếu hoặc không hợp lệ → ném MessageDeliveryException → từ chối kết nối.
     * Các lệnh STOMP khác (SEND, SUBSCRIBE...) không bị chặn ở đây.
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new MessageDeliveryException("Thiếu Authorization header");
            }
            String token = authHeader.substring(7);
            try {
                Jwt jwt = jwtDecoder.decode(token);
                String email = jwt.getSubject();
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new MessageDeliveryException("Người dùng không tồn tại"));
                // Đặt userId làm principal name để dùng cho /user/{userId}/queue/...
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        user.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
                accessor.setUser(auth);
                onlineStatusService.setOnline(user.getId());
                // Đồng bộ isOnline vào MongoDB
                user.setIsOnline(true);
                userRepository.save(user);
            } catch (MessageDeliveryException e) {
                throw e;
            } catch (Exception e) {
                throw new MessageDeliveryException("Token không hợp lệ: " + e.getMessage());
            }
        }
        return message;
    }
}
