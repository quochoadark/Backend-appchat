package com.example.appchatbackend.feature.upload;

import com.example.appchatbackend.feature.message.Message;
import com.example.appchatbackend.helper.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * FileUploadController — xu ly upload file (anh hoac file dinh kem).
 *
 * Luong upload:
 * 1. Client POST /upload voi multipart/form-data (field "file")
 * 2. Server luu file voi ten UUID (tranh trung ten + tranh path traversal)
 * 3. Tra ve MediaAttachment (url, fileName, fileSize, mimeType)
 * 4. Client dung MediaAttachment nay khi gui tin nhan IMAGE/FILE qua WebSocket hoac REST
 *
 * URL tra ve:
 * - Neu co Ngrok URL (dev): dung ngrok URL → co the truy cap tu internet
 * - Khong co Ngrok: dung server URL hien tai → chi truy cap trong mang noi bo
 *
 * File duoc phuc vu qua GET /files/{filename} (dinh nghia trong WebMvcConfig).
 */
@RestController
@RequestMapping("/upload")
public class FileUploadController {

    @Value("${upload.dir}")
    private String uploadDir;

    @Value("${ngrok.public-url:}")
    private String ngrokPublicUrl;

    /**
     * Upload file (ảnh hoặc file đính kèm)
     * POST /upload
     * Content-Type: multipart/form-data
     * field: file
     *
     * Trả về MediaAttachment để dùng khi gửi tin nhắn qua WebSocket hoặc REST
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Message.MediaAttachment>> upload(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("File không được để trống"));
        }

        // Tạo thư mục nếu chưa có
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
        Files.createDirectories(uploadPath);

        // Tạo tên file duy nhất: UUID + giữ extension gốc
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String savedFilename = UUID.randomUUID() + extension;

        // Lưu file
        Path targetPath = uploadPath.resolve(savedFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Build URL trả về — dùng ngrok public URL nếu có
        String baseUrl = (ngrokPublicUrl != null && !ngrokPublicUrl.isBlank())
                ? ngrokPublicUrl
                : request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        String fileUrl = baseUrl + "/files/" + savedFilename;

        Message.MediaAttachment attachment = Message.MediaAttachment.builder()
                .url(fileUrl)
                .fileName(originalFilename)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Upload file thành công", attachment));
    }
}
