package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Lô còn tồn có thể đưa vào phiếu hủy (kể cả lô đã hết hạn). {@code donGia} = giá nhập gần nhất của lô. */
public record DisposalLotOption(
        Integer maLo,
        InventoryItemType loai,
        String maMatHang,
        String tenMatHang,
        String donViTinh,
        String soLo,
        LocalDate hanSuDung,
        int tonKho,
        boolean hetHan,
        BigDecimal donGia) {
}
