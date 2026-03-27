package com.example.appchatbackend.model;

import jakarta.validation.constraints.*;
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

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 2, max = 100, message = "Tên đăng nhập phải từ 2 đến 100 ký tự")
    @Indexed(unique = true)
    @Field("username")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Indexed(unique = true)
    @Field("email")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    @Field("password_hash")
    private String passwordHash;

    @Size(min = 2, max = 100, message = "Tên hiển thị phải từ 2 đến 100 ký tự")
    @Field("display_name")
    private String displayName;

    @Field("avatar_url")
    private String avatarUrl;

    @Size(max = 255, message = "Bio không được quá 255 ký tự")
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
