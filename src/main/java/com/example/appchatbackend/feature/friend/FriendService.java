package com.example.appchatbackend.feature.friend;

import com.example.appchatbackend.feature.user.User;

import java.util.List;

/**
 * FriendService — interface nghiep vu quan ly ket ban.
 * Implementation: FriendServiceImpl
 */
public interface FriendService {

    /** Gui loi moi ket ban → kiem tra trung, kiem tra da la ban truoc khi tao */
    FriendRequest sendRequest(String senderId, String receiverId);

    /** Chap nhan loi moi → cap nhat trang thai + them nhau vao friend_ids (atomic $addToSet) */
    FriendRequest acceptRequest(String requestId, String currentUserId);

    /** Tu choi loi moi → cap nhat trang thai thanh DECLINED */
    FriendRequest declineRequest(String requestId, String currentUserId);

    /** Huy loi moi da gui → xoa document khoi DB (chi nguoi gui moi huy duoc) */
    void cancelRequest(String requestId, String currentUserId);

    /** Huy ket ban → xoa nhau khoi friend_ids cua ca 2 (atomic $pull) */
    void unfriend(String userId, String friendId);

    /** Lay danh sach ban be cua user */
    List<User> getFriends(String userId);

    /** Lay cac loi moi ket ban MA USER DA NHAN (dang cho phan hoi) */
    List<FriendRequest> getReceivedPendingRequests(String userId);

    /** Lay cac loi moi ket ban NGUOI DUNG DA GUI (dang cho phan hoi) */
    List<FriendRequest> getSentPendingRequests(String userId);
}
