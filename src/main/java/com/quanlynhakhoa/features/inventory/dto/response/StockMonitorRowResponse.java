package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.enums.StockLevel;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Một mặt hàng trên màn Tồn kho.
 *
 * @param tonThucTe  tổng tồn các lô (kể cả lô đã hết hạn chờ hủy)
 * @param tonKhaDung tồn các lô chưa hết hạn — dùng để so với định mức
 * @param giaTri     tồn thực tế × giá nhập gần nhất của từng lô
 */
public record StockMonitorRowResponse(
        InventoryItemType loai,
        String ma,
        String ten,
        String donViTinh,
        int tonThucTe,
        int tonKhaDung,
        int dinhMucToiThieu,
        String viTriLuuTru,
        StockLevel mucTon,
        boolean daDatHang,
        LocalDate hanGanNhat,
        BigDecimal giaTri) {

    public int tonHetHan() {
        return tonThucTe - tonKhaDung;
    }
}
