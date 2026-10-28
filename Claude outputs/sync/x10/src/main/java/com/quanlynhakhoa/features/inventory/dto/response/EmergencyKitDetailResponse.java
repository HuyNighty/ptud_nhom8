package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.EmergencyKitStatus;
import com.quanlynhakhoa.features.inventory.enums.EmergencyKitType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record EmergencyKitDetailResponse(
        String maHop,
        String tenHop,
        EmergencyKitType loai,
        String viTri,
        int chuKyKiemTraNgay,
        int nguongHanDungNgay,
        String ghiChu,
        String nguoiTao,
        LocalDateTime thoiDiemTao,
        EmergencyKitStatus trangThai,
        LocalDateTime kiemTraGanNhat,
        String nguoiKiemTraGanNhat,
        LocalDate hanKiemTraTiep,
        boolean denHanKiemTra,
        List<EmergencyKitItemResponse> matHang,
        List<EmergencyKitLogResponse> nhatKy) {
}
