package com.example.appchatbackend.feature.auth;

import com.example.appchatbackend.feature.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * JwtService — tạo (ký) JWT access token sau khi user xác thực thành công.
 *
 * Chỉ có 1 nghiệp vụ duy nhất: generateToken().
 * Được AuthController gọi sau khi AuthenticationManager xác thực thành công.
 */
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final UserRepository userRepository;

    /** Thời gian sống của access token (ms), đọc từ application.properties (jwt.expiration) */
    @Value("${jwt.expiration}")
    private long expiration;

    public JwtService(JwtEncoder jwtEncoder, UserRepository userRepository) {
        this.jwtEncoder = jwtEncoder;
        this.userRepository = userRepository;
    }

    /**
     * Tạo JWT access token từ thông tin xác thực.
     *
     * Các claims được nhúng vào token:
     * - subject: email của user (dùng để định danh)
     * - issuedAt: thời điểm phát hành
     * - expiresAt: thời điểm hết hạn
     * - roles: danh sách quyền (vd: "ROLE_USER")
     * - userId: ID của user trong MongoDB — lưu sẵn để controller không phải query DB mỗi request
     *
     * Thuật toán ký: HMAC-SHA256 (HS256) với secret key từ SecurityConfig.
     */
    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();
        String email = authentication.getName();

        // Lưu userId vào claim để tránh query DB mỗi request
        String userId = userRepository.findByEmail(email)
                .map(u -> u.getId())
                .orElse("");

        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(email)
                .issuedAt(now)
                .expiresAt(now.plusMillis(expiration))
                .claim("roles", roles)
                .claim("userId", userId)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
