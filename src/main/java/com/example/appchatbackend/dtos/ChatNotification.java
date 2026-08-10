package com.example.appchatbackend.dtos;

// --- CÁC IMPORT LIÊN QUAN ĐẾN SỰ KIỆN ---
import com.example.appchatbackend.config.websocket.TypingEvent; // Sự kiện gõ phím
import com.example.appchatbackend.config.websocket.ReadReceiptEvent; // Sự kiện đã đọc tin nhắn

// --- CÁC IMPORT LOMBOK ---
// Lombok là thư viện giúp tự động sinh ra các hàm Getter, Setter, Constructor... để code gọn hơn
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ChatNotification — "Chiếc phong bì" (envelope) chứa tất cả các loại thông báo thời gian thực qua WebSocket.
 *
 * Class này được chuyển đổi (serialize) thành chuỗi JSON khi đẩy lên Redis hoặc khi gửi xuống Client.
 * Phía Client (React Native/React) sẽ đọc trường "type" để biết cách xử lý cục dữ liệu nằm trong trường "data":
 * 
 * Các trường hợp xử lý ở Client:
 * - Nếu type = NEW_MESSAGE  → "data" sẽ là 1 đối tượng Message → Hiển thị tin nhắn mới lên màn hình
 * - Nếu type = TYPING       → "data" sẽ là 1 đối tượng TypingEvent → Hiển thị "Ai đó đang gõ..."
 * - Nếu type = READ_RECEIPT → "data" sẽ là 1 đối tượng ReadReceiptEvent → Hiển thị icon đã xem / avatar nhỏ
 * - Nếu type = USER_ONLINE hoặc USER_OFFLINE → "data" sẽ là userId (dạng String) → Sáng đèn/tắt đèn chấm xanh online
 *
 * Tại sao "data" lại để kiểu Object?
 * → Để linh hoạt chứa được nhiều loại cấu trúc dữ liệu khác nhau (Message, Event, String) mà không cần
 * phải tạo ra hàng chục class Notification riêng lẻ gây rác code.
 */
@Data // Tự động sinh Getter, Setter, toString, equals, hashCode
@Builder // Cung cấp pattern Builder để khởi tạo object dễ dàng hơn (vd: ChatNotification.builder().type(...).build())
@NoArgsConstructor // Tự động sinh Constructor không có tham số
@AllArgsConstructor // Tự động sinh Constructor có đầy đủ tất cả tham số
public class ChatNotification {

    // Loại thông báo (Dựa vào Enum bên dưới)
    private NotificationType type;

    // ID của cuộc hội thoại để Client biết thông báo này thuộc về box chat nào
    private String conversationId;

    // Payload linh hoạt: Chứa dữ liệu thực tế mang theo (Message, TypingEvent, ReadReceiptEvent, userId...)
    private Object data;

    /**
     * Enum định nghĩa các loại thông báo hỗ trợ trong hệ thống
     */
    public enum NotificationType {
        NEW_MESSAGE,   // Báo có tin nhắn mới
        TYPING,        // Báo ai đó đang soạn tin
        READ_RECEIPT,  // Báo ai đó đã đọc tin nhắn
        USER_ONLINE,   // Báo ai đó vừa đăng nhập/mở app (sáng chấm xanh)
        USER_OFFLINE   // Báo ai đó vừa tắt app (tắt chấm xanh)
    }
}
