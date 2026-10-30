package com.quanlynhakhoa.billing;

import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.billing.ui.HoaDonPanel;
import com.quanlynhakhoa.dichvu.model.DichVu;
import com.quanlynhakhoa.dichvu.ui.DichVuPanel;
import com.quanlynhakhoa.model.Role;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Synthetic, headless layout preview only. Never opens a database or authenticates a real user. */
public final class ServiceBillingPreview {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    FeatureJdbcDouble jdbc = new FeatureJdbcDouble();
                    DichVuPanel pnlCatalog = new DichVuPanel(ServiceBillingSelfTest.session(Role.CLINIC_MANAGER), jdbc);
                    DichVu value = new DichVu("TST001", "[KIỂM THỬ] Dịch vụ tổng hợp", new BigDecimal("123456"), "Dữ liệu tổng hợp chỉ dùng xem bố cục.", true, true);
                    pnlCatalog.showRows(List.of(value)); pnlCatalog.showForm(value);
                    pnlCatalog.status("XEM BỐ CỤC HEADLESS · dữ liệu tổng hợp · chưa kiểm thử SQL thật");
                    render(pnlCatalog, "target/service-catalog-preview.png");
                    HoaDonPanel pnlBilling = new HoaDonPanel(ServiceBillingSelfTest.session(Role.RECEPTIONIST), jdbc);
                    HoaDon invoice = new HoaDon("TSTHD001", "TSTNV", "TSTBN", "Bệnh nhân tổng hợp", "TSTPK",
                            LocalDateTime.of(2026, 10, 30, 9, 0), TrangThaiHoaDon.THANH_TOAN_MOT_PHAN,
                            new BigDecimal("300000"), new BigDecimal("100000"), new BigDecimal("200000"));
                    pnlBilling.showRows(List.of(invoice));
                    pnlBilling.showDetail(new ChiTietBilling(invoice, List.of(new ChiTietHoaDon(1, "TSTHD001", "TST001", null, null, 2, new BigDecimal("150000"))), List.of()));
                    pnlBilling.status("XEM BỐ CỤC HEADLESS · dữ liệu tổng hợp · chưa kiểm thử SQL thật");
                    render(pnlBilling, "target/billing-preview.png");
                    for(HoaDonPanel.ViewMode mode:HoaDonPanel.ViewMode.values()) {
                        HoaDonPanel pnlMode=new HoaDonPanel(ServiceBillingSelfTest.session(Role.RECEPTIONIST),jdbc,mode);
                        pnlMode.showRows(List.of(invoice));pnlMode.showDetail(new ChiTietBilling(invoice,List.of(),List.of()));
                        pnlMode.status("XEM BỐ CỤC · dữ liệu tổng hợp · không SQL");render(pnlMode,"target/"+mode.name().toLowerCase()+"-preview.png");
                    }
                    for(DichVuPanel.EditorMode mode:List.of(DichVuPanel.EditorMode.SERVICES,DichVuPanel.EditorMode.PRICES)) {
                        DichVuPanel pnlMode=new DichVuPanel(ServiceBillingSelfTest.session(Role.CLINIC_MANAGER),jdbc,mode);
                        pnlMode.showRows(List.of(value));pnlMode.showForm(value);render(pnlMode,"target/"+mode.name().toLowerCase()+"-preview.png");
                    }
                    com.quanlynhakhoa.billing.ui.CancellationPanel pnlCancellation=new com.quanlynhakhoa.billing.ui.CancellationPanel(ServiceBillingSelfTest.session(Role.CLINIC_MANAGER),jdbc);
                    YeuCauHuyHoaDon request=new YeuCauHuyHoaDon("TSTYC001",invoice.maHoaDon(),invoice.maBenhNhan(),invoice.tenBenhNhan(),invoice.tongTien(),invoice.daThu(),"TST_ACC",LocalDateTime.of(2026,10,30,10,0),"[KIỂM THỬ] Đề nghị kiểm tra chứng từ",YeuCauHuyHoaDon.TrangThai.CHO_DUYET,null,null,null);
                    pnlCancellation.showRows(List.of(request));
                    pnlCancellation.showDetail(new com.quanlynhakhoa.billing.service.CancellationService.Detail(request,new ChiTietBilling(invoice,List.of(),List.of())));
                    pnlCancellation.lblTrangThai.setText("XEM BỐ CỤC HEADLESS · dữ liệu tổng hợp · hóa đơn có thu tiền nên chặn duyệt hủy");render(pnlCancellation,"target/cancellation-preview.png");
                    if (jdbc.opens != 0) throw new AssertionError("Preview must not open JDBC");
                } catch (Exception failure) { throw new RuntimeException(failure); }
            }
        });
        System.out.println("PASS: synthetic layout previews rendered under target/ (no SQL).");
    }
    private static void render(JPanel pnlPanel, String file) throws Exception {
        pnlPanel.setSize(1360, 740); layout(pnlPanel);
        BufferedImage image = new BufferedImage(1360, 740, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics(); pnlPanel.printAll(graphics); graphics.dispose();
        ImageIO.write(image, "png", Path.of(file).toFile());
    }
    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) if (child instanceof Container nested) layout(nested);
    }
}
