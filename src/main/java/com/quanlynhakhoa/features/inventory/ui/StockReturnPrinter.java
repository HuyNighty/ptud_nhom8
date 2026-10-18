package com.quanlynhakhoa.features.inventory.ui;

import com.quanlynhakhoa.features.inventory.dto.response.StockReturnDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockReturnLineResponse;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/** In phiếu hoàn trả kho khổ A4. */
final class StockReturnPrinter {
    private StockReturnPrinter() {
    }

    static void print(Component parent, StockReturnDetailResponse detail) {
        VoucherPrinter.print(parent, document(detail));
    }

    static VoucherPrinter.Document document(StockReturnDetailResponse d) {
        List<String> info = new ArrayList<>();
        info.add("Phiếu xuất gốc: " + d.maPhieuXuat() + "   ·   Lý do: " + d.lyDo().label()
                + (d.lyDo().restock() ? "" : " (không cộng lại tồn)"));
        info.add("Nguồn hoàn trả: " + d.nguonHoanTra() + "   ·   Người hoàn trả: " + d.nguoiHoanTra());
        info.add("Người lập: " + d.nguoiLap());
        if (d.nguoiDuyet() != null) {
            info.add("Người nhận (duyệt): " + d.nguoiDuyet() + " — " + InventoryFormat.dateTime(d.thoiDiemDuyet()));
        }
        if (d.ghiChu() != null) {
            info.add("Ghi chú: " + d.ghiChu());
        }
        List<List<String>> rows = new ArrayList<>();
        int stt = 1;
        for (StockReturnLineResponse l : d.dong()) {
            rows.add(List.of(String.valueOf(stt++), l.maMatHang() + " " + l.tenMatHang(), l.soLo(),
                    InventoryFormat.date(l.hanSuDung()), String.valueOf(l.soLuong()), l.donViTinh(),
                    InventoryFormat.money(l.thanhTien())));
        }
        return new VoucherPrinter.Document("Phieu hoan tra " + d.maPhieu(), "PHIẾU HOÀN TRẢ KHO",
                "Số: " + d.maPhieu() + "   ·   Ngày hoàn trả: " + InventoryFormat.date(d.ngayHoanTra())
                        + "   ·   Trạng thái: " + StockReturnPanel.statusLabel(d.trangThai()),
                info,
                List.of(new VoucherPrinter.Column("#", 0, false),
                        new VoucherPrinter.Column("Mặt hàng", 26, false),
                        new VoucherPrinter.Column("Số lô", 186, false),
                        new VoucherPrinter.Column("Hạn dùng", 250, false),
                        new VoucherPrinter.Column("SL", 322, true),
                        new VoucherPrinter.Column("ĐVT", 352, false),
                        new VoucherPrinter.Column("Thành tiền", 392, true)),
                rows, "Giá trị hoàn trả: " + InventoryFormat.money(d.tongTien()), "Người hoàn trả", "Thủ kho");
    }
}
