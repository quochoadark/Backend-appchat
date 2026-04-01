package com.example.appchatbackend.feature.message;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MessageService {

    List<Message> getMessages(String conversationId);

    List<Message> getMessagesBefore(String conversationId, Instant cursor);

    Optional<Message> getMessageById(String id);

    Message sendMessage(Message message);

    boolean deleteMessage(String id);

    void markAsRead(String conversationId, String userId);

    long countUnread(String conversationId, String userId);
}
