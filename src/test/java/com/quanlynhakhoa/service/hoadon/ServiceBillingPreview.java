package com.quanlynhakhoa.service.hoadon;

import com.quanlynhakhoa.enums.CheDoSuaDanhMucDichVu;
import com.quanlynhakhoa.enums.TrangThaiYeuCauHuyHoaDon;
import com.quanlynhakhoa.enums.TrangThaiHoaDon;
import com.quanlynhakhoa.enums.CheDoXemHoaDon;
import com.quanlynhakhoa.model.hoadon.ThongTinYeuCauHuyHoaDon;
import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import com.quanlynhakhoa.model.hoadon.DuLieuChiTietHoaDon;
import com.quanlynhakhoa.model.hoadon.DongChiTietHoaDon;
import com.quanlynhakhoa.view.hoadon.HoaDonPanel;
import com.quanlynhakhoa.entity.DichVuNhaKhoa;
import com.quanlynhakhoa.nghiepvu.dichvu.ui.ServiceCatalogPanel;
import com.quanlynhakhoa.enums.VaiTroHeThong;
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
                    ServiceCatalogPanel pnlCatalog = new ServiceCatalogPanel(ServiceBillingSelfTest.session(VaiTroHeThong.CLINIC_MANAGER), jdbc);
                    DichVuNhaKhoa value = new DichVuNhaKhoa("TST001", "[KIỂM THỬ] Dịch vụ tổng hợp", new BigDecimal("123456"), "Dữ liệu tổng hợp chỉ dùng xem bố cục.", true, true);
                    pnlCatalog.showRows(List.of(value)); pnlCatalog.showForm(value);
                    pnlCatalog.status("XEM BỐ CỤC HEADLESS · dữ liệu tổng hợp · chưa kiểm thử SQL thật");
                    render(pnlCatalog, "target/service-catalog-preview.png");
                    HoaDonPanel pnlBilling = new HoaDonPanel(ServiceBillingSelfTest.session(VaiTroHeThong.RECEPTIONIST), jdbc);
                    ThongTinHoaDon invoice = new ThongTinHoaDon("TSTHD001", "TSTNV", "TSTBN", "Bệnh nhân tổng hợp", "TSTPK",
                            LocalDateTime.of(2026, 10, 30, 9, 0), TrangThaiHoaDon.THANH_TOAN_MOT_PHAN,
                            new BigDecimal("300000"), new BigDecimal("100000"), new BigDecimal("200000"));
                    pnlBilling.showRows(List.of(invoice));
                    pnlBilling.showDetail(new DuLieuChiTietHoaDon(invoice, List.of(new DongChiTietHoaDon(1, "TSTHD001", "TST001", null, null, 2, new BigDecimal("150000"))), List.of()));
                    pnlBilling.status("XEM BỐ CỤC HEADLESS · dữ liệu tổng hợp · chưa kiểm thử SQL thật");
                    render(pnlBilling, "target/billing-preview.png");
                    com.quanlynhakhoa.view.hoadon.CongNoPanel pnlDebt=new com.quanlynhakhoa.view.hoadon.CongNoPanel(ServiceBillingSelfTest.session(VaiTroHeThong.RECEPTIONIST),jdbc);
                    pnlDebt.showPatients(com.quanlynhakhoa.service.hoadon.CongNoService.byPatient(List.of(invoice)));
                    pnlDebt.tblBenhNhan.setRowSelectionInterval(0,0);pnlDebt.showDetail(new DuLieuChiTietHoaDon(invoice,List.of(),List.of()));render(pnlDebt,"target/latest-debt-preview.png");
                    com.quanlynhakhoa.view.hoadon.ThanhToanPanel pnlPayment=new com.quanlynhakhoa.view.hoadon.ThanhToanPanel(ServiceBillingSelfTest.session(VaiTroHeThong.RECEPTIONIST),jdbc);
                    com.quanlynhakhoa.view.hoadon.ThanhToanForm pnlForm=new com.quanlynhakhoa.view.hoadon.ThanhToanForm();pnlForm.showInvoice(new DuLieuChiTietHoaDon(invoice,List.of(),List.of()));pnlForm.busy(false,true,false);pnlForm.txtSoTien.setText("200000");pnlForm.txtTienMat.setText("100000");pnlForm.txtChuyenKhoan.setText("100000");pnlForm.txtKhachDua.setText("100000");pnlPayment.showForm(pnlForm);render(pnlPayment,"target/latest-payment-preview.png");
                    for(CheDoXemHoaDon mode:CheDoXemHoaDon.values()) {
                        HoaDonPanel pnlMode=new HoaDonPanel(ServiceBillingSelfTest.session(VaiTroHeThong.RECEPTIONIST),jdbc,mode);
                        pnlMode.showRows(List.of(invoice));pnlMode.showDetail(new DuLieuChiTietHoaDon(invoice,List.of(),List.of()));
                        pnlMode.status("XEM BỐ CỤC · dữ liệu tổng hợp · không SQL");render(pnlMode,"target/"+mode.name().toLowerCase()+"-preview.png");
                    }
                    for(CheDoSuaDanhMucDichVu mode:List.of(CheDoSuaDanhMucDichVu.SERVICES,CheDoSuaDanhMucDichVu.PRICES)) {
                        ServiceCatalogPanel pnlMode=new ServiceCatalogPanel(ServiceBillingSelfTest.session(VaiTroHeThong.CLINIC_MANAGER),jdbc,mode);
                        pnlMode.showRows(List.of(value));pnlMode.showForm(value);render(pnlMode,"target/"+mode.name().toLowerCase()+"-preview.png");
                    }
                    com.quanlynhakhoa.view.hoadon.HuyHoaDonPanel pnlCancellation=new com.quanlynhakhoa.view.hoadon.HuyHoaDonPanel(ServiceBillingSelfTest.session(VaiTroHeThong.CLINIC_MANAGER),jdbc);
                    ThongTinYeuCauHuyHoaDon request=new ThongTinYeuCauHuyHoaDon("TSTYC001",invoice.maHoaDon(),invoice.maBenhNhan(),invoice.tenBenhNhan(),invoice.tongTien(),invoice.daThu(),"TST_ACC",LocalDateTime.of(2026,10,30,10,0),"[KIỂM THỬ] Đề nghị kiểm tra chứng từ",TrangThaiYeuCauHuyHoaDon.CHO_DUYET,null,null,null);
                    pnlCancellation.showRows(List.of(request));
                    pnlCancellation.showDetail(new com.quanlynhakhoa.model.hoadon.ChiTietYeuCauHuy(request,new DuLieuChiTietHoaDon(invoice,List.of(),List.of())));
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
