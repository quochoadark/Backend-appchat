package com.example.appchatbackend.exception;

import java.io.Serial;
import java.io.Serializable;

/**
 * ResourceNotFoundException — exception ném khi không tìm thấy tài nguyên trong DB (HTTP 404).
 *
 * Được GlobalExceptionHandler bắt và trả về response 404 Not Found chuẩn.
 * Extends RuntimeException: không cần khai báo throws ở mỗi hàm.
 * Implements Serializable: cần thiết cho exception class (best practice).
 *
 * Cách dùng:
 *   throw new ResourceNotFoundException("Người dùng", "id", userId);
 *   → message: "Không tìm thấy Người dùng với id = '123'"
 */
public class ResourceNotFoundException extends RuntimeException implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String resourceName;  // Tên loại tài nguyên: "Người dùng", "Tin nhắn"...
    private final String fieldName;     // Tên field dùng để tìm: "id", "email"...
    private final Object fieldValue;    // Giá trị đã tìm nhưng không thấy

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("Không tìm thấy %s với %s = '%s'", resourceName, fieldName, fieldValue));
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
