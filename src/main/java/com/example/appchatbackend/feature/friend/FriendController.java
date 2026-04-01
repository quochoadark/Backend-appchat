package com.example.appchatbackend.feature.friend;

import com.example.appchatbackend.exception.ResourceNotFoundException;
import com.example.appchatbackend.feature.friend.dto.SendFriendRequestDto;
import com.example.appchatbackend.feature.user.User;
import com.example.appchatbackend.feature.user.UserRepository;
import com.example.appchatbackend.helper.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/friends")
public class FriendController {

    private final FriendService friendService;
    private final UserRepository userRepository;

    public FriendController(FriendService friendService, UserRepository userRepository) {
        this.friendService = friendService;
        this.userRepository = userRepository;
    }

    // Lấy danh sách bạn bè
    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getFriends(@AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        List<User> friends = friendService.getFriends(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bạn bè thành công", friends));
    }

    // Lấy lời mời kết bạn nhận được (đang chờ)
    @GetMapping("/requests/received")
    public ResponseEntity<ApiResponse<List<FriendRequest>>> getReceivedRequests(@AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        List<FriendRequest> requests = friendService.getReceivedPendingRequests(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy lời mời nhận được thành công", requests));
    }

    // Lấy lời mời kết bạn đã gửi (đang chờ)
    @GetMapping("/requests/sent")
    public ResponseEntity<ApiResponse<List<FriendRequest>>> getSentRequests(@AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        List<FriendRequest> requests = friendService.getSentPendingRequests(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy lời mời đã gửi thành công", requests));
    }

    // Gửi lời mời kết bạn
    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<FriendRequest>> sendRequest(
            @Valid @RequestBody SendFriendRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        FriendRequest request = friendService.sendRequest(currentUserId, dto.getTargetUserId());
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Gửi lời mời kết bạn thành công", request));
    }

    // Chấp nhận lời mời kết bạn
    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<FriendRequest>> acceptRequest(
            @PathVariable String requestId,
            @AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        FriendRequest request = friendService.acceptRequest(requestId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Chấp nhận lời mời kết bạn thành công", request));
    }

    // Từ chối lời mời kết bạn
    @PostMapping("/requests/{requestId}/decline")
    public ResponseEntity<ApiResponse<FriendRequest>> declineRequest(
            @PathVariable String requestId,
            @AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        FriendRequest request = friendService.declineRequest(requestId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Từ chối lời mời kết bạn thành công", request));
    }

    // Hủy lời mời kết bạn đã gửi
    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<ApiResponse<Void>> cancelRequest(
            @PathVariable String requestId,
            @AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        friendService.cancelRequest(requestId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    // Hủy kết bạn
    @DeleteMapping("/{friendId}")
    public ResponseEntity<ApiResponse<Void>> unfriend(
            @PathVariable String friendId,
            @AuthenticationPrincipal Jwt jwt) {
        String currentUserId = getCurrentUserId(jwt);
        friendService.unfriend(currentUserId, friendId);
        return ResponseEntity.noContent().build();
    }

    private String getCurrentUserId(Jwt jwt) {
        String email = jwt.getSubject();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "email", email));
        return user.getId();
    }
}
