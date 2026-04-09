package com.example.appchatbackend.config;

import com.example.appchatbackend.feature.auth.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtBlacklistFilter — HTTP filter kiểm tra token có bị blacklist không.
 *
 * Extends OncePerRequestFilter: đảm bảo filter chỉ chạy đúng 1 lần mỗi request
 * (tránh bị gọi nhiều lần do forward nội bộ).
 *
 * Vấn đề cần giải quyết: JWT mặc định stateless — server không thể "thu hồi" token.
 * Khi user logout, token vẫn còn hiệu lực đến khi hết hạn.
 * Giải pháp: lưu token đã logout vào Redis blacklist, filter này kiểm tra mỗi request.
 *
 * Filter này được đăng ký vào SecurityFilterChain trong SecurityConfig,
 * chạy TRƯỚC UsernamePasswordAuthenticationFilter.
 */
@Component
public class JwtBlacklistFilter extends OncePerRequestFilter {

    private final TokenBlacklistService tokenBlacklistService;

    public JwtBlacklistFilter(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }

    /**
     * Logic chính của filter — chạy với mỗi HTTP request.
     *
     * Luồng xử lý:
     * 1. Lấy header "Authorization" từ request.
     * 2. Nếu có Bearer token → kiểm tra trong Redis blacklist.
     * 3. Nếu token đã bị blacklist → trả về 401 ngay, dừng filter chain.
     * 4. Nếu token hợp lệ (chưa bị blacklist) → cho đi tiếp qua filterChain.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (tokenBlacklistService.isBlacklisted(token)) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Token đã bị vô hiệu hóa, vui lòng đăng nhập lại\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
