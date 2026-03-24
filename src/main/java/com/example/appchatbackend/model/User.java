package com.example.appchatbackend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Set;

@Data // Tự tạo get set
@NoArgsConstructor // tạo constructor không tham số và đủ tham số
@AllArgsConstructor //
@Builder
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true) // không trùng lặp
    @Field("username")
    private String username;

    @Indexed(unique = true)
    @Field("email")
    private String email;

    @Field("password_hash")
    private String passwordHash;

    @Field("display_name")
    private String displayName;

    @Field("avatar_url")
    private String avatarUrl;

    @Field("bio")
    private String bio;

    @Field("roles")
    private Set<String> roles;

    @Field("is_online")
    private Boolean isOnline;

    @Field("last_seen_at")
    private Instant lastSeenAt;

    @Builder.Default
    @Field("is_active")
    private Boolean isActive = true;

    @CreatedDate // Tự động điền thời gian hiện tại khi document được tạo mới lần đầu.
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate // Tự động cập nhật thời gian mỗi khi document có sự thay đổi/update.
    @Field("updated_at")
    private Instant updatedAt;
}
