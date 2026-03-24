package com.example.appchatbackend.model;

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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "messages")
@CompoundIndexes({
    // Phân trang tin nhắn trong 1 hội thoại
    @CompoundIndex(name = "idx_msg_conv_created",
                   def = "{'conversation_id': 1, 'created_at': -1}"),
    // Tìm tin nhắn của 1 user trong 1 hội thoại
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
