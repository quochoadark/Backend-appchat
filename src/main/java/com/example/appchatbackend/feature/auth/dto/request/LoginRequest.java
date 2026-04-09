package com.example.appchatbackend.feature.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * LoginRequest — DTO nhận dữ liệu từ client khi đăng nhập.
 * POST /auth/login
 *
 * Validation được Spring tự động kiểm tra nhờ @Valid ở controller.
 * Nếu sai → GlobalExceptionHandler bắt MethodArgumentNotValidException → 400.
 */
public class LoginRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
