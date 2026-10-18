package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.StocktakeScope;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record StocktakeDetailResponse(
        String maKiemKe,
        LocalDate ngayKiemKe,
        StocktakeScope phamVi,
        VoucherStatus trangThai,
        String ghiChu,
        String maNhanVienLap,
        String nguoiLap,
        LocalDateTime thoiDiemLap,
        String nguoiDuyet,
        LocalDateTime thoiDiemDuyet,
        String nguoiHuy,
        LocalDateTime thoiDiemHuy,
        String lyDoHuy,
        List<StocktakeLineResponse> dong,
        int tongMatHang,
        int daDem,
        int thieu,
        int thua) {
}
