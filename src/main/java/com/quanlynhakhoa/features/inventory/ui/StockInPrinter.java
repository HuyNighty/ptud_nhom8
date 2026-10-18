package com.quanlynhakhoa.features.inventory.ui;

import com.quanlynhakhoa.features.inventory.dto.response.StockInDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockInLineResponse;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/** In phiếu nhập kho khổ A4. */
final class StockInPrinter {
    private StockInPrinter() {
    }

    static void print(Component parent, StockInDetailResponse detail) {
        VoucherPrinter.print(parent, document(detail));
    }

    static VoucherPrinter.Document document(StockInDetailResponse d) {
        List<String> info = new ArrayList<>();
        info.add("Nhà cung cấp: " + d.tenNhaCungCap());
        info.add("Người lập: " + d.nguoiLap());
        if (d.nguoiNhapKho() != null) {
            info.add("Người nhập kho: " + d.nguoiNhapKho() + " — " + InventoryFormat.dateTime(d.thoiDiemNhapKho()));
        }
        if (d.ghiChu() != null) {
            info.add("Ghi chú: " + d.ghiChu());
        }
        List<List<String>> rows = new ArrayList<>();
        int stt = 1;
        for (StockInLineResponse l : d.dong()) {
            rows.add(List.of(String.valueOf(stt++), l.maMatHang() + " " + l.tenMatHang(), l.soLo(),
                    InventoryFormat.date(l.hanSuDung()), String.valueOf(l.soLuong()), l.donViTinh(),
                    InventoryFormat.money(l.thanhTien())));
        }
        return new VoucherPrinter.Document("Phieu nhap " + d.maPhieu(), "PHIẾU NHẬP KHO",
                "Số: " + d.maPhieu() + "   ·   Ngày nhập: " + InventoryFormat.date(d.ngayNhap())
                        + "   ·   Trạng thái: " + d.trangThai().label(),
                info,
                List.of(new VoucherPrinter.Column("#", 0, false),
                        new VoucherPrinter.Column("Mặt hàng", 26, false),
                        new VoucherPrinter.Column("Số lô", 186, false),
                        new VoucherPrinter.Column("Hạn dùng", 250, false),
                        new VoucherPrinter.Column("SL", 322, true),
                        new VoucherPrinter.Column("ĐVT", 352, false),
                        new VoucherPrinter.Column("Thành tiền", 392, true)),
                rows, "Tổng tiền: " + InventoryFormat.money(d.tongTien()), "Người lập phiếu", "Thủ kho");
    }
}
