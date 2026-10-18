package com.quanlynhakhoa.features.inventory.ui;

import com.quanlynhakhoa.features.inventory.dto.response.StocktakeDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StocktakeLineResponse;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/** In biên bản kiểm kê khổ A4. */
final class StocktakePrinter {
    private StocktakePrinter() {
    }

    static void print(Component parent, StocktakeDetailResponse detail) {
        VoucherPrinter.print(parent, document(detail));
    }

    static VoucherPrinter.Document document(StocktakeDetailResponse d) {
        List<String> info = new ArrayList<>();
        info.add("Phạm vi: " + d.phamVi().label() + "   ·   Người lập: " + d.nguoiLap()
                + " — sổ sách chụp lúc " + InventoryFormat.dateTime(d.thoiDiemLap()));
        if (d.nguoiDuyet() != null) {
            info.add("Người duyệt: " + d.nguoiDuyet() + " — " + InventoryFormat.dateTime(d.thoiDiemDuyet()));
        }
        if (d.ghiChu() != null) {
            info.add("Ghi chú: " + d.ghiChu());
        }
        List<List<String>> rows = new ArrayList<>();
        int stt = 1;
        for (StocktakeLineResponse l : d.dong()) {
            rows.add(List.of(String.valueOf(stt++), l.maMatHang() + " " + l.tenMatHang(), l.soLo(),
                    String.valueOf(l.soSach()), l.thucTe() == null ? "" : String.valueOf(l.thucTe()),
                    l.chenhLech() == null ? "" : l.chenhLech() > 0 ? "+" + l.chenhLech() : String.valueOf(l.chenhLech()),
                    l.ghiChu() == null ? "" : l.ghiChu()));
        }
        return new VoucherPrinter.Document("Bien ban kiem ke " + d.maKiemKe(), "BIÊN BẢN KIỂM KÊ KHO",
                "Số: " + d.maKiemKe() + "   ·   Ngày kiểm kê: " + InventoryFormat.date(d.ngayKiemKe())
                        + "   ·   Trạng thái: " + StocktakePanel.statusLabel(d.trangThai()),
                info,
                List.of(new VoucherPrinter.Column("#", 0, false),
                        new VoucherPrinter.Column("Mặt hàng", 22, false),
                        new VoucherPrinter.Column("Số lô", 170, false),
                        new VoucherPrinter.Column("Sổ sách", 222, true),
                        new VoucherPrinter.Column("Thực tế", 268, true),
                        new VoucherPrinter.Column("Lệch", 314, true),
                        new VoucherPrinter.Column("Lý do chênh lệch", 350, false)),
                rows, d.tongMatHang() + " mặt hàng · " + d.dong().size() + " lô · "
                        + StocktakePanel.differenceText(d.thieu(), d.thua()),
                "Người kiểm kê", "Người duyệt");
    }
}
