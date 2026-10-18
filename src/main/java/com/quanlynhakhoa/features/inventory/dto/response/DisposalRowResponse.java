package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.DisposalReason;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Một dòng trong danh sách phiếu hủy. {@code nguoiDuyet} null khi chưa duyệt. */
public record DisposalRowResponse(
        String maPhieu,
        LocalDate ngayHuy,
        DisposalReason lyDo,
        String moTa,
        String nguoiYeuCau,
        String nguoiDuyet,
        BigDecimal giaTri,
        VoucherStatus trangThai) {

    /** Lý do hiển thị: mô tả cụ thể nếu có, không thì nhãn lý do. */
    public String lyDoHienThi() {
        return moTa == null || moTa.isBlank() ? lyDo.label() : moTa;
    }
}
