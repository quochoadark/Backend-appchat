package com.example.appchatbackend.feature.message;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * MessageRepository — truy cap MongoDB collection "messages".
 *
 * Dung cursor-based pagination (theo created_at) thay vi offset pagination
 * de tranh van de performance khi collection lon (offset phai skip N document).
 */
@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    // Trang đầu: lấy tin nhắn mới nhất trong hội thoại
    List<Message> findByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(
            String conversationId, Pageable pageable);

    // Trang tiếp: cursor-based pagination (cuộn lên xem tin cũ hơn)
    List<Message> findByConversationIdAndCreatedAtBeforeAndIsDeletedFalseOrderByCreatedAtDesc(
            String conversationId, Instant cursor, Pageable pageable);

    // Đếm tin nhắn chưa đọc của 1 user trong 1 hội thoại
    @Query(value = "{ 'conversation_id': ?0, 'read_by.?1': { $exists: false }, 'sender_id': { $ne: ?1 }, 'is_deleted': false }",
           count = true)
    long countUnread(String conversationId, String userId);
}
