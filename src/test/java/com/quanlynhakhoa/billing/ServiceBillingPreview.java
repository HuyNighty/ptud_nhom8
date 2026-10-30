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
