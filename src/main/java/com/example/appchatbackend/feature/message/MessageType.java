package com.example.appchatbackend.feature.message;

public enum MessageType {
    TEXT,    // Tin nhắn văn bản
    IMAGE,   // Ảnh đính kèm
    FILE,    // Tệp đính kèm
    SYSTEM   // Tin nhắn hệ thống: "A đã tham gia nhóm", "B bị xóa khỏi nhóm"
}
