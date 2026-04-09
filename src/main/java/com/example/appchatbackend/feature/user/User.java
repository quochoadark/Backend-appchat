package com.example.appchatbackend.feature.user;

import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * User — document MongoDB lưu thông tin người dùng, collection "users".
 *
 * @Data: Lombok tự tạo getter/setter/equals/hashCode/toString
 * @NoArgsConstructor: constructor không tham số (bắt buộc cho MongoDB deserialization)
 * @AllArgsConstructor: constructor đủ tham số (dùng với @Builder)
 * @Builder: cho phép tạo object theo kiểu User.builder()...build()
 * @Document: đánh dấu đây là MongoDB document, Spring Data MongoDB tự map
 *
 * @JsonProperty(WRITE_ONLY) trên passwordHash: field chỉ nhận từ client (khi đăng ký),
 * KHÔNG bao giờ trả ra ngoài trong JSON response → bảo mật mật khẩu.
 */
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

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
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
    @Field("friend_ids")
    private List<String> friendIds = new ArrayList<>();

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
