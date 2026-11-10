package com.quanlynhakhoa.nghiepvu.hoadon;

import com.quanlynhakhoa.enums.TrangThaiHoaDon;
import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import com.quanlynhakhoa.model.hoadon.DuLieuChiTietHoaDon;
import com.quanlynhakhoa.model.hoadon.ThuocKeTinhTien;
import com.quanlynhakhoa.model.hoadon.DongChiTietHoaDon;
import com.quanlynhakhoa.nghiepvu.hoadon.ui.InvoicePanel;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.*;
import static com.quanlynhakhoa.nghiepvu.hoadon.ServiceBillingSelfTest.*;

public final class InvoiceMedicineSelfTest {
    public static void main(String[] args) throws Exception {
        var medicine = new com.quanlynhakhoa.entity.Thuoc("TESTPR", "Thuốc thử", "Viên", false, null, null, 0, false);
        com.quanlynhakhoa.nghiepvu.kho.thuoc.service.ThuocService.kiemTraVaChuanHoa(medicine);
        medicine.setDonGiaBan(BigDecimal.ZERO);
        com.quanlynhakhoa.nghiepvu.kho.thuoc.service.ThuocService.kiemTraVaChuanHoa(medicine);
        for (String price : new String[]{"-1", "1.5", "1000000000000000000"}) {
            medicine.setDonGiaBan(new BigDecimal(price));
            expect(IllegalArgumentException.class, () -> com.quanlynhakhoa.nghiepvu.kho.thuoc.service.ThuocService.kiemTraVaChuanHoa(medicine));
        }
        var priced = new ThuocKeTinhTien("T1", "Thuốc mẫu", "Viên", 2, new BigDecimal("25000"));
        var unpriced = new ThuocKeTinhTien("T2", "Thuốc chưa tính tiền", "Hộp", 3, null);
        check(priced.total().compareTo(new BigDecimal("50000")) == 0, "medicine line amount");
        check(unpriced.total() == null, "unknown price is not silently treated as free");
        var invoice = new ThongTinHoaDon("H1", "NV1", "BN1", "Synthetic", "PK1", LocalDateTime.now(),
                TrangThaiHoaDon.CHUA_THANH_TOAN, new BigDecimal("350000"), BigDecimal.ZERO, new BigDecimal("350000"));
        var lines = List.of(new DongChiTietHoaDon(1,"H1","DV1",null,null,1,new BigDecimal("300000")),
                new DongChiTietHoaDon(2,"H1",null,"T1",null,2,new BigDecimal("25000")));
        var detail = new DuLieuChiTietHoaDon(invoice, lines, List.of(), List.of(), List.of(priced,unpriced));
        SwingUtilities.invokeAndWait(() -> {
            try {
                InvoicePanel panel = new InvoicePanel(session(VaiTroHeThong.RECEPTIONIST), new FeatureJdbcDouble());
                panel.showDetail(detail);
                var field = InvoicePanel.class.getDeclaredField("tblPrescribedMedicines");field.setAccessible(true);
                JTable table = (JTable)field.get(panel);
                check(table.getColumnCount() == 6 && table.getRowCount() == 2, "billing-only medicine table");
                check(table.getModel().getValueAt(0,5).equals(new BigDecimal("50000")), "priced amount displayed");
                check(table.getModel().getValueAt(1,4).equals("Chưa chốt") && table.getModel().getValueAt(1,5) == null,
                        "unpriced prescription distinguished from invoice charge");
                check(detail.hoaDon().tongTien().equals(new BigDecimal("350000")), "no duplicate charges");
                panel.clearDetail();check(table.getRowCount() == 0, "medicine rows cleared on invoice change");
            } catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
        });
        System.out.println("PASS: medicine quantities, locked prices, unknown price display and invoice totals.");
    }
}
