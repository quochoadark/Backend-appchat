package com.example.appchatbackend.feature.friend;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * FriendRequestRepository — truy cap MongoDB collection "friend_requests".
 * Spring Data tu sinh implementation tu ten method.
 */
@Repository
public interface FriendRequestRepository extends MongoRepository<FriendRequest, String> {

    Optional<FriendRequest> findBySenderIdAndReceiverId(String senderId, String receiverId);

    List<FriendRequest> findByReceiverIdAndStatus(String receiverId, FriendRequestStatus status);

    List<FriendRequest> findBySenderIdAndStatus(String senderId, FriendRequestStatus status);

    boolean existsBySenderIdAndReceiverIdAndStatus(String senderId, String receiverId, FriendRequestStatus status);

    // Tìm request giữa 2 người bất kể chiều gửi
    Optional<FriendRequest> findBySenderIdAndReceiverIdAndStatus(String senderId, String receiverId, FriendRequestStatus status);
}
