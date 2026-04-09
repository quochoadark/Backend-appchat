package com.example.appchatbackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * NgrokConfig — in Ngrok public URL ra log khi ứng dụng khởi động xong.
 *
 * Ngrok là công cụ tạo tunnel công khai từ internet vào localhost,
 * dùng khi dev local nhưng cần expose API ra ngoài (webhook, test mobile...).
 *
 * Giá trị ngrok.public-url được cấu hình trong application.properties.
 * Nếu không cấu hình (blank) thì không in gì cả — an toàn khi deploy production.
 */
@Slf4j
@Component
public class NgrokConfig {

    /**
     * URL public do Ngrok cấp (vd: https://abc123.ngrok.io).
     * Mặc định là chuỗi rỗng nếu không cấu hình trong application.properties.
     */
    @Value("${ngrok.public-url:}")
    private String ngrokPublicUrl;

    /**
     * Chạy sau khi toàn bộ ứng dụng Spring Boot đã sẵn sàng (ApplicationReadyEvent).
     * In URL ra log để developer biết địa chỉ public có thể dùng để test.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logNgrokUrl() {
        if (!ngrokPublicUrl.isBlank()) {
            log.info("==============================================");
            log.info("Ngrok public URL: {}", ngrokPublicUrl);
            log.info("==============================================");
        }
    }
}
