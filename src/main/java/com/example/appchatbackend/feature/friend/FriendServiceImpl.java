package com.example.appchatbackend.feature.friend;

import com.example.appchatbackend.exception.DuplicateResourceException;
import com.example.appchatbackend.exception.ResourceNotFoundException;
import com.example.appchatbackend.feature.user.User;
import com.example.appchatbackend.feature.user.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FriendServiceImpl implements FriendService {

    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;

    public FriendServiceImpl(FriendRequestRepository friendRequestRepository, UserRepository userRepository) {
        this.friendRequestRepository = friendRequestRepository;
        this.userRepository = userRepository;
    }

    @Override
    public FriendRequest sendRequest(String senderId, String receiverId) {
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("Không thể gửi lời mời kết bạn cho chính mình");
        }
        // Kiểm tra người nhận tồn tại
        userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", receiverId));

        // Kiểm tra đã là bạn bè chưa
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", senderId));
        if (sender.getFriendIds() != null && sender.getFriendIds().contains(receiverId)) {
            throw new DuplicateResourceException("Quan hệ bạn bè", "userId", receiverId);
        }

        // Kiểm tra đã có lời mời PENDING chưa (cả 2 chiều)
        if (friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(senderId, receiverId, FriendRequestStatus.PENDING)
                || friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(receiverId, senderId, FriendRequestStatus.PENDING)) {
            throw new DuplicateResourceException("Lời mời kết bạn", "giữa 2 người dùng", senderId + " & " + receiverId);
        }

        FriendRequest request = FriendRequest.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .status(FriendRequestStatus.PENDING)
                .build();
        return friendRequestRepository.save(request);
    }

    @Override
    public FriendRequest acceptRequest(String requestId, String currentUserId) {
        FriendRequest request = findRequestOrThrow(requestId);
        if (!request.getReceiverId().equals(currentUserId)) {
            throw new AccessDeniedException("Bạn không có quyền chấp nhận lời mời này");
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalStateException("Lời mời này không còn ở trạng thái PENDING");
        }

        request.setStatus(FriendRequestStatus.ACCEPTED);
        friendRequestRepository.save(request);

        // Thêm vào friendIds của cả 2 người
        addFriend(request.getSenderId(), request.getReceiverId());
        addFriend(request.getReceiverId(), request.getSenderId());

        return request;
    }

    @Override
    public FriendRequest declineRequest(String requestId, String currentUserId) {
        FriendRequest request = findRequestOrThrow(requestId);
        if (!request.getReceiverId().equals(currentUserId)) {
            throw new AccessDeniedException("Bạn không có quyền từ chối lời mời này");
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalStateException("Lời mời này không còn ở trạng thái PENDING");
        }
        request.setStatus(FriendRequestStatus.DECLINED);
        return friendRequestRepository.save(request);
    }

    @Override
    public void cancelRequest(String requestId, String currentUserId) {
        FriendRequest request = findRequestOrThrow(requestId);
        if (!request.getSenderId().equals(currentUserId)) {
            throw new AccessDeniedException("Bạn không có quyền hủy lời mời này");
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalStateException("Lời mời này không còn ở trạng thái PENDING");
        }
        friendRequestRepository.delete(request);
    }

    @Override
    public void unfriend(String userId, String friendId) {
        removeFriend(userId, friendId);
        removeFriend(friendId, userId);
    }

    @Override
    public List<User> getFriends(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", userId));
        if (user.getFriendIds() == null || user.getFriendIds().isEmpty()) {
            return List.of();
        }
        return userRepository.findByIdIn(user.getFriendIds());
    }

    @Override
    public List<FriendRequest> getReceivedPendingRequests(String userId) {
        return friendRequestRepository.findByReceiverIdAndStatus(userId, FriendRequestStatus.PENDING);
    }

    @Override
    public List<FriendRequest> getSentPendingRequests(String userId) {
        return friendRequestRepository.findBySenderIdAndStatus(userId, FriendRequestStatus.PENDING);
    }

    private FriendRequest findRequestOrThrow(String requestId) {
        return friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Lời mời kết bạn", "id", requestId));
    }

    private void addFriend(String userId, String friendId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", userId));
        if (user.getFriendIds() == null) {
            user.setFriendIds(new ArrayList<>());
        }
        if (!user.getFriendIds().contains(friendId)) {
            user.getFriendIds().add(friendId);
            userRepository.save(user);
        }
    }

    private void removeFriend(String userId, String friendId) {
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getFriendIds() != null) {
                user.getFriendIds().remove(friendId);
                userRepository.save(user);
            }
        });
    }
}
