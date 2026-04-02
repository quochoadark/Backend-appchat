package com.example.appchatbackend.feature.user;

import com.example.appchatbackend.helper.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userService.findAll();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công", users));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<User>>> searchUsers(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());
        Page<User> result = userService.search(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm thành công", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable String id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", user));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<User>> createUser(@Valid @RequestBody User user) {
        User created = userService.create(user);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.created("Tạo người dùng thành công", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable String id, @Valid @RequestBody User user) {
        User updated = userService.update(id, user);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật người dùng thành công", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
