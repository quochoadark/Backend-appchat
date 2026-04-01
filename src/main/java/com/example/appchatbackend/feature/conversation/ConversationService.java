package com.example.appchatbackend.feature.conversation;

import java.util.List;
import java.util.Optional;

public interface ConversationService {

    List<Conversation> getConversationsByUserId(String userId);

    Optional<Conversation> getConversationById(String id);

    Conversation getOrCreateDirectConversation(String userId1, String userId2);

    Conversation createGroupConversation(Conversation conversation);

    Optional<Conversation> updateConversation(String id, Conversation updatedConversation);

    boolean deleteConversation(String id);

    boolean isParticipant(String conversationId, String userId);
}
