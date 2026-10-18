package com.quanlynhakhoa.features.inventory.ui;

import com.quanlynhakhoa.features.inventory.dto.response.StockOutDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockOutLineResponse;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/** In phiếu xuất kho khổ A4. */
final class StockOutPrinter {
    private StockOutPrinter() {
    }

    static void print(Component parent, StockOutDetailResponse detail) {
        VoucherPrinter.print(parent, document(detail));
    }

    static VoucherPrinter.Document document(StockOutDetailResponse d) {
        boolean done = d.trangThai() == VoucherStatus.DA_HOAN_THANH;
        List<String> info = new ArrayList<>();
        info.add("Lý do xuất: " + d.lyDo().label() + "   ·   Người nhận: " + d.nguoiNhan());
        info.add("Người lập: " + d.nguoiLap());
        if (d.nguoiXuatKho() != null) {
            info.add("Người xuất kho: " + d.nguoiXuatKho() + " — " + InventoryFormat.dateTime(d.thoiDiemXuatKho()));
        }
        if (d.ghiChu() != null) {
            info.add("Ghi chú: " + d.ghiChu());
        }
        List<List<String>> rows = new ArrayList<>();
        int stt = 1;
        for (StockOutLineResponse l : d.dong()) {
            rows.add(List.of(String.valueOf(stt++), l.maMatHang() + " " + l.tenMatHang(), l.soLo(),
                    InventoryFormat.date(l.hanSuDung()), String.valueOf(l.soLuong()), l.donViTinh(),
                    l.thanhTien() == null ? "—" : InventoryFormat.money(l.thanhTien())));
        }
        return new VoucherPrinter.Document("Phieu xuat " + d.maPhieu(), "PHIẾU XUẤT KHO",
                "Số: " + d.maPhieu() + "   ·   Ngày xuất: " + InventoryFormat.date(d.ngayXuat())
                        + "   ·   Trạng thái: " + d.trangThai().label(),
                info,
                List.of(new VoucherPrinter.Column("#", 0, false),
                        new VoucherPrinter.Column("Mặt hàng", 26, false),
                        new VoucherPrinter.Column("Số lô", 186, false),
                        new VoucherPrinter.Column("Hạn dùng", 250, false),
                        new VoucherPrinter.Column("SL", 322, true),
                        new VoucherPrinter.Column("ĐVT", 352, false),
                        new VoucherPrinter.Column("Thành tiền", 392, true)),
                rows, done ? "Tổng giá trị xuất: " + InventoryFormat.money(d.tongTien()) : "Chưa xuất kho — chưa chốt giá vốn",
                "Người nhận hàng", "Thủ kho");
    }
}
