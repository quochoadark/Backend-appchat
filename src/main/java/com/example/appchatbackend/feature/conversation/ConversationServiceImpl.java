package com.example.appchatbackend.feature.conversation;

import com.example.appchatbackend.exception.ResourceNotFoundException;
import com.example.appchatbackend.feature.message.Message;
import com.example.appchatbackend.feature.message.MessageType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ConversationServiceImpl — implementation cua ConversationService.
 *
 * Logic quan trong:
 * - getOrCreateDirectConversation: kiem tra ton tai truoc khi tao → tranh tao trung
 * - deleteConversation: soft delete (isActive=false), khong xoa khoi DB
 * - kickMember / promoteAdmin / demoteAdmin: chi admin moi co quyen,
 *   nguoi tao nhom (createdBy) duoc bao ve khoi bi kick hoac bi ha quyen
 * - requireAdmin(): ham kiem tra quyen dung chung cho 3 thao tac quan ly nhom
 */
@Service
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;

    public ConversationServiceImpl(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    @Override
    public List<Conversation> getConversationsByUserId(String userId) {
        return conversationRepository.findByParticipantsContainingAndIsActiveTrueOrderByUpdatedAtDesc(userId);
    }

    @Override
    public Optional<Conversation> getConversationById(String id) {
        return conversationRepository.findById(id);
    }

    @Override
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

    @Override
    public Conversation createGroupConversation(Conversation conversation) {
        conversation.setType(ConversationType.GROUP);
        return conversationRepository.save(conversation);
    }

    @Override
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

    @Override
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

    @Override
    public boolean isParticipant(String conversationId, String userId) {
        return conversationRepository.existsByIdAndParticipantsContaining(conversationId, userId);
    }

    @Override
    public void updateLastMessage(String conversationId, Message message) {
        conversationRepository.findById(conversationId).ifPresent(conv -> {
            String preview;
            if (message.getMessageType() == MessageType.IMAGE) {
                preview = "[Ảnh]";
            } else if (message.getMessageType() == MessageType.FILE) {
                preview = message.getContent();
            } else {
                preview = message.getContent();
            }
            conv.setLastMessage(Conversation.LastMessageSnapshot.builder()
                    .messageId(message.getId())
                    .senderId(message.getSenderId())
                    .senderDisplayName(message.getSenderDisplayName())
                    .contentPreview(preview)
                    .messageType(message.getMessageType())
                    .sentAt(message.getCreatedAt())
                    .build());
            conversationRepository.save(conv);
        });
    }

    // Fix 2: Kick thành viên khỏi nhóm (chỉ admin)
    @Override
    public void kickMember(String conversationId, String requesterId, String targetUserId) {
        Conversation conv = getGroupOrThrow(conversationId);
        requireAdmin(conv, requesterId);
        if (targetUserId.equals(conv.getCreatedBy())) {
            throw new IllegalStateException("Không thể kick người tạo nhóm");
        }
        List<String> participants = new ArrayList<>(conv.getParticipants());
        participants.remove(targetUserId);
        conv.setParticipants(participants);
        if (conv.getAdminIds() != null) {
            List<String> admins = new ArrayList<>(conv.getAdminIds());
            admins.remove(targetUserId);
            conv.setAdminIds(admins);
        }
        conversationRepository.save(conv);
    }

    // Fix 2: Thăng thành viên lên admin (chỉ admin)
    @Override
    public void promoteAdmin(String conversationId, String requesterId, String targetUserId) {
        Conversation conv = getGroupOrThrow(conversationId);
        requireAdmin(conv, requesterId);
        if (!conv.getParticipants().contains(targetUserId)) {
            throw new ResourceNotFoundException("Thành viên", "id", targetUserId);
        }
        List<String> admins = conv.getAdminIds() != null
                ? new ArrayList<>(conv.getAdminIds()) : new ArrayList<>();
        if (!admins.contains(targetUserId)) {
            admins.add(targetUserId);
            conv.setAdminIds(admins);
            conversationRepository.save(conv);
        }
    }

    // Fix 2: Hạ admin xuống thành viên thường (chỉ admin)
    @Override
    public void demoteAdmin(String conversationId, String requesterId, String targetUserId) {
        Conversation conv = getGroupOrThrow(conversationId);
        requireAdmin(conv, requesterId);
        if (targetUserId.equals(conv.getCreatedBy())) {
            throw new IllegalStateException("Không thể hạ quyền người tạo nhóm");
        }
        if (conv.getAdminIds() != null) {
            List<String> admins = new ArrayList<>(conv.getAdminIds());
            admins.remove(targetUserId);
            conv.setAdminIds(admins);
            conversationRepository.save(conv);
        }
    }

    private Conversation getGroupOrThrow(String conversationId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Hội thoại", "id", conversationId));
        if (conv.getType() != ConversationType.GROUP) {
            throw new IllegalStateException("Thao tác chỉ áp dụng cho nhóm");
        }
        return conv;
    }

    private void requireAdmin(Conversation conv, String userId) {
        if (conv.getAdminIds() == null || !conv.getAdminIds().contains(userId)) {
            throw new AccessDeniedException("Chỉ admin mới có quyền thực hiện thao tác này");
        }
    }
}
