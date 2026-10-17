package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import java.time.LocalDate;

/** Một lô trong đợt kiểm kê. {@code thucTe}/{@code chenhLech} null khi chưa đếm. */
public record StocktakeLineResponse(
        Integer maLo,
        InventoryItemType loai,
        String maMatHang,
        String tenMatHang,
        String donViTinh,
        String soLo,
        LocalDate hanSuDung,
        int soSach,
        Integer thucTe,
        Integer chenhLech,
        String ghiChu) {
}
