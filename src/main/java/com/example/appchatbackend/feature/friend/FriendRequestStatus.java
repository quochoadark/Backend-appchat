package com.example.appchatbackend.feature.friend;

/**
 * FriendRequestStatus — trang thai cua loi moi ket ban.
 *
 * PENDING  → da gui, cho phan hoi
 * ACCEPTED → da chap nhan → ca 2 tro thanh ban, friend_ids duoc cap nhat
 * DECLINED → bi tu choi → co the gui lai sau
 */
public enum FriendRequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}
