package com.example.appchatbackend.feature.conversation;

import com.example.appchatbackend.feature.message.Message;

import java.util.List;
import java.util.Optional;

/**
 * ConversationService — interface nghiệp vụ quản lý hội thoại.
 * Implementation: ConversationServiceImpl
 */
public interface ConversationService {

    /** Lấy danh sách hội thoại đang active của user, mới nhất trước */
    List<Conversation> getConversationsByUserId(String userId);

    /** Lấy hội thoại theo ID */
    Optional<Conversation> getConversationById(String id);

    /**
     * Lấy hoặc tạo hội thoại DIRECT giữa 2 user.
     * Nếu đã tồn tại → trả về luôn (không tạo trùng).
     * Nếu chưa có → tạo mới và trả về.
     */
    Conversation getOrCreateDirectConversation(String userId1, String userId2);

    /** Tạo hội thoại nhóm mới */
    Conversation createGroupConversation(Conversation conversation);

    /** Cập nhật thông tin nhóm (tên, avatar, mô tả) */
    Optional<Conversation> updateConversation(String id, Conversation updatedConversation);

    /** Soft delete hội thoại (đặt isActive = false) */
    boolean deleteConversation(String id);

    /** Kiểm tra userId có phải thành viên của hội thoại không */
    boolean isParticipant(String conversationId, String userId);

    /** Cập nhật snapshot lastMessage sau mỗi lần có tin nhắn mới */
    void updateLastMessage(String conversationId, Message message);

    /** Xóa thành viên khỏi nhóm (chỉ admin được thực hiện) */
    void kickMember(String conversationId, String requesterId, String targetUserId);

    /** Thăng thành viên lên admin (chỉ admin được thực hiện) */
    void promoteAdmin(String conversationId, String requesterId, String targetUserId);

    /** Hạ admin xuống thành viên thường (chỉ admin, không hạ được người tạo nhóm) */
    void demoteAdmin(String conversationId, String requesterId, String targetUserId);
}
