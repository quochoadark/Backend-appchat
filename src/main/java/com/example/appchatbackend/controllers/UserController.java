// Khai báo package chứa class này, giúp tổ chức code và phân nhóm theo chức năng
package com.example.appchatbackend.controllers;

// --- CÁC IMPORT ---
// Import Service để sử dụng các logic nghiệp vụ liên quan đến User
import com.example.appchatbackend.services.UserService;
// Import Model User (đại diện cho đối tượng người dùng trong CSDL)
import com.example.appchatbackend.models.User;
// Import lớp helper dùng để chuẩn hóa định dạng dữ liệu trả về cho tất cả API
import com.example.appchatbackend.helper.ApiResponse;

// Các import của Spring Data để hỗ trợ Phân trang (Pagination) và Sắp xếp (Sorting)
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
// Import ResponseEntity để bao bọc HTTP Response (status code, header, body)
import org.springframework.http.ResponseEntity;
// Import annotation để validate (kiểm tra tính hợp lệ) dữ liệu đầu vào
import jakarta.validation.Valid;
// Import các annotation của Spring Web để xây dựng RESTful API
import org.springframework.web.bind.annotation.*;
// Import công cụ để tạo đường dẫn URI động
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * UserController — REST API quản lý người dùng.
 * Cung cấp các endpoint:
 * - GET    /users           → lấy tất cả user
 * - GET    /users/search    → tìm kiếm user theo keyword (phân trang)
 * - GET    /users/{id}      → lấy thông tin 1 user
 * - POST   /users           → tạo user mới (admin use case)
 * - PUT    /users/{id}      → cập nhật thông tin user
 * - DELETE /users/{id}      → xóa user
 */
// Đánh dấu class này là Controller của REST API. Tự động chuyển data trả về thành JSON.
@RestController
// Định nghĩa URL gốc cho tất cả các API trong class này là "/users"
@RequestMapping("/users")
public class UserController {

    // Khai báo UserService. Dùng 'final' để đảm bảo biến không bị thay đổi sau khi khởi tạo (Best practice)
    private final UserService userService;

    // Constructor Injection: Spring Boot sẽ tự động tiêm (inject) đối tượng UserService vào khi khởi tạo Controller
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // --- API: Lấy danh sách tất cả người dùng ---
    // Bắt các request HTTP GET gọi tới "/users"
    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        // Gọi Service lấy danh sách từ Database
        List<User> users = userService.findAll();
        // Trả về HTTP 200 (OK) với dữ liệu bọc trong ApiResponse
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công", users));
    }

    // --- API: Tìm kiếm người dùng (có phân trang) ---
    // Bắt request GET tới "/users/search"
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<User>>> searchUsers(
            // Lấy tham số 'keyword' từ URL, mặc định là chuỗi rỗng
            @RequestParam(defaultValue = "") String keyword,
            // Lấy tham số 'page' (trang hiện tại), mặc định là trang đầu (0)
            @RequestParam(defaultValue = "0") int page,
            // Lấy tham số 'size' (số kết quả/trang), mặc định là 10
            @RequestParam(defaultValue = "10") int size) {
        
        // Tạo cấu hình phân trang, sắp xếp theo 'username' tăng dần
        Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());
        // Gọi Service tìm kiếm dựa theo keyword và phân trang
        Page<User> result = userService.search(keyword, pageable);
        // Trả về kết quả JSON HTTP 200 OK
        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm thành công", result));
    }

    // --- API: Lấy thông tin 1 user cụ thể ---
    // Bắt request GET tới "/users/{id}"
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUserById(
            // Trích xuất '{id}' từ URL truyền vào biến String id
            @PathVariable String id) {
        // Gọi Service tìm user theo ID
        User user = userService.findById(id);
        // Trả về kết quả
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", user));
    }

    // --- API: Tạo user mới ---
    // Bắt request POST tới "/users"
    @PostMapping
    public ResponseEntity<ApiResponse<User>> createUser(
            // @Valid: Kích hoạt validate dữ liệu
            // @RequestBody: Chuyển JSON từ Request Body thành đối tượng User
            @Valid @RequestBody User user) {
        // Gọi Service lưu dữ liệu
        User created = userService.create(user);
        
        // Tạo đường dẫn URI trỏ tới user vừa tạo (VD: /users/123)
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        
        // Trả về HTTP 201 (Created) kèm link URI và dữ liệu đã tạo
        return ResponseEntity.created(location)
                .body(ApiResponse.created("Tạo người dùng thành công", created));
    }

    // --- API: Cập nhật user ---
    // Bắt request PUT tới "/users/{id}"
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> updateUser(
            @PathVariable String id, 
            @Valid @RequestBody User user) {
        // Gọi Service cập nhật thông tin
        User updated = userService.update(id, user);
        // Trả về HTTP 200 OK sau khi cập nhật thành công
        return ResponseEntity.ok(ApiResponse.success("Cập nhật người dùng thành công", updated));
    }

    // --- API: Xóa user ---
    // Bắt request DELETE tới "/users/{id}"
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String id) {
        // Gọi Service xóa user
        userService.deleteById(id);
        // Trả về HTTP 204 (No Content) báo hiệu xóa thành công nhưng không có body
        return ResponseEntity.noContent().build();
    }
}
