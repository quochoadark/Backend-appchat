package com.example.appchatbackend.config;

// --- CÁC IMPORT LIÊN QUAN ĐẾN SERVICE VÀ FILTER ---
import com.example.appchatbackend.services.TokenBlacklistService; // Service kiểm tra token trong Redis
import jakarta.servlet.FilterChain; // Chuỗi các bộ lọc của hệ thống mạng
import jakarta.servlet.ServletException; // Ngoại lệ liên quan đến Servlet
import jakarta.servlet.http.HttpServletRequest; // Yêu cầu từ Client gửi lên
import jakarta.servlet.http.HttpServletResponse; // Phản hồi trả về cho Client
import org.springframework.http.HttpStatus; // Chứa mã trạng thái (ví dụ 401 Unauthorized)
import org.springframework.stereotype.Component; // Khai báo class này là một Bean để Spring quản lý
import org.springframework.web.filter.OncePerRequestFilter; // Đảm bảo bộ lọc này chỉ chạy 1 lần cho mỗi Request

import java.io.IOException;

/**
 * JwtBlacklistFilter — Màng lọc HTTP dùng để chặn các Token (JWT) đã bị đăng xuất.
 *
 * Extends OncePerRequestFilter: Kế thừa lớp này để đảm bảo rằng filter chỉ chạy đúng 1 lần duy nhất cho mỗi Request
 * (tránh trường hợp bị gọi lặp lại nhiều lần do cơ chế forward nội bộ của Spring).
 *
 * VẤN ĐỀ BẢO MẬT:
 * - JWT mặc định là "stateless" (không lưu trạng thái ở server). Nghĩa là server cấp token xong là quên.
 * - Khi user bấm "Đăng xuất" (Logout), Server không có cách nào thu hồi JWT lại từ phía client,
 *   token đó vẫn sẽ có hiệu lực cho đến khi tự hết hạn (hết thời gian).
 * 
 * GIẢI PHÁP:
 * - Khi user đăng xuất, lưu Token đó vào một "Danh sách đen" (Blacklist) trong Redis.
 * - Màng lọc này (Filter) sẽ đứng gác ở cửa. Mỗi khi có Request gửi tới kèm Token, nó sẽ lấy Token
 *   đem hỏi Redis xem có nằm trong Blacklist không. Nếu có thì đuổi về (trả lỗi 401).
 *
 * Filter này sẽ được cài đặt để chạy TRƯỚC UsernamePasswordAuthenticationFilter trong SecurityConfig.
 */
@Component
public class JwtBlacklistFilter extends OncePerRequestFilter {

    // Service thao tác với Redis Blacklist
    private final TokenBlacklistService tokenBlacklistService;

    // Tự động tiêm Service vào Filter
    public JwtBlacklistFilter(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }

    /**
     * Hàm lõi của Filter — Chạy mỗi khi có 1 Request HTTP đi vào Server.
     *
     * Luồng xử lý chi tiết:
     * 1. Nhìn vào tiêu đề (header) "Authorization" của request.
     * 2. Nếu thấy có chữ "Bearer " (nghĩa là có gắn Token) → Lấy Token đó ra.
     * 3. Gửi Token cho BlacklistService hỏi xem token này bị ban chưa.
     * 4. Nếu đã bị ban → Ghi lỗi 401 vào Response, in thông báo, và LẬP TỨC CHẶN LẠI (không cho đi tiếp).
     * 5. Nếu an toàn → Cho phép đi tiếp qua các màng lọc tiếp theo (filterChain.doFilter).
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
                
        // Bước 1: Lấy đoạn text trong header "Authorization"
        String authHeader = request.getHeader("Authorization");
        
        // Bước 2: Kiểm tra xem có bắt đầu bằng "Bearer " không
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Cắt 7 ký tự đầu tiên ("Bearer ") để lấy ra đoạn mã Token thuần
            String token = authHeader.substring(7);
            
            // Bước 3: Tra cứu Token trong Redis Blacklist
            if (tokenBlacklistService.isBlacklisted(token)) {
                // Bước 4: Token nằm trong sổ đen
                // Đặt HTTP Status code là 401 (Không được phép)
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                // Khai báo kiểu trả về là JSON
                response.setContentType("application/json;charset=UTF-8");
                // Bắn trực tiếp thông báo lỗi dạng JSON về cho Client
                response.getWriter().write("{\"message\":\"Token đã bị vô hiệu hóa do đăng xuất, vui lòng đăng nhập lại\"}");
                
                // Dừng lại ngay lập tức (Lệnh return giúp ngắt luồng, không chạy lệnh filterChain bên dưới)
                return;
            }
        }
        
        // Bước 5: Nếu không có token, hoặc token sạch, thì cho phép request đi tiếp vào hệ thống
        filterChain.doFilter(request, response);
    }
}
