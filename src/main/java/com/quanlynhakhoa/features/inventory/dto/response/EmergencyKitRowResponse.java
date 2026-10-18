package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.EmergencyKitStatus;
import com.quanlynhakhoa.features.inventory.enums.EmergencyKitType;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Một dòng bảng hộp cấp cứu.
 *
 * @param soMatHangDat mặt hàng đủ số lượng và còn hạn trên ngưỡng
 * @param tuKhoa       tên các mặt hàng trong hộp, để tìm kiếm theo tên thuốc
 */
public record EmergencyKitRowResponse(
        String maHop,
        String tenHop,
        EmergencyKitType loai,
        String viTri,
        int soMatHangDat,
        int soMatHang,
        LocalDateTime kiemTraGanNhat,
        LocalDate hanDungKeTiep,
        LocalDate hanKiemTraTiep,
        boolean denHanKiemTra,
        EmergencyKitStatus trangThai,
        String tuKhoa) {
}
