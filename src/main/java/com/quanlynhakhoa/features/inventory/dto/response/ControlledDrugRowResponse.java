package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.ControlledDrugGroup;
import com.quanlynhakhoa.features.inventory.enums.NoticeSeverity;
import java.time.LocalDate;

/**
 * Một thuốc kiểm soát đặc biệt trong danh sách.
 * {@code tonKho} = tổng tồn vật lý (kể cả lô hết hạn chưa hủy); {@code hanGanNhat} = hạn sớm nhất của lô còn tồn.
 * {@code trangThai}/{@code mucDo}: "An toàn" (INFO), "Sắp hết" (WARNING), "Thiếu", "Cận hạn", "Hết hạn",
 * "Hết hàng" hoặc ghép "Thiếu/Cận hạn" (CRITICAL).
 */
public record ControlledDrugRowResponse(
        String maThuoc,
        String tenThuoc,
        String hamLuong,
        String hoatChat,
        ControlledDrugGroup nhom,
        String donViTinh,
        int dinhMucToiThieu,
        int tonKho,
        LocalDate hanGanNhat,
        String trangThai,
        NoticeSeverity mucDo,
        boolean thieuHang,
        boolean canHan) {

    /** "Diazepam 5mg/2ml". */
    public String tenDayDu() {
        return hamLuong == null || hamLuong.isBlank() ? tenThuoc : tenThuoc + " " + hamLuong;
    }
}
