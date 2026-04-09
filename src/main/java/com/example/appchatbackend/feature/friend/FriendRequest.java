package com.example.appchatbackend.feature.friend;

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

/**
 * FriendRequest — document MongoDB luu loi moi ket ban, collection "friend_requests".
 *
 * Vong doi: PENDING → ACCEPTED (them vao friend_ids cua ca 2) hoac DECLINED / bi huy.
 *
 * CompoundIndex unique (sender_id + receiver_id):
 * - Dam bao moi cap (A, B) chi co 1 loi moi tai 1 thoi diem
 * - Ngan gui loi moi trung (ca 2 chieu)
 * - FriendServiceImpl kiem tra ca chieu nguoc lai truoc khi cho phep gui
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "friend_requests")
@CompoundIndexes({
    @CompoundIndex(name = "idx_friend_req_sender_receiver",
                   def = "{'sender_id': 1, 'receiver_id': 1}", unique = true)
})
public class FriendRequest {

    @Id
    private String id;

    @Field("sender_id")
    private String senderId;

    @Field("receiver_id")
    private String receiverId;

    @Builder.Default
    @Field("status")
    private FriendRequestStatus status = FriendRequestStatus.PENDING;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;
}
