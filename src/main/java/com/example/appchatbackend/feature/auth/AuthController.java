package com.example.appchatbackend.feature.auth;

import com.example.appchatbackend.exception.ResourceNotFoundException;
import com.example.appchatbackend.feature.auth.dto.request.LoginRequest;
import com.example.appchatbackend.feature.auth.dto.request.RefreshRequest;
import com.example.appchatbackend.feature.auth.dto.request.RegisterRequest;
import com.example.appchatbackend.feature.auth.dto.response.LoginResponse;
import com.example.appchatbackend.feature.user.User;
import com.example.appchatbackend.feature.user.UserRepository;
import com.example.appchatbackend.feature.user.UserService;
import com.example.appchatbackend.helper.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;

    @Value("${jwt.expiration}")
    private long expiration;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
                          UserService userService, UserRepository userRepository,
                          TokenBlacklistService tokenBlacklistService,
                          RefreshTokenService refreshTokenService,
                          UserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.tokenBlacklistService = tokenBlacklistService;
        this.refreshTokenService = refreshTokenService;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        String accessToken = jwtService.generateToken(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "email", request.getEmail()));

        String deviceInfo = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();
        RefreshToken refreshToken = refreshTokenService.createToken(user.getId(), deviceInfo, ipAddress);

        LoginResponse loginResponse = new LoginResponse(
                accessToken, "Bearer", expiration, refreshToken.getToken());
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", loginResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshRequest request,
            HttpServletRequest httpRequest) {
        RefreshToken oldToken = refreshTokenService.findValidToken(request.getRefreshToken())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ hoặc đã hết hạn"));

        User user = userRepository.findById(oldToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", oldToken.getUserId()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        String newAccessToken = jwtService.generateToken(auth);

        // Rotate refresh token
        refreshTokenService.revokeToken(request.getRefreshToken());
        String deviceInfo = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();
        RefreshToken newRefreshToken = refreshTokenService.createToken(user.getId(), deviceInfo, ipAddress);

        LoginResponse response = new LoginResponse(
                newAccessToken, "Bearer", expiration, newRefreshToken.getToken());
        return ResponseEntity.ok(ApiResponse.success("Làm mới token thành công", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            Authentication authentication,
            HttpServletRequest request,
            @RequestBody(required = false) RefreshRequest refreshRequest) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Jwt jwt = (Jwt) authentication.getPrincipal();
            tokenBlacklistService.blacklist(token, jwt.getExpiresAt());
        }
        if (refreshRequest != null && refreshRequest.getRefreshToken() != null) {
            refreshTokenService.revokeToken(refreshRequest.getRefreshToken());
        }
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> me(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String email = jwt.getSubject();
        User user = userService.findByEmail(email);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin thành công", user));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@Valid @RequestBody RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(request.getPassword());
        user.setDisplayName(request.getDisplayName());

        User created = userService.create(user);
        return ResponseEntity.ok(ApiResponse.success("Đăng ký thành công", created));
    }
}
