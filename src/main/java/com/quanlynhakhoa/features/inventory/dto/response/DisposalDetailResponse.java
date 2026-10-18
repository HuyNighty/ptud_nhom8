package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.DisposalReason;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DisposalDetailResponse(
        String maPhieu,
        LocalDate ngayHuy,
        DisposalReason lyDo,
        String moTa,
        VoucherStatus trangThai,
        String maNhanVienLap,
        String nguoiYeuCau,
        LocalDateTime thoiDiemLap,
        String nguoiDuyet,
        LocalDateTime thoiDiemDuyet,
        String nguoiHuy,
        LocalDateTime thoiDiemHuy,
        String lyDoHuy,
        List<DisposalLineResponse> dong,
        BigDecimal giaTri) {
}
