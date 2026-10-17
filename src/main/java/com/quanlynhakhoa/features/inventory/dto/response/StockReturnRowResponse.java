package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.ReturnReason;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Một dòng trong danh sách phiếu hoàn trả. {@code nguoiNhan} = người duyệt nhận lại kho, null khi chưa duyệt. */
public record StockReturnRowResponse(
        String maPhieu,
        LocalDate ngayHoanTra,
        String maPhieuXuat,
        ReturnReason lyDo,
        String nguonHoanTra,
        String nguoiHoanTra,
        String nguoiNhan,
        BigDecimal tongTien,
        VoucherStatus trangThai) {
}
