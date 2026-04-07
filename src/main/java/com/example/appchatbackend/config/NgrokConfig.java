package com.example.appchatbackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NgrokConfig {

    @Value("${ngrok.public-url:}")
    private String ngrokPublicUrl;

    @EventListener(ApplicationReadyEvent.class)
    public void logNgrokUrl() {
        if (!ngrokPublicUrl.isBlank()) {
            log.info("==============================================");
            log.info("Ngrok public URL: {}", ngrokPublicUrl);
            log.info("==============================================");
        }
    }
}
