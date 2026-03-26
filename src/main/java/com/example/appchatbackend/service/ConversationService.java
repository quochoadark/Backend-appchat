package com.example.appchatbackend.service;

import com.example.appchatbackend.model.Conversation;
import com.example.appchatbackend.model.ConversationType;
import com.example.appchatbackend.repository.ConversationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public List<Conversation> getConversationsByUserId(String userId) {
        return conversationRepository.findByParticipantsContainingAndIsActiveTrueOrderByUpdatedAtDesc(userId);
    }

    public Optional<Conversation> getConversationById(String id) {
        return conversationRepository.findById(id);
    }

    // Tạo hoặc trả về hội thoại DIRECT đã tồn tại giữa 2 người
    public Conversation getOrCreateDirectConversation(String userId1, String userId2) {
        Optional<Conversation> existing = conversationRepository
                .findDirectConversation(ConversationType.DIRECT, userId1, userId2);
        if (existing.isPresent()) {
            return existing.get();
        }
        Conversation conversation = Conversation.builder()
                .type(ConversationType.DIRECT)
                .participants(List.of(userId1, userId2))
                .isActive(true)
                .build();
        return conversationRepository.save(conversation);
    }

    public Conversation createGroupConversation(Conversation conversation) {
        conversation.setType(ConversationType.GROUP);
        return conversationRepository.save(conversation);
    }

    public Optional<Conversation> updateConversation(String id, Conversation updatedConversation) {
        Optional<Conversation> optConversation = conversationRepository.findById(id);
        if (optConversation.isPresent()) {
            Conversation existing = optConversation.get();
            existing.setName(updatedConversation.getName());
            existing.setAvatarUrl(updatedConversation.getAvatarUrl());
            existing.setDescription(updatedConversation.getDescription());
            return Optional.of(conversationRepository.save(existing));
        }
        return Optional.empty();
    }

    public boolean deleteConversation(String id) {
        Optional<Conversation> optConversation = conversationRepository.findById(id);
        if (optConversation.isPresent()) {
            Conversation existing = optConversation.get();
            existing.setActive(false);
            conversationRepository.save(existing);
            return true;
        }
        return false;
    }

    public boolean isParticipant(String conversationId, String userId) {
        return conversationRepository.existsByIdAndParticipantsContaining(conversationId, userId);
    }
}
