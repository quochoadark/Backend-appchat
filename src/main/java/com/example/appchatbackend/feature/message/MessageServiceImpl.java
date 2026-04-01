package com.example.appchatbackend.feature.message;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;

    public MessageServiceImpl(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    // Trang đầu tiên (50 tin nhắn mới nhất)
    @Override
    public List<Message> getMessages(String conversationId) {
        return messageRepository.findByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(
                conversationId, PageRequest.of(0, 50));
    }

    // Trang tiếp theo — cursor là created_at của tin nhắn cũ nhất đã nhận
    @Override
    public List<Message> getMessagesBefore(String conversationId, Instant cursor) {
        return messageRepository.findByConversationIdAndCreatedAtBeforeAndIsDeletedFalseOrderByCreatedAtDesc(
                conversationId, cursor, PageRequest.of(0, 50));
    }

    @Override
    public Optional<Message> getMessageById(String id) {
        return messageRepository.findById(id);
    }

    @Override
    public Message sendMessage(Message message) {
        // Tự động thêm người gửi vào read_by
        if (message.getReadBy() == null) {
            message.setReadBy(new HashMap<>());
        }
        message.getReadBy().put(message.getSenderId(), Instant.now());
        return messageRepository.save(message);
    }

    @Override
    public boolean deleteMessage(String id) {
        Optional<Message> optMessage = messageRepository.findById(id);
        if (optMessage.isPresent()) {
            Message existing = optMessage.get();
            existing.setDeleted(true);
            existing.setDeletedAt(Instant.now());
            existing.setContent(null);
            messageRepository.save(existing);
            return true;
        }
        return false;
    }

    // Đánh dấu đã đọc — thêm userId vào read_by của từng tin nhắn
    @Override
    public void markAsRead(String conversationId, String userId) {
        List<Message> unread = messageRepository
                .findByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(
                        conversationId, PageRequest.of(0, 200));
        Instant now = Instant.now();
        for (Message msg : unread) {
            if (!msg.getReadBy().containsKey(userId)) {
                msg.getReadBy().put(userId, now);
            }
        }
        messageRepository.saveAll(unread);
    }

    @Override
    public long countUnread(String conversationId, String userId) {
        return messageRepository.countUnread(conversationId, userId);
    }
}
