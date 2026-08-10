package com.example.appchatbackend.controllers;

// --- CÁC IMPORT LIÊN QUAN ĐẾN WEBSOCKET VÀ REDIS ---
// Lắng nghe tin nhắn từ Redis
import com.example.appchatbackend.config.websocket.RedisMessageSubscriber;
// Gửi tin nhắn lên Redis (để broadcast cho các instance khác nếu chạy nhiều server)
import com.example.appchatbackend.config.websocket.RedisMessagePublisher;
// Quản lý trạng thái Online/Offline của người dùng
import com.example.appchatbackend.services.OnlineStatusService;

// --- CÁC IMPORT DTO VÀ SỰ KIỆN ---
// DTO yêu cầu gửi tin nhắn
import com.example.appchatbackend.dtos.request.ChatMessageRequest;
// DTO chuẩn hóa dữ liệu thông báo qua WebSocket
import com.example.appchatbackend.dtos.ChatNotification;
// Sự kiện đánh dấu "đã đọc"
import com.example.appchatbackend.config.websocket.ReadReceiptEvent;
// Sự kiện "đang gõ phím"
import com.example.appchatbackend.config.websocket.TypingEvent;

// --- CÁC IMPORT LIÊN QUAN ĐẾN DỊCH VỤ VÀ MODEL ---
import com.example.appchatbackend.services.ConversationService; // Quản lý cuộc hội thoại
import com.example.appchatbackend.models.Message; // Model Tin nhắn
import com.example.appchatbackend.services.MessageService; // Quản lý lưu trữ tin nhắn
import com.example.appchatbackend.models.User; // Model Người dùng
import com.example.appchatbackend.repositories.UserRepository; // Repository tương tác DB User

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// Annotation bắt các bản tin (message) gửi qua WebSocket
import org.springframework.messaging.handler.annotation.MessageMapping;
// Annotation lấy dữ liệu payload (body) từ message
import org.springframework.messaging.handler.annotation.Payload;
// Công cụ dùng để gửi tin nhắn thẳng từ Server xuống Client qua WebSocket
import org.springframework.messaging.simp.SimpMessagingTemplate;
// Đánh dấu đây là một Controller (không phải RestController, vì nó dùng cho WebSocket)
import org.springframework.stereotype.Controller;

import java.security.Principal; // Chứa thông tin user đang gọi WebSocket
import java.time.Instant; // Xử lý thời gian

/**
 * ChatController — Xử lý các sự kiện thời gian thực (real-time) qua WebSocket + STOMP.
 *
 * Khác với MessageController dùng REST HTTP thông thường, ChatController dùng @MessageMapping
 * để nhận dữ liệu từ client thông qua kết nối WebSocket, xử lý và phát (broadcast) lại cho các client khác.
 *
 * Luồng xử lý tin nhắn (ví dụ chat.send):
 *   1. Client gửi tới → /app/chat.send → Hàm ChatController.sendMessage() nhận
 *   2. Lưu tin nhắn vào cơ sở dữ liệu (MongoDB)
 *   3. Publish tin nhắn lên Redis → RedisMessageSubscriber ở các máy chủ khác (nếu có) sẽ nhận được
 *   4. Broadcast qua SimpMessagingTemplate tới kênh → /topic/conversation/{id}
 *   5. Tất cả client đang "theo dõi" (subscribe) kênh đó sẽ nhận được tin nhắn ngay lập tức.
 *
 * Tại sao phải qua Redis thay vì broadcast thẳng luôn?
 * → Để hỗ trợ khả năng mở rộng ngang (horizontal scaling). Nếu bạn chạy 3 server ứng dụng chat:
 *   Server 1 chỉ biết kết nối của client A, không biết client B (ở server 2).
 *   Redis làm kênh trung gian. Server 1 báo Redis -> Redis báo Server 2 -> Server 2 gửi cho client B.
 *
 * Các sự kiện WebSocket hỗ trợ:
 * - /app/chat.send    → gửi tin nhắn (lưu DB + thông báo real-time)
 * - /app/chat.typing  → báo "đang gõ" (chỉ gửi real-time, không lưu DB)
 * - /app/chat.read    → báo "đã đọc" (lưu DB + thông báo real-time)
 */
@Controller
public class ChatController {

    // Khởi tạo Logger để in log ra màn hình console (tiện debug)
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    // Khai báo các Dependencies
    private final MessageService messageService;
    private final ConversationService conversationService;
    private final UserRepository userRepository;
    private final RedisMessagePublisher redisPublisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final OnlineStatusService onlineStatusService;

