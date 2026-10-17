package com.quanlynhakhoa.features.inventory.service;

import com.quanlynhakhoa.features.auth.enums.Role;
import java.util.Objects;
import java.util.Set;

/**
 * Người đang thao tác, lấy từ phiên đăng nhập hợp lệ (do shell/Login của Huy cung cấp),
 * không lấy từ dữ liệu người dùng nhập trên form.
 *
 * @param maNhanVien mã nhân viên ghi vào chứng từ (người lập / người nhập kho / người hủy)
 */
public record InventoryActor(Set<Role> roles, String maNhanVien, String hoTen) {
    public InventoryActor {
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles must not be null"));
    }
}
