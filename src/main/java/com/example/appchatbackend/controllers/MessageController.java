package com.example.appchatbackend.controllers;

// --- CÁC IMPORT LIÊN QUAN ĐẾN SERVICE VÀ MODEL ---
import com.example.appchatbackend.services.MessageService; // Xử lý logic tin nhắn
import com.example.appchatbackend.models.Message; // Cấu trúc dữ liệu của một tin nhắn

// --- CÁC IMPORT LIÊN QUAN ĐẾN LỖI VÀ WEBSOCKET ---
import com.example.appchatbackend.exception.ResourceNotFoundException; // Lỗi 404 khi không tìm thấy dữ liệu
import com.example.appchatbackend.config.websocket.RedisMessagePublisher; // Gửi thông báo real-time qua Redis
import com.example.appchatbackend.dtos.ChatNotification; // Khung thông báo gửi qua WebSocket
import com.example.appchatbackend.services.ConversationService; // Xử lý logic cuộc trò chuyện
import com.example.appchatbackend.dtos.request.SendMessageRequest; // Dữ liệu client gửi lên khi chat
import com.example.appchatbackend.models.User; // Người dùng
import com.example.appchatbackend.repositories.UserRepository; // Database người dùng
import com.example.appchatbackend.helper.ApiResponse; // Chuẩn hóa dữ liệu trả về

import jakarta.validation.Valid; // Annotation kiểm tra tính hợp lệ của dữ liệu
import org.springframework.http.ResponseEntity; // Đóng gói Response HTTP
import org.springframework.security.access.AccessDeniedException; // Lỗi quyền truy cập (403)
import org.springframework.security.core.annotation.AuthenticationPrincipal; // Lấy thông tin user hiện tại từ Token
import org.springframework.security.oauth2.jwt.Jwt; // Kiểu dữ liệu của Token
import org.springframework.web.bind.annotation.*; // Spring RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder; // Tạo URL động

import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * MessageController — REST API quản lý tin nhắn thông qua giao thức HTTP.
 * Khác với ChatController sử dụng WebSocket.
 *
 * Yêu cầu chung:
 * 1. Phải có JWT (phải đăng nhập).
 * 2. Chỉ có thành viên của cuộc hội thoại mới được xem/nhắn tin.
 */
@RestController
public class MessageController {

    private final MessageService messageService;
    private final ConversationService conversationService;
    private final UserRepository userRepository;
    private final RedisMessagePublisher redisPublisher;

    public MessageController(MessageService messageService, ConversationService conversationService,
                             UserRepository userRepository, RedisMessagePublisher redisPublisher) {
        this.messageService = messageService;
        this.conversationService = conversationService;
        this.userRepository = userRepository;
        this.redisPublisher = redisPublisher;
    }

    /**
     * Lấy danh sách tin nhắn của một cuộc hội thoại.
     * Hỗ trợ con trỏ (cursor pagination) thông qua tham số `before` để tải tin nhắn cũ.
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<List<Message>>> getMessages(
            @PathVariable String conversationId, // ID cuộc hội thoại
            @RequestParam(required = false) String before, // Lấy các tin nhắn trước mốc thời gian này
            @AuthenticationPrincipal Jwt jwt) { // Lấy thông tin người gọi API
            
        String currentUserId = getCurrentUserId(jwt);
        // Kiểm tra xem người này có quyền xem hội thoại không
        checkParticipant(conversationId, currentUserId);

        List<Message> messages;
        // Nếu có truyền mốc thời gian (before) -> Tải tin nhắn cũ (Vuốt lên để tải thêm)
        if (before != null && !before.isBlank()) {
            Instant cursor = Instant.parse(before);
            messages = messageService.getMessagesBefore(conversationId, cursor);
        } else {
            // Không truyền thì lấy tin nhắn mới nhất
            messages = messageService.getMessages(conversationId);
        }
        return ResponseEntity.ok(ApiResponse.success("Lấy tin nhắn thành công", messages));
    }

    /**
     * Xem thông tin chi tiết 1 tin nhắn dựa trên ID
     */
    @GetMapping("/messages/{id}")
    public ResponseEntity<ApiResponse<Message>> getMessageById(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        Message message = messageService.getMessageById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tin nhắn", "id", id));
        
        // Vẫn phải kiểm tra xem user này có ở trong cuộc hội thoại chứa tin nhắn này không
        checkParticipant(message.getConversationId(), currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy tin nhắn thành công", message));
    }

    /**
     * Gửi một tin nhắn mới qua giao thức REST HTTP (Một cách dự phòng ngoài WebSocket)
     */
    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<Message>> sendMessage(
            @PathVariable String conversationId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        User currentUser = getCurrentUser(jwt);
        checkParticipant(conversationId, currentUser.getId());

        // Đóng gói tin nhắn
        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(currentUser.getId())
                .senderDisplayName(currentUser.getDisplayName())
                .messageType(request.getMessageType())
                .content(request.getContent())
                .media(request.getMedia())
                .replyToMessageId(request.getReplyToMessageId())
                .build();

        // Lưu vào DB
        Message sent = messageService.sendMessage(message);
        // Cập nhật tin nhắn mới nhất của Box Chat
        conversationService.updateLastMessage(conversationId, sent);

        // BẮT BUỘC: Vì gửi qua API HTTP, ta phải chủ động đẩy sự kiện lên Redis
        // để các người dùng khác (đang dùng WebSocket) thấy ngay lập tức mà không cần reload trang
        redisPublisher.publish(conversationId, ChatNotification.builder()
                .type(ChatNotification.NotificationType.NEW_MESSAGE)
                .conversationId(conversationId)
                .data(sent)
                .build());

        // Tạo URL trả về đường dẫn tới tin nhắn vừa tạo
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath("/messages/{id}")
                .buildAndExpand(sent.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.created("Gửi tin nhắn thành công", sent));
    }

    /**
     * Xóa tin nhắn (Chỉ người gửi mới có quyền xóa)
     */
    @DeleteMapping("/messages/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        Message message = messageService.getMessageById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tin nhắn", "id", id));
        
