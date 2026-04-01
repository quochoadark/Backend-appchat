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
