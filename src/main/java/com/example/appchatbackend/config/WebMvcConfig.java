package com.example.appchatbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * WebMvcConfig — cấu hình CORS toàn cục và endpoint phục vụ file upload.
 *
 * Kết hợp 2 vai trò trong 1 class:
 * - @Configuration + WebMvcConfigurer: cấu hình MVC (CORS)
 * - @RestController: cung cấp endpoint GET /files/{filename} để tải file
 */
@Configuration
@RestController
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Đường dẫn thư mục lưu file upload, đọc từ application.properties (upload.dir).
     * Ví dụ: upload.dir=./uploads
     */
    @Value("${upload.dir}")
    private String uploadDir;

    /**
     * Cấu hình CORS (Cross-Origin Resource Sharing) cho toàn bộ API.
     *
     * Cho phép frontend (chạy ở domain/port khác) gọi API mà không bị trình duyệt chặn.
     * - addMapping("/**"): áp dụng cho tất cả endpoints
     * - allowedOriginPatterns("*"): chấp nhận mọi origin (dùng patterns thay vì origins
     *   vì allowCredentials và wildcard origin không dùng chung được)
     * - allowedMethods: các HTTP method được phép
     * - exposedHeaders: cho phép client đọc các header này từ response
     *   (Authorization để lấy token mới, Content-Disposition để lấy tên file)
     * - allowCredentials(false): không gửi cookie/credentials kèm request
     * - maxAge(3600): trình duyệt cache kết quả preflight OPTIONS trong 1 giờ
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Disposition")
                .allowCredentials(false)
                .maxAge(3600);
    }

    /**
     * Endpoint phục vụ file đã upload: GET /files/{filename}
     *
     * - Resolve tên file vào thư mục upload, trả về file dưới dạng binary stream.
     * - normalize() + toAbsolutePath(): ngăn path traversal attack (vd: ../../etc/passwd).
     * - Content-Disposition: "attachment" → trình duyệt tải file thay vì hiển thị inline.
     * - filename*=UTF-8'': chuẩn RFC 5987, hỗ trợ tên file chứa ký tự Unicode (tiếng Việt...).
     * - {filename:.+}: regex .+ để Spring không cắt phần mở rộng file (vd: .jpg, .png).
     */
    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) throws Exception {
        Path filePath = Paths.get(uploadDir).toAbsolutePath().resolve(filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String encodedName = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
