package com.quanlynhakhoa.billing.ui;

import com.quanlynhakhoa.billing.model.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class InvoiceHistoryPanel extends JPanel {
    private final JLabel lblSummary=new JLabel("Chưa chọn hóa đơn.");
    private final JLabel lblTongTien=new JLabel("Tổng tiền: —"),lblDaThu=new JLabel("Đã thu: —"),lblConNo=new JLabel("Còn nợ: —");
    private final DefaultTableModel lines=BillingUi.model("Loại","Mã hàng","Tên dịch vụ / hàng","Số lượng","Đơn giá đã chốt","Thành tiền");
    private final DefaultTableModel payments=BillingUi.model("Mã thu","Thời gian","Số tiền","Phương thức","Tham chiếu","Trạng thái");
    private final JTable tblLines=BillingUi.table(lines),tblPayments=BillingUi.table(payments);
    public InvoiceHistoryPanel(){setLayout(new BorderLayout(8,8));JPanel pnlHeader=new JPanel(new BorderLayout(0,8));pnlHeader.add(lblSummary,BorderLayout.NORTH);pnlHeader.add(BillingUi.row(BillingUi.card(lblTongTien),BillingUi.card(lblDaThu),BillingUi.card(lblConNo)),BorderLayout.CENTER);add(pnlHeader,BorderLayout.NORTH);BillingUi.columns(tblLines,100,110,230,90,160,160);BillingUi.columns(tblPayments,115,180,160,150,180,135);BillingUi.tableState(tblLines,"Chọn hóa đơn để xem chi tiết.");BillingUi.tableState(tblPayments,"Chọn hóa đơn để xem lịch sử thanh toán.");JTabbedPane tabDetails=new JTabbedPane();tabDetails.addTab("Chi tiết hóa đơn",BillingUi.scroll(tblLines));tabDetails.addTab("Lịch sử giao dịch",BillingUi.scroll(tblPayments));add(tabDetails,BorderLayout.CENTER);}
    public void showInvoice(ChiTietBilling detail){
        clear();HoaDon h=detail.hoaDon();lblSummary.setText(h.maHoaDon()+" · "+h.maBenhNhan()+" · "+h.tenBenhNhan());
        lblTongTien.setText("Tổng: "+BillingUi.vnd(h.tongTien()));lblDaThu.setText("Đã thu: "+BillingUi.vnd(h.daThu()));lblConNo.setText("Còn nợ: "+BillingUi.vnd(h.conNo()));
        for(var line:detail.chiTiet())lines.addRow(new Object[]{line.loaiHang(),line.maHang(),line.tenHienThi(),line.soLuong(),line.donGiaApDung(),line.thanhTien()});
        for(var payment:detail.thanhToan())payments.addRow(new Object[]{payment.maThanhToan(),payment.thoiGian(),payment.soTien(),payment.phuongThuc(),payment.maGiaoDich(),payment.trangThai()});
        BillingUi.tableState(tblLines,"Hóa đơn chưa có dòng chi tiết.");BillingUi.tableState(tblPayments,"Chưa có khoản thanh toán.");
    }
    public void clear(){lblSummary.setText("Chưa chọn hóa đơn.");lblTongTien.setText("Tổng: —");lblDaThu.setText("Đã thu: —");lblConNo.setText("Còn nợ: —");lines.setRowCount(0);payments.setRowCount(0);BillingUi.tableState(tblLines,"Chọn hóa đơn để xem chi tiết.");BillingUi.tableState(tblPayments,"Chọn hóa đơn để xem lịch sử thanh toán.");}
}
