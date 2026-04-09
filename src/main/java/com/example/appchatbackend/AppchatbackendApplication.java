package com.example.appchatbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * AppchatbackendApplication — điểm khởi động của toàn bộ ứng dụng Spring Boot.
 *
 * @SpringBootApplication: bật auto-configuration, component scan, và configuration.
 * @EnableMongoAuditing: kích hoạt tính năng tự động gán @CreatedDate / @LastModifiedDate
 *   cho các MongoDB document khi lưu — không cần set thủ công.
 */
@SpringBootApplication
@EnableMongoAuditing
public class AppchatbackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppchatbackendApplication.class, args);
	}

}