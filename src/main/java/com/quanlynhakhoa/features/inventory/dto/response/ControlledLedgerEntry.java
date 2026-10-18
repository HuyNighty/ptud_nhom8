package com.quanlynhakhoa.features.inventory.dto.response;

import java.time.LocalDateTime;

/**
 * Một dòng sổ theo dõi thuốc kiểm soát đặc biệt: mọi biến động tồn đã được ghi sổ
 * (nhập kho, xuất kho, hoàn trả nhập lại, điều chỉnh kiểm kê, hủy). {@code tonSau} = tồn của thuốc sau dòng này.
 */
public record ControlledLedgerEntry(
        LocalDateTime thoiDiem,
        String maChungTu,
        String loai,
        String maThuoc,
        String tenThuoc,
        String soLo,
        int nhap,
        int xuat,
        int tonSau,
        String doiTac,
        String nguoiThucHien) {
}
