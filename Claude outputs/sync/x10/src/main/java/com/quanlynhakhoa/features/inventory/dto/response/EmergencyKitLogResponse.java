package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.KitLogType;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Một lần kiểm tra / bổ sung / thay thế hộp cấp cứu.
 *
 * @param dat null với bổ sung / thay thế
 */
public record EmergencyKitLogResponse(
        Integer maNhatKy,
        String maHop,
        String tenHop,
        KitLogType loai,
        Boolean dat,
        String maPhieuXuat,
        String nguoiThucHien,
        LocalDateTime thoiDiem,
        String ghiChu,
        List<EmergencyKitLogLineResponse> dong) {
}
