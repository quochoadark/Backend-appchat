// Định nghĩa package chứa class này
package com.example.appchatbackend.controllers;

// --- CÁC IMPORT LIÊN QUAN ĐẾN DỊCH VỤ VÀ MODEL ---
import com.example.appchatbackend.services.TokenBlacklistService; // Dịch vụ chặn các token đã đăng xuất
import com.example.appchatbackend.services.RefreshTokenService; // Dịch vụ xử lý Refresh Token
import com.example.appchatbackend.models.RefreshToken; // Model của Refresh Token
import com.example.appchatbackend.services.JwtService; // Dịch vụ tạo và xác thực JWT

// --- CÁC IMPORT LIÊN QUAN ĐẾN LỖI, DTO VÀ NGƯỜI DÙNG ---
import com.example.appchatbackend.exception.ResourceNotFoundException; // Lỗi không tìm thấy tài nguyên
import com.example.appchatbackend.dtos.request.LoginRequest; // Dữ liệu đầu vào khi Đăng nhập
import com.example.appchatbackend.dtos.request.RefreshRequest; // Dữ liệu đầu vào khi làm mới Token
import com.example.appchatbackend.dtos.request.RegisterRequest; // Dữ liệu đầu vào khi Đăng ký
import com.example.appchatbackend.dtos.response.LoginResponse; // Cấu trúc trả về khi Đăng nhập thành công
import com.example.appchatbackend.models.User; // Model User
import com.example.appchatbackend.repositories.UserRepository; // Kho lưu trữ User (tương tác với CSDL)
import com.example.appchatbackend.services.UserService; // Dịch vụ quản lý User
import com.example.appchatbackend.helper.ApiResponse; // Helper để format Response chuẩn

// --- CÁC IMPORT SPRING BOOT & SECURITY ---
import jakarta.servlet.http.HttpServletRequest; // Lấy thông tin từ request (IP, User-Agent)
import jakarta.validation.Valid; // Hỗ trợ Validate dữ liệu đầu vào
import org.springframework.beans.factory.annotation.Value; // Lấy cấu hình từ application.properties
import org.springframework.http.HttpStatus; // Chứa các mã trạng thái HTTP (200, 401, 404,...)
import org.springframework.http.ResponseEntity; // Bọc Response HTTP
import org.springframework.security.authentication.AuthenticationManager; // Quản lý quá trình xác thực
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // Token dùng cho quá trình đăng nhập
import org.springframework.security.core.Authentication; // Lưu trữ thông tin người dùng đã xác thực
import org.springframework.security.core.userdetails.UserDetails; // Chứa thông tin tài khoản Spring Security
import org.springframework.security.core.userdetails.UserDetailsService; // Dịch vụ tải thông tin UserDetails
import org.springframework.security.oauth2.jwt.Jwt; // Đối tượng JWT của Spring Security
import org.springframework.web.bind.annotation.*; // Annotation của Spring Web
import org.springframework.web.server.ResponseStatusException; // Exception trả về HTTP Status tương ứng

/**
 * AuthController — xử lý các endpoint xác thực người dùng.
 *
 * Các Endpoints cung cấp:
 * - POST /auth/register  → Đăng ký tài khoản mới
 * - POST /auth/login     → Đăng nhập, trả về Access Token + Refresh Token
 * - POST /auth/refresh   → Lấy Access Token mới bằng Refresh Token
 * - POST /auth/logout    → Đăng xuất (Đưa Access Token vào blacklist và vô hiệu hóa Refresh Token)
 * - GET  /auth/me        → Lấy thông tin tài khoản đang đăng nhập hiện tại
 */
@RestController // Đánh dấu đây là REST API Controller
@RequestMapping("/auth") // Đặt đường dẫn gốc cho các API trong file này là /auth
public class AuthController {

    // Khai báo các Dependencies (Dịch vụ) cần thiết
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;

    // Lấy thời gian hết hạn của JWT từ file cấu hình (ví dụ: application.properties)
    @Value("${jwt.expiration}")
    private long expiration;

    // Constructor Injection: Spring tự động tiêm các dependencies vào Controller này
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

