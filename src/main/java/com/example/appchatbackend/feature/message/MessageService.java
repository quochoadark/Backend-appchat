package com.example.appchatbackend.feature.message;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * MessageService — interface nghiep vu quan ly tin nhan.
 * Implementation: MessageServiceImpl
 */
public interface MessageService {

    /** Lay 50 tin nhan moi nhat cua hoi thoai (trang dau) */
    List<Message> getMessages(String conversationId);

    /**
     * Lay 50 tin nhan truoc thoi diem cursor (cursor-based pagination).
     * Client gui cursor = created_at cua tin nhan cu nhat dang hien → cuon len xem them.
     */
    List<Message> getMessagesBefore(String conversationId, Instant cursor);

    /** Lay 1 tin nhan theo ID */
    Optional<Message> getMessageById(String id);

    /**
     * Luu tin nhan moi vao DB.
     * Tu dong them sender vao readBy (nguoi gui tu dong da "doc" tin nhan cua chinh minh).
     */
    Message sendMessage(Message message);

    /** Soft delete tin nhan: dat isDeleted=true, xoa content, luu deletedAt */
    boolean deleteMessage(String id);

    /**
     * Danh dau tat ca tin nhan chua doc trong hoi thoai la da doc.
     * Dung MongoDB bulk updateMulti → khong fetch tung tin nhan, hieu qua hon.
     */
    void markAsRead(String conversationId, String userId);

    /** Dem so tin nhan chua doc cua userId trong hoi thoai */
    long countUnread(String conversationId, String userId);
}
