package com.quanlynhakhoa.billing.ui;

import com.quanlynhakhoa.billing.model.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class InvoiceHistoryPanel extends JPanel {
    private final JLabel lblSummary=new JLabel("Chưa chọn hóa đơn.");
    private final DefaultTableModel lines=BillingUi.model("Loại","Mã hàng","Số lượng","Giá chốt","Thành tiền");
    private final DefaultTableModel payments=BillingUi.model("Mã thu","Thời gian","Số tiền","Phương thức","Tham chiếu","Trạng thái");
    public InvoiceHistoryPanel(){setLayout(new BorderLayout(8,8));add(lblSummary,BorderLayout.NORTH);JTabbedPane tabDetails=new JTabbedPane();tabDetails.addTab("Chi tiết hóa đơn",BillingUi.scroll(BillingUi.table(lines)));tabDetails.addTab("Lịch sử giao dịch",BillingUi.scroll(BillingUi.table(payments)));add(tabDetails,BorderLayout.CENTER);}
    public void showInvoice(ChiTietBilling detail){
        clear();HoaDon h=detail.hoaDon();lblSummary.setText(h.maHoaDon()+" · "+h.maBenhNhan()+" · "+h.tenBenhNhan()+" · Tổng "+BillingUi.vnd(h.tongTien())+" · Đã thu "+BillingUi.vnd(h.daThu())+" · Còn nợ "+BillingUi.vnd(h.conNo()));
        for(var line:detail.chiTiet())lines.addRow(new Object[]{line.loaiHang(),line.maHang(),line.soLuong(),line.donGiaApDung(),line.thanhTien()});
        for(var payment:detail.thanhToan())payments.addRow(new Object[]{payment.maThanhToan(),payment.thoiGian(),payment.soTien(),payment.phuongThuc(),payment.maGiaoDich(),payment.trangThai()});
    }
    public void clear(){lblSummary.setText("Chưa chọn hóa đơn.");lines.setRowCount(0);payments.setRowCount(0);}
}