    /**
     * API Đăng nhập
     * Xác thực bằng email/password. Trả về JWT Access Token + Refresh Token.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request, // Nhận email và password từ request body
            HttpServletRequest httpRequest) { // HttpServletRequest dùng để lấy IP và thiết bị (User-Agent)

        // Thực hiện xác thực email và password bằng AuthenticationManager của Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        // Tạo JWT Access Token mới từ thông tin người dùng vừa xác thực thành công
        String accessToken = jwtService.generateToken(authentication);

        // Lấy đối tượng User từ CSDL dựa theo email. Nếu không thấy sẽ ném ra lỗi ResourceNotFoundException
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "email", request.getEmail()));

        // Lấy thông tin trình duyệt / thiết bị (User-Agent) và địa chỉ IP
        String deviceInfo = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();
        
        // Tạo một Refresh Token lưu vào Database cùng với ID người dùng, IP, thiết bị
        RefreshToken refreshToken = refreshTokenService.createToken(user.getId(), deviceInfo, ipAddress);

        // Tạo Response chứa chuỗi Access Token, loại (Bearer), thời hạn, và Refresh Token
        LoginResponse loginResponse = new LoginResponse(
                accessToken, "Bearer", expiration, refreshToken.getToken());
        
        // Trả về HTTP 200 (OK) với dữ liệu được định dạng chuẩn
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", loginResponse));
    }

    /**
     * API Làm mới Access Token (Refresh Token)
     * Cung cấp Refresh Token cũ để lấy Access Token và Refresh Token mới.
     * Sử dụng kỹ thuật "Refresh Token Rotation" để tăng cường bảo mật.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshRequest request, // Nhận Refresh Token từ client gửi lên
            HttpServletRequest httpRequest) {

        // Tìm Refresh Token trong CSDL xem có tồn tại và còn hạn không
        RefreshToken oldToken = refreshTokenService.findValidToken(request.getRefreshToken())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ hoặc đã hết hạn"));

        // Lấy thông tin User tương ứng với Refresh Token này
        User user = userRepository.findById(oldToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", oldToken.getUserId()));

        // Nạp thông tin UserDetails từ email
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        
        // Tạo Authentication Token tạm thời dựa trên UserDetails (không dùng mật khẩu vì đã xác minh bằng Refresh Token)
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        
        // Tạo Access Token mới cho lần đăng nhập tiếp theo
        String newAccessToken = jwtService.generateToken(auth);

        // ROTATION: Vô hiệu hóa (revoke) Refresh Token cũ để tránh bị tái sử dụng (chống tấn công replay)
        refreshTokenService.revokeToken(request.getRefreshToken());
        
        // Lấy IP và Thiết bị để tạo Refresh Token mới
        String deviceInfo = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();
        RefreshToken newRefreshToken = refreshTokenService.createToken(user.getId(), deviceInfo, ipAddress);

        // Trả về cả Access Token và Refresh Token mới
        LoginResponse response = new LoginResponse(
                newAccessToken, "Bearer", expiration, newRefreshToken.getToken());
        return ResponseEntity.ok(ApiResponse.success("Làm mới token thành công", response));
    }

    /**
     * API Đăng xuất
     * Xóa bỏ hiệu lực của Access Token và Refresh Token hiện tại.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            Authentication authentication, // Thông tin người dùng hiện tại đang gọi API
            HttpServletRequest request, // Lấy Header Authorization để lấy Access Token
            @RequestBody(required = false) RefreshRequest refreshRequest) { // Nhận Refresh Token nếu client gửi lên
        
        // Lấy chuỗi Access Token từ header "Authorization"
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Cắt chuỗi "Bearer " để lấy đúng token
            String token = authHeader.substring(7);
            // Ép kiểu Authentication về dạng Jwt để lấy ngày hết hạn
            Jwt jwt = (Jwt) authentication.getPrincipal();
            // Đưa Access Token vào danh sách đen (Blacklist) trong Redis cho đến khi nó tự hết hạn
            tokenBlacklistService.blacklist(token, jwt.getExpiresAt());
        }
        
        // Nếu client có gửi lên Refresh Token, hãy vô hiệu hóa nó ngay trong CSDL
        if (refreshRequest != null && refreshRequest.getRefreshToken() != null) {
            refreshTokenService.revokeToken(refreshRequest.getRefreshToken());
        }
        
        // Trả về thành công
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }

    /**
     * API Lấy thông tin tài khoản đang đăng nhập
     * Chỉ những ai mang Access Token hợp lệ mới gọi được.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> me(Authentication authentication) {
        // Lấy thông tin JWT từ Spring Security Context
        Jwt jwt = (Jwt) authentication.getPrincipal();
        // Lấy email (subject) từ trong JWT payload
        String email = jwt.getSubject();
        // Tìm User trong DB thông qua email
        User user = userService.findByEmail(email);
        
        // Trả về thông tin người dùng
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin thành công", user));
    }

    /**
     * API Đăng ký tài khoản mới
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@Valid @RequestBody RegisterRequest request) {
        // Tạo một đối tượng User rỗng
        User user = new User();
        // Gán thông tin từ request vào đối tượng user
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(request.getPassword()); // Tên biến là passwordHash nhưng Service sẽ lo việc mã hóa (Bcrypt)
        user.setDisplayName(request.getDisplayName());

        // Gọi Service thực thi việc lưu vào DB
        User created = userService.create(user);
        
        // Trả về thông báo thành công
        return ResponseEntity.ok(ApiResponse.success("Đăng ký thành công", created));
    }
}
