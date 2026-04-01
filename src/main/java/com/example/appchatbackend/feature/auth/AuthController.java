package com.example.appchatbackend.feature.auth;

import com.example.appchatbackend.feature.auth.dto.request.LoginRequest;
import com.example.appchatbackend.feature.auth.dto.response.LoginResponse;
import com.example.appchatbackend.helper.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${jwt.expiration}")
    private long expiration;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
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
}
