package com.example.appchatbackend.config;

import com.example.appchatbackend.feature.chat.RedisMessageSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisConfig {

    /**
     * Bean 1: StringRedisTemplate
     *
     * - StringRedisTemplate là một công cụ để thao tác với Redis, chuyên dùng cho kiểu dữ liệu String.
     *   (Khác với RedisTemplate generic, StringRedisTemplate mặc định dùng StringSerializer
     *    nên key/value đều được lưu dưới dạng chuỗi thuần — dễ đọc trong Redis CLI.)
     *
     * - RedisConnectionFactory là đối tượng quản lý kết nối tới Redis server.
     *   Spring Boot tự động tạo factory này dựa trên cấu hình trong application.properties
     *   (spring.data.redis.host, port, password...).
     *   Ta chỉ cần inject vào và truyền cho StringRedisTemplate.
     *
     * - Hàm bean này dùng để: tạo và đăng ký StringRedisTemplate vào Spring Context,
     *   để các class khác có thể inject và dùng Redis (ví dụ: lưu tin nhắn, publish message...).
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    /**
     * Bean 2: RedisMessageListenerContainer
     *
     * - RedisMessageListenerContainer là một container (hộp chứa) chịu trách nhiệm
     *   lắng nghe các message được publish lên Redis theo cơ chế Pub/Sub.
     *   Nó chạy ngầm (background thread), liên tục lắng nghe Redis channel/pattern.
     *
     * - RedisMessageSubscriber là class implement MessageListener —
     *   định nghĩa logic xử lý khi có message đến (ví dụ: gửi tin nhắn qua WebSocket tới client).
     *
     * - PatternTopic("chat:conversation:*") là pattern channel Redis cần lắng nghe.
     *   Dấu * là wildcard, nghĩa là container sẽ lắng nghe TẤT CẢ các channel
     *   có tên bắt đầu bằng "chat:conversation:" (vd: chat:conversation:42, chat:conversation:99...).
     *
     * - Hàm bean này dùng để: kết nối subscriber vào đúng channel Redis,
     *   để mỗi khi có tin nhắn mới được publish lên channel đó,
     *   RedisMessageSubscriber sẽ tự động được gọi để xử lý.
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory factory,
            RedisMessageSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(subscriber, new PatternTopic("chat:conversation:*"));
        return container;
    }
}
