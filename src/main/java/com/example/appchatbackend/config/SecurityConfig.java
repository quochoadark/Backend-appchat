package com.example.appchatbackend.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.spec.SecretKeySpec;

/**
 * SecurityConfig — cấu hình bảo mật trung tâm của toàn bộ ứng dụng.
 *
 * @EnableWebSecurity: bật Spring Security, thay thế toàn bộ cấu hình mặc định.
 * Định nghĩa 4 bean cốt lõi: SecurityFilterChain, JwtDecoder, JwtEncoder,
 * AuthenticationManager, PasswordEncoder.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Secret key dùng để ký và xác thực JWT, đọc từ application.properties (jwt.secret).
     * Phải đủ dài (>= 256 bit với HS256) và được giữ bí mật tuyệt đối.
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * SecurityFilterChain — cấu hình chuỗi filter bảo mật cho HTTP request.
     *
     * - csrf disable: không cần CSRF vì dùng JWT stateless (không có session/cookie).
     * - cors: kích hoạt CORS, dùng cấu hình từ WebMvcConfig.addCorsMappings.
     * - sessionManagement STATELESS: server không tạo session, mỗi request tự xác thực bằng JWT.
     * - authorizeHttpRequests:
     *     + permitAll: các endpoint public (đăng nhập, WebSocket, file tĩnh).
     *     + anyRequest().authenticated(): mọi endpoint còn lại yêu cầu JWT hợp lệ.
     * - oauth2ResourceServer jwt: Spring tự động xác thực Bearer token bằng JwtDecoder bean.
     * - addFilterBefore: chèn JwtBlacklistFilter trước filter xác thực,
     *   để chặn token đã logout trước khi Spring kịp xác thực.
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtBlacklistFilter jwtBlacklistFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/auth/**", "/ws/**", "/files/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {
                        }))
                .addFilterBefore(jwtBlacklistFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * JwtDecoder — dùng để GIẢI MÃ và XÁC THỰC JWT nhận từ client.
     *
     * Dùng thuật toán HMAC-SHA256 (HS256) với secret key đối xứng.
     * Spring Security tự động gọi bean này khi có request mang Bearer token.
     */
    @Bean
    JwtDecoder jwtDecoder() {
        SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(), MacAlgorithm.HS256.getName());
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * JwtEncoder — dùng để TẠO (ký) JWT khi user đăng nhập thành công.
     *
     * ImmutableSecret: bọc secret key thành JWK source cho NimbusJwtEncoder.
     * Được inject vào AuthService để tạo access token sau khi xác thực thành công.
     */
    @Bean
    JwtEncoder jwtEncoder() {
        SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(), MacAlgorithm.HS256.getName());
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    /**
     * AuthenticationManager — điều phối quá trình xác thực username/password.
     *
     * Được dùng trong AuthService khi xử lý login:
     * gọi authenticate() → Spring tự load user qua CustomUserDetailsService
     * → so sánh password bằng PasswordEncoder.
     */
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * PasswordEncoder — mã hóa mật khẩu bằng BCrypt trước khi lưu vào DB.
     *
     * BCrypt tự động thêm salt ngẫu nhiên, chống rainbow table attack.
     * Được dùng khi đăng ký (encode) và khi login (matches để so sánh).
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
