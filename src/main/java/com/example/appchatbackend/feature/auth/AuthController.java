package com.example.appchatbackend.feature.auth;

import com.example.appchatbackend.feature.auth.dto.request.LoginRequest;
import com.example.appchatbackend.feature.auth.dto.request.RegisterRequest;
import com.example.appchatbackend.feature.auth.dto.response.LoginResponse;
import com.example.appchatbackend.feature.user.User;
import com.example.appchatbackend.feature.user.UserService;
import com.example.appchatbackend.helper.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${jwt.expiration}")
    private long expiration;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
                          UserService userService, TokenBlacklistService tokenBlacklistService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String accessToken = jwtService.generateToken(authentication);
        LoginResponse loginResponse = new LoginResponse(accessToken, "Bearer", expiration);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", loginResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication, HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Jwt jwt = (Jwt) authentication.getPrincipal();
            tokenBlacklistService.blacklist(token, jwt.getExpiresAt());
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
