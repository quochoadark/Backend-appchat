package com.example.appchatbackend.feature.conversation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ConversationRepository — truy cập MongoDB collection "conversations".
 * Bao gồm các query phức tạp dùng @Query annotation với MongoDB query syntax.
 */
@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {

    // Lấy tất cả hội thoại của 1 user, sắp xếp theo tin nhắn mới nhất
    List<Conversation> findByParticipantsContainingAndIsActiveTrueOrderByUpdatedAtDesc(String userId);

    // Tìm cuộc hội thoại DIRECT giữa 2 người (tránh tạo trùng)
    @Query("{ 'type': ?0, 'participants': { $all: [?1, ?2], $size: 2 } }")
    Optional<Conversation> findDirectConversation(ConversationType type, String userId1, String userId2);

    // Kiểm tra user có thuộc hội thoại không
    boolean existsByIdAndParticipantsContaining(String id, String userId);
}
