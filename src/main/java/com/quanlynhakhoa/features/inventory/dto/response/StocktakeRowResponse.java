package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.StocktakeScope;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import java.time.LocalDate;

/**
 * Một dòng trong danh sách đợt kiểm kê.
 * {@code thieu}/{@code thua} = tổng số đơn vị thiếu/thừa trên các lô đã đếm.
 */
public record StocktakeRowResponse(
        String maKiemKe,
        LocalDate ngayKiemKe,
        StocktakeScope phamVi,
        String nguoiKiemKe,
        int tongMatHang,
        int soLo,
        int daDem,
        int thieu,
        int thua,
        VoucherStatus trangThai) {

    public boolean lech() {
        return thieu > 0 || thua > 0;
    }
}
