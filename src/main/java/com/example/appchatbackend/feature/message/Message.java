package com.example.appchatbackend.feature.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

/**
 * Message — document MongoDB luu tin nhan, collection "messages".
 *
 * Ho tro nhieu loai tin nhan (MessageType): TEXT, IMAGE, FILE, SYSTEM.
 * - content: noi dung van ban (null neu la IMAGE/FILE)
 * - media (embedded MediaAttachment): thong tin file dinh kem (null neu la TEXT)
 * - replyToMessageId: ID tin nhan duoc reply (null neu khong phai reply)
 * - readBy (Map<userId, thoiDiemDoc>): theo doi ai da doc tin — dung de dem tin chua doc
 *   va hien thi "da xem" (read receipt)
 * - isDeleted: soft delete — van giu document trong DB nhung khong hien cho client
 * - senderDisplayName: luu ten hien thi tai thoi diem gui
 *   → tranh query DB moi lan hien thi, ten van chinh xac du user doi sau
 *
 * CompoundIndexes toi uu 2 truy van chinh:
 * - idx_msg_conv_created: phan trang tin nhan cua 1 hoi thoai (DESC theo thoi gian)
 * - idx_msg_conv_sender: tim tin nhan cua 1 user trong 1 hoi thoai
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "messages")
@CompoundIndexes({
    // Phan trang tin nhan trong 1 hoi thoai
    @CompoundIndex(name = "idx_msg_conv_created",
                   def = "{'conversation_id': 1, 'created_at': -1}"),
    // Tim tin nhan cua 1 user trong 1 hoi thoai
    @CompoundIndex(name = "idx_msg_conv_sender",
                   def = "{'conversation_id': 1, 'sender_id': 1}")
})
public class Message {

    @Id
    private String id;

    @Field("conversation_id")
    private String conversationId;

    @Field("sender_id")
    private String senderId;

    // Lưu lại tên người gửi tại thời điểm gửi (tránh join vào users)
    @Field("sender_display_name")
    private String senderDisplayName;

    @Field("message_type")
    private MessageType messageType;

    // Nội dung text (null nếu là ảnh/file)
    @Field("content")
    private String content;

    // Tệp đính kèm (null nếu là TEXT)
    @Field("media")
    private MediaAttachment media;

    // Trả lời tin nhắn nào (null nếu không reply)
    @Field("reply_to_message_id")
    private String replyToMessageId;

    // Map<userId, thời điểm đọc> — dùng để đếm tin chưa đọc
    @Field("read_by")
    private Map<String, Instant> readBy;

    @Builder.Default
    @Field("is_deleted")
    private boolean isDeleted = false;

    @Field("deleted_at")
    private Instant deletedAt;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    // Embedded object: thông tin tệp đính kèm
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MediaAttachment {

        @Field("url")
        private String url;

        @Field("file_name")
        private String fileName;

        @Field("file_size")
        private Long fileSize;

        @Field("mime_type")
        private String mimeType;

        @Field("thumbnail_url")
        private String thumbnailUrl;
    }
}