    // Tự động tiêm các dependencies bằng Constructor
    public ChatController(MessageService messageService,
                          ConversationService conversationService,
                          UserRepository userRepository,
                          RedisMessagePublisher redisPublisher,
                          SimpMessagingTemplate messagingTemplate,
                          OnlineStatusService onlineStatusService) {
        this.messageService = messageService;
        this.conversationService = conversationService;
        this.userRepository = userRepository;
        this.redisPublisher = redisPublisher;
        this.messagingTemplate = messagingTemplate;
        this.onlineStatusService = onlineStatusService;
    }

    /**
     * Client gửi tin nhắn: gọi đến /app/chat.send
     * Sau khi lưu vào CSDL, tin nhắn sẽ được broadcast qua Redis đến /topic/conversation/{conversationId}
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        // Lấy userId của người gửi từ đối tượng Principal (được set lúc xác thực kết nối WebSocket)
        String senderId = principal.getName(); 

        // Kiểm tra xem người gửi có phải là thành viên của cuộc hội thoại này không.
        // Nếu không phải, từ chối và in log cảnh báo.
        if (!conversationService.isParticipant(request.getConversationId(), senderId)) {
            log.warn("User {} cố gửi tin vào conversation {} nhưng không phải thành viên", senderId, request.getConversationId());
            return;
        }

        // Lấy thông tin người gửi từ DB
        User sender = userRepository.findById(senderId).orElse(null);
        if (sender == null) return; // Nếu user không tồn tại thì dừng lại

        // Xây dựng đối tượng Tin Nhắn (Message) để chuẩn bị lưu vào Database
        Message message = Message.builder()
                .conversationId(request.getConversationId()) // ID cuộc trò chuyện
                .senderId(senderId) // ID người gửi
                .senderDisplayName(sender.getDisplayName()) // Tên hiển thị người gửi
                .messageType(request.getMessageType()) // Loại tin (TEXT, IMAGE, FILE...)
                .content(request.getContent()) // Nội dung text
                .media(request.getMedia()) // Đường dẫn file/ảnh đính kèm
                .replyToMessageId(request.getReplyToMessageId()) // ID tin nhắn đang trả lời (nếu có)
                .build();

        // Lưu tin nhắn mới vào CSDL
        Message saved = messageService.sendMessage(message);
        
        // Cập nhật lại "tin nhắn cuối cùng" (last message) cho cuộc trò chuyện đó
        conversationService.updateLastMessage(request.getConversationId(), saved);
        
        // Reset trạng thái online của người dùng (vì họ vừa thao tác)
        onlineStatusService.refreshOnline(senderId);

        // Đẩy thông báo lên Redis để tất cả các server đều biết có tin nhắn mới.
        // Dữ liệu bọc trong ChatNotification với type là NEW_MESSAGE.
        redisPublisher.publish(request.getConversationId(),
                ChatNotification.builder()
                        .type(ChatNotification.NotificationType.NEW_MESSAGE)
                        .conversationId(request.getConversationId())
                        .data(saved)
                        .build()
        );
    }

    /**
     * Client phát sự kiện đang gõ phím: gọi đến /app/chat.typing
     * Bắn thẳng xuống các client khác qua WebSocket (không lưu vào DB, không cần qua Redis)
     */
    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingEvent event, Principal principal) {
        String senderId = principal.getName(); // Lấy ID người gửi

        // Nếu không thuộc cuộc hội thoại thì bỏ qua
        if (!conversationService.isParticipant(event.getConversationId(), senderId)) return;

        // Gán ID người gửi vào sự kiện
        event.setSenderId(senderId);

        // Sử dụng SimpMessagingTemplate gửi thẳng tín hiệu xuống kênh /topic/conversation/...
        // Các user khác đang mở màn hình chat này sẽ thấy hiệu ứng "Đang soạn tin..."
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + event.getConversationId(),
                ChatNotification.builder()
                        .type(ChatNotification.NotificationType.TYPING)
                        .conversationId(event.getConversationId())
                        .data(event)
                        .build()
        );
    }

    /**
     * Client đánh dấu đã đọc tin nhắn: gọi đến /app/chat.read
     * Cập nhật trạng thái vào DB và phát thông báo "đã đọc"
     */
    @MessageMapping("/chat.read")
    public void markAsRead(@Payload TypingEvent event, Principal principal) {
        String userId = principal.getName();
        String conversationId = event.getConversationId();

        // Kiểm tra quyền
        if (!conversationService.isParticipant(conversationId, userId)) return;

        // Gọi Service lưu vào Database trạng thái đã đọc của người này
        messageService.markAsRead(conversationId, userId);

        // Gửi thông báo real-time để client phía bên kia thấy nhãn "Đã đọc" hoặc icon con mắt
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId,
                ChatNotification.builder()
                        .type(ChatNotification.NotificationType.READ_RECEIPT)
                        .conversationId(conversationId)
                        // Data đi kèm là thời gian đọc hiện tại
                        .data(new ReadReceiptEvent(conversationId, userId, Instant.now()))
                        .build()
        );
    }
}
