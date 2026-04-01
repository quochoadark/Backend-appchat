package com.example.appchatbackend.feature.conversation;

import com.example.appchatbackend.exception.ResourceNotFoundException;
import com.example.appchatbackend.feature.conversation.dto.request.CreateDirectConversationRequest;
import com.example.appchatbackend.feature.conversation.dto.request.CreateGroupConversationRequest;
import com.example.appchatbackend.feature.conversation.dto.request.UpdateConversationRequest;
import com.example.appchatbackend.feature.user.User;
import com.example.appchatbackend.feature.user.UserRepository;
import com.example.appchatbackend.helper.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final UserRepository userRepository;

    public ConversationController(ConversationService conversationService, UserRepository userRepository) {
        this.conversationService = conversationService;
        this.userRepository = userRepository;
    }

    // Lấy danh sách hội thoại của user hiện tại
    @GetMapping
    public ResponseEntity<ApiResponse<List<Conversation>>> getMyConversations(@AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        List<Conversation> conversations = conversationService.getConversationsByUserId(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách hội thoại thành công", conversations));
    }

    // Lấy chi tiết một hội thoại
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Conversation>> getConversationById(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        Conversation conversation = conversationService.getConversationById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hội thoại", "id", id));
        if (!conversationService.isParticipant(id, currentUserId)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.forbidden("Bạn không có quyền xem hội thoại này"));
        }
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin hội thoại thành công", conversation));
    }

    // Tạo hoặc lấy hội thoại DIRECT với một người khác
    @PostMapping("/direct")
    public ResponseEntity<ApiResponse<Conversation>> getOrCreateDirectConversation(
            @Valid @RequestBody CreateDirectConversationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        // Kiểm tra người dùng đích tồn tại
        userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", request.getTargetUserId()));
        Conversation conversation = conversationService.getOrCreateDirectConversation(currentUserId, request.getTargetUserId());
        return ResponseEntity.ok(ApiResponse.success("Lấy hoặc tạo hội thoại thành công", conversation));
    }

    // Tạo hội thoại nhóm
    @PostMapping("/group")
    public ResponseEntity<ApiResponse<Conversation>> createGroupConversation(
            @Valid @RequestBody CreateGroupConversationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);

        List<String> participants = new ArrayList<>(request.getParticipantIds());
        if (!participants.contains(currentUserId)) {
            participants.add(currentUserId);
        }

        Conversation conversation = Conversation.builder()
                .type(ConversationType.GROUP)
                .name(request.getName())
                .description(request.getDescription())
                .avatarUrl(request.getAvatarUrl())
                .participants(participants)
                .createdBy(currentUserId)
                .adminIds(List.of(currentUserId))
                .isActive(true)
                .build();

        Conversation created = conversationService.createGroupConversation(conversation);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath("/conversations/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.created("Tạo nhóm hội thoại thành công", created));
    }

    // Cập nhật thông tin hội thoại nhóm
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Conversation>> updateConversation(
            @PathVariable String id,
            @Valid @RequestBody UpdateConversationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        if (!conversationService.isParticipant(id, currentUserId)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.forbidden("Bạn không có quyền cập nhật hội thoại này"));
        }
        Conversation updatedData = Conversation.builder()
                .name(request.getName())
                .avatarUrl(request.getAvatarUrl())
                .description(request.getDescription())
                .build();
        Conversation updated = conversationService.updateConversation(id, updatedData)
                .orElseThrow(() -> new ResourceNotFoundException("Hội thoại", "id", id));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hội thoại thành công", updated));
    }

    // Xóa hội thoại (soft delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        conversationService.getConversationById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hội thoại", "id", id));
        if (!conversationService.isParticipant(id, currentUserId)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.forbidden("Bạn không có quyền xóa hội thoại này"));
        }
        conversationService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }

    private String getCurrentUserId(Jwt jwt) {
        String email = jwt.getSubject();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "email", email));
        return user.getId();
    }
}
