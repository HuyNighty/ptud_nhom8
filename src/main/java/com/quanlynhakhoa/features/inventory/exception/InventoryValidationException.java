package com.quanlynhakhoa.features.inventory.exception;

/**
 * Dữ liệu danh mục kho không hợp lệ hoặc vi phạm quy tắc nghiệp vụ.
 * Thông điệp tiếng Việt, hiển thị trực tiếp được cho người dùng.
 */
public final class InventoryValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InventoryValidationException(String message) {
        super(message);
    }
}
