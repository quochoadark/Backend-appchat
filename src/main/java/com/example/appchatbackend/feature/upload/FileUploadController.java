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