        // Chặn nếu cố xóa tin nhắn của người khác
        if (!message.getSenderId().equals(currentUserId)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.forbidden("Bạn không có quyền xóa tin nhắn này"));
        }
        messageService.deleteMessage(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Đánh dấu là đã đọc toàn bộ tin nhắn trong cuộc hội thoại
     */
    @PostMapping("/conversations/{conversationId}/messages/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable String conversationId,
            @AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        checkParticipant(conversationId, currentUserId);
        
        // Gọi service xử lý logic DB
        messageService.markAsRead(conversationId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Đánh dấu đã đọc thành công", null));
    }

    /**
     * Đếm số lượng tin nhắn chưa đọc trong 1 box chat
     */
    @GetMapping("/conversations/{conversationId}/messages/unread")
    public ResponseEntity<ApiResponse<Long>> countUnread(
            @PathVariable String conversationId,
            @AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        checkParticipant(conversationId, currentUserId);
        long count = messageService.countUnread(conversationId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy số tin nhắn chưa đọc thành công", count));
    }

    /**
     * Thả cảm xúc (React emoji) vào một tin nhắn cụ thể
     */
    @PostMapping("/messages/{id}/react")
    public ResponseEntity<ApiResponse<Message>> reactToMessage(
            @PathVariable String id, // ID của tin nhắn bị thả cảm xúc
            @RequestParam(required = false) String emoji, // Icon cảm xúc
            @AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        Message message = messageService.getMessageById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tin nhắn", "id", id));
        checkParticipant(message.getConversationId(), currentUserId);

        Message updatedMessage = messageService.reactToMessage(id, currentUserId, emoji);

        // Bắn sự kiện qua WebSocket để icon nhảy lên màn hình của mọi người
        redisPublisher.publish(message.getConversationId(), ChatNotification.builder()
                .type(ChatNotification.NotificationType.NEW_MESSAGE)
                .conversationId(message.getConversationId())
                .data(updatedMessage)
                .build());

        return ResponseEntity.ok(ApiResponse.success("Thả cảm xúc thành công", updatedMessage));
    }

    // --- CÁC HÀM TIỆN ÍCH (HELPER METHODS) BÊN DƯỚI ---

    // Lấy ID người dùng trực tiếp từ Payload của JWT để tiết kiệm 1 câu query vào CSDL
    private String getCurrentUserId(Jwt jwt) {
        String userId = jwt.getClaimAsString("userId");
        if (userId != null && !userId.isBlank()) return userId;
        // Fallback: Nếu JWT cũ không có userId, phải query DB bằng email
        return getCurrentUser(jwt).getId();
    }

    // Tìm kiếm User trong DB thông qua email được cấp trong JWT
    private User getCurrentUser(Jwt jwt) {
        String email = jwt.getSubject();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "email", email));
    }

    // Hàm kiểm tra xem 1 người dùng có nằm trong cuộc hội thoại hay không
    private void checkParticipant(String conversationId, String userId) {
        conversationService.getConversationById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Hội thoại", "id", conversationId));
        if (!conversationService.isParticipant(conversationId, userId)) {
            // Ném lỗi 403 Forbidden nếu không phải thành viên
            throw new AccessDeniedException("Bạn không phải thành viên của hội thoại này");
        }
    }
}
