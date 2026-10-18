package com.quanlynhakhoa.features.inventory.ui;

import com.quanlynhakhoa.features.inventory.dto.response.DisposalDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.DisposalLineResponse;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/** In phiếu hủy / hao hụt khổ A4. */
final class DisposalPrinter {
    private DisposalPrinter() {
    }

    static void print(Component parent, DisposalDetailResponse detail) {
        VoucherPrinter.print(parent, document(detail));
    }

    static VoucherPrinter.Document document(DisposalDetailResponse d) {
        List<String> info = new ArrayList<>();
        info.add("Lý do: " + d.lyDo().label() + (d.moTa() == null ? "" : " — " + d.moTa()));
        info.add("Người yêu cầu: " + d.nguoiYeuCau() + " — " + InventoryFormat.dateTime(d.thoiDiemLap()));
        if (d.nguoiDuyet() != null) {
            info.add("Người duyệt: " + d.nguoiDuyet() + " — " + InventoryFormat.dateTime(d.thoiDiemDuyet()));
        }
        List<List<String>> rows = new ArrayList<>();
        int stt = 1;
        for (DisposalLineResponse l : d.dong()) {
            rows.add(List.of(String.valueOf(stt++), l.maMatHang() + " " + l.tenMatHang(), l.soLo(),
                    InventoryFormat.date(l.hanSuDung()), String.valueOf(l.soLuong()), l.donViTinh(),
                    InventoryFormat.money(l.thanhTien())));
        }
        return new VoucherPrinter.Document("Phieu huy " + d.maPhieu(), "PHIẾU HỦY / HAO HỤT",
                "Số: " + d.maPhieu() + "   ·   Ngày hủy: " + InventoryFormat.date(d.ngayHuy())
                        + "   ·   Trạng thái: " + DisposalPanel.statusLabel(d.trangThai()),
                info,
                List.of(new VoucherPrinter.Column("#", 0, false),
                        new VoucherPrinter.Column("Mặt hàng", 26, false),
                        new VoucherPrinter.Column("Số lô", 186, false),
                        new VoucherPrinter.Column("Hạn dùng", 250, false),
                        new VoucherPrinter.Column("SL", 322, true),
                        new VoucherPrinter.Column("ĐVT", 352, false),
                        new VoucherPrinter.Column("Thành tiền", 392, true)),
                rows, "Giá trị hủy: " + InventoryFormat.money(d.giaTri()), "Người yêu cầu", "Người duyệt");
    }
}
