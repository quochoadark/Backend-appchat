package com.example.appchatbackend.feature.friend;

import com.example.appchatbackend.feature.user.User;

import java.util.List;

public interface FriendService {

    FriendRequest sendRequest(String senderId, String receiverId);

    FriendRequest acceptRequest(String requestId, String currentUserId);

    FriendRequest declineRequest(String requestId, String currentUserId);

    void cancelRequest(String requestId, String currentUserId);

    void unfriend(String userId, String friendId);

    List<User> getFriends(String userId);

    List<FriendRequest> getReceivedPendingRequests(String userId);

    List<FriendRequest> getSentPendingRequests(String userId);
}
