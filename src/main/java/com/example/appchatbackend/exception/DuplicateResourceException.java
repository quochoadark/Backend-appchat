package com.example.appchatbackend.exception;

import java.io.Serial;
import java.io.Serializable;

/**
 * DuplicateResourceException — exception ném khi tài nguyên đã tồn tại (HTTP 409 Conflict).
 *
 * Được GlobalExceptionHandler bắt và trả về response 409 Conflict chuẩn.
 * Dùng khi user đăng ký email đã có, tạo username trùng, gửi lời mời kết bạn trùng...
 *
 * Cách dùng:
 *   throw new DuplicateResourceException("Người dùng", "email", email);
 *   → message: "Người dùng đã tồn tại với email = 'abc@gmail.com'"
 */
public class DuplicateResourceException extends RuntimeException implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String resourceName;  // Tên loại tài nguyên: "Người dùng", "Lời mời kết bạn"...
    private final String fieldName;     // Tên field bị trùng: "email", "username"...
    private final Object fieldValue;    // Giá trị bị trùng

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s đã tồn tại với %s = '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }
}
