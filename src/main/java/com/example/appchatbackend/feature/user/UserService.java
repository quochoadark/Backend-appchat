package com.example.appchatbackend.feature.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * UserService — interface định nghĩa nghiệp vụ quản lý người dùng.
 * Implementation: UserServiceImpl
 */
public interface UserService {

    /** Lấy toàn bộ danh sách user */
    List<User> findAll();

    /** Tìm kiếm user theo keyword (username / displayName), có phân trang */
    Page<User> search(String keyword, Pageable pageable);

    /** Tìm user theo ID, ném ResourceNotFoundException nếu không tìm thấy */
    User findById(String id);

    /** Tìm user theo email, ném ResourceNotFoundException nếu không tìm thấy */
    User findByEmail(String email);

    /** Tạo user mới: kiểm tra trùng email/username, hash mật khẩu, lưu DB */
    User create(User user);

    /** Cập nhật thông tin profile (displayName, email, avatarUrl, bio) */
    User update(String id, User user);

    /** Xóa user theo ID */
    void deleteById(String id);
}
