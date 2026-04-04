package com.example.appchatbackend.feature.message;

import com.example.appchatbackend.exception.ResourceNotFoundException;
import com.example.appchatbackend.feature.conversation.ConversationService;
import com.example.appchatbackend.feature.message.dto.request.SendMessageRequest;
import com.example.appchatbackend.feature.user.User;
import com.example.appchatbackend.feature.user.UserRepository;
import com.example.appchatbackend.helper.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestController
public class MessageController {

    private final MessageService messageService;
    private final ConversationService conversationService;
    private final UserRepository userRepository;

    public MessageController(MessageService messageService, ConversationService conversationService, UserRepository userRepository) {
        this.messageService = messageService;
        this.conversationService = conversationService;
        this.userRepository = userRepository;
    }

    // Lấy tin nhắn trong hội thoại (hỗ trợ cursor-based pagination qua query param ?before=<iso-timestamp>)
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<List<Message>>> getMessages(
            @PathVariable String conversationId,
            @RequestParam(required = false) String before,
            @AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        checkParticipant(conversationId, currentUserId);

        List<Message> messages;
        if (before != null && !before.isBlank()) {
            Instant cursor = Instant.parse(before);
            messages = messageService.getMessagesBefore(conversationId, cursor);
        } else {
            messages = messageService.getMessages(conversationId);
        }
        return ResponseEntity.ok(ApiResponse.success("Lấy tin nhắn thành công", messages));
    }

    // Lấy một tin nhắn theo ID
    @GetMapping("/messages/{id}")
    public ResponseEntity<ApiResponse<Message>> getMessageById(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        Message message = messageService.getMessageById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tin nhắn", "id", id));
        checkParticipant(message.getConversationId(), currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy tin nhắn thành công", message));
    }

    // Gửi tin nhắn vào hội thoại
    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<Message>> sendMessage(
            @PathVariable String conversationId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = getCurrentUser(jwt);
        checkParticipant(conversationId, currentUser.getId());

        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(currentUser.getId())
                .senderDisplayName(currentUser.getDisplayName())
                .messageType(request.getMessageType())
                .content(request.getContent())
                .media(request.getMedia())
                .replyToMessageId(request.getReplyToMessageId())
                .build();

        Message sent = messageService.sendMessage(message);
        conversationService.updateLastMessage(conversationId, sent);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath("/messages/{id}")
                .buildAndExpand(sent.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.created("Gửi tin nhắn thành công", sent));
    }

    // Xóa tin nhắn (soft delete, chỉ người gửi mới được xóa)
    @DeleteMapping("/messages/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        Message message = messageService.getMessageById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tin nhắn", "id", id));
        if (!message.getSenderId().equals(currentUserId)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.forbidden("Bạn không có quyền xóa tin nhắn này"));
        }
        messageService.deleteMessage(id);
        return ResponseEntity.noContent().build();
    }

    // Đánh dấu đã đọc tất cả tin nhắn trong hội thoại
    @PostMapping("/conversations/{conversationId}/messages/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable String conversationId,
            @AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        checkParticipant(conversationId, currentUserId);
        messageService.markAsRead(conversationId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Đánh dấu đã đọc thành công", null));
    }

    // Đếm tin nhắn chưa đọc trong hội thoại
    @GetMapping("/conversations/{conversationId}/messages/unread")
    public ResponseEntity<ApiResponse<Long>> countUnread(
            @PathVariable String conversationId,
            @AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        checkParticipant(conversationId, currentUserId);
        long count = messageService.countUnread(conversationId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy số tin nhắn chưa đọc thành công", count));
    }

    private User getCurrentUser(Jwt jwt) {
        String email = jwt.getSubject();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "email", email));
    }

    private String getCurrentUserId(Jwt jwt) {
        return getCurrentUser(jwt).getId();
    }

    private void checkParticipant(String conversationId, String userId) {
        conversationService.getConversationById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Hội thoại", "id", conversationId));
        if (!conversationService.isParticipant(conversationId, userId)) {
            throw new AccessDeniedException("Bạn không phải thành viên của hội thoại này");
        }
    }
}
