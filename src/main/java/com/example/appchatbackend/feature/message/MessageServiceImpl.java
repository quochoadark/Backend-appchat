package com.example.appchatbackend.feature.message;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final MongoTemplate mongoTemplate;

    public MessageServiceImpl(MessageRepository messageRepository, MongoTemplate mongoTemplate) {
        this.messageRepository = messageRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Message> getMessages(String conversationId) {
        return messageRepository.findByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(
                conversationId, PageRequest.of(0, 50));
    }

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

    // Fix 4: Dùng MongoDB bulk updateMulti thay vì fetch 200 rồi save từng cái
    @Override
    public void markAsRead(String conversationId, String userId) {
        Query query = Query.query(
                Criteria.where("conversation_id").is(conversationId)
                        .and("is_deleted").is(false)
                        .and("read_by." + userId).exists(false)
                        .and("sender_id").ne(userId)
        );
        Update update = new Update().set("read_by." + userId, Instant.now());
        mongoTemplate.updateMulti(query, update, Message.class);
    }

    @Override
    public long countUnread(String conversationId, String userId) {
        return messageRepository.countUnread(conversationId, userId);
    }
}
