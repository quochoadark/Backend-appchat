package com.example.appchatbackend.feature.conversation;

import com.example.appchatbackend.feature.message.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "conversations")
@CompoundIndexes({
    @CompoundIndex(name = "idx_conv_participants_updated",
                   def = "{'participants': 1, 'updated_at': -1}")
})
public class Conversation {

    @Id
    private String id;

    @Field("type")
    private ConversationType type;

    @Field("participants")
    private List<String> participants;

    // Chỉ dùng cho GROUP
    @Field("name")
    private String name;

    @Field("avatar_url")
    private String avatarUrl;

    @Field("description")
    private String description;

    @Field("created_by")
    private String createdBy;

    @Field("admin_ids")
    private List<String> adminIds;

    @Field("last_message")
    private LastMessageSnapshot lastMessage;

    @Builder.Default
    @Field("is_active")
    private boolean isActive = true;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;

    // Embedded object: snapshot tin nhắn cuối để hiển thị danh sách hội thoại
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LastMessageSnapshot {

        @Field("message_id")
        private String messageId;

        @Field("sender_id")
        private String senderId;

        @Field("sender_display_name")
        private String senderDisplayName;

        @Field("content_preview")
        private String contentPreview;

        @Field("message_type")
        private MessageType messageType;

        @Field("sent_at")
        private Instant sentAt;
    }
}
