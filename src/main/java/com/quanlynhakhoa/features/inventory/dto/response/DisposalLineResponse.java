package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DisposalLineResponse(
        Integer maLo,
        InventoryItemType loai,
        String maMatHang,
        String tenMatHang,
        String donViTinh,
        String soLo,
        LocalDate hanSuDung,
        int tonHienTai,
        int soLuong,
        BigDecimal donGia,
        BigDecimal thanhTien) {
}
