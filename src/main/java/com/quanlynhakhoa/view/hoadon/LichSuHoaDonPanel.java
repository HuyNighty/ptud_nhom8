package com.quanlynhakhoa.view.hoadon;

import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import com.quanlynhakhoa.model.hoadon.DuLieuChiTietHoaDon;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class LichSuHoaDonPanel extends JPanel {
    private final JLabel lblSummary=new JLabel("Chưa chọn hóa đơn.");
    private final JLabel lblTongTien=new JLabel("Tổng tiền: —"),lblDaThu=new JLabel("Đã thu: —"),lblConNo=new JLabel("Còn nợ: —");
    private final DefaultTableModel lines=GiaoDienHoaDon.model("Loại","Mã hàng","Tên dịch vụ / hàng","Số lượng","Đơn giá đã chốt","Thành tiền");
    private final DefaultTableModel payments=GiaoDienHoaDon.model("Mã thu","Thời gian","Số tiền","Phương thức","Tham chiếu","Trạng thái");
    private final JTable tblLines=GiaoDienHoaDon.table(lines),tblPayments=GiaoDienHoaDon.table(payments);
    public LichSuHoaDonPanel(){setLayout(new BorderLayout(8,8));JPanel pnlHeader=new JPanel(new BorderLayout(0,8));pnlHeader.add(lblSummary,BorderLayout.NORTH);pnlHeader.add(GiaoDienHoaDon.row(GiaoDienHoaDon.card(lblTongTien),GiaoDienHoaDon.card(lblDaThu),GiaoDienHoaDon.card(lblConNo)),BorderLayout.CENTER);add(pnlHeader,BorderLayout.NORTH);GiaoDienHoaDon.columns(tblLines,100,110,230,90,160,160);GiaoDienHoaDon.columns(tblPayments,115,180,160,150,180,135);GiaoDienHoaDon.tableState(tblLines,"Chọn hóa đơn để xem chi tiết.");GiaoDienHoaDon.tableState(tblPayments,"Chọn hóa đơn để xem lịch sử thanh toán.");JTabbedPane tabDetails=new JTabbedPane();tabDetails.addTab("Chi tiết hóa đơn",GiaoDienHoaDon.scroll(tblLines));tabDetails.addTab("Lịch sử giao dịch",GiaoDienHoaDon.scroll(tblPayments));add(tabDetails,BorderLayout.CENTER);}
    public void showInvoice(DuLieuChiTietHoaDon detail){
        clear();ThongTinHoaDon h=detail.hoaDon();lblSummary.setText(h.maHoaDon()+" · "+h.maBenhNhan()+" · "+h.tenBenhNhan());
        lblTongTien.setText("Tổng: "+GiaoDienHoaDon.vnd(h.tongTien()));lblDaThu.setText("Đã thu: "+GiaoDienHoaDon.vnd(h.daThu()));lblConNo.setText("Còn nợ: "+GiaoDienHoaDon.vnd(h.conNo()));
        for(var line:detail.chiTiet())lines.addRow(new Object[]{line.loaiHang(),line.maHang(),line.tenHienThi(),line.soLuong(),line.donGiaApDung(),line.thanhTien()});
        for(var payment:detail.thanhToan())payments.addRow(new Object[]{payment.maThanhToan(),payment.thoiGian(),payment.soTien(),payment.phuongThuc(),payment.maGiaoDich(),payment.trangThai()});
        GiaoDienHoaDon.tableState(tblLines,"Hóa đơn chưa có dòng chi tiết.");GiaoDienHoaDon.tableState(tblPayments,"Chưa có khoản thanh toán.");
    }
    public void clear(){lblSummary.setText("Chưa chọn hóa đơn.");lblTongTien.setText("Tổng: —");lblDaThu.setText("Đã thu: —");lblConNo.setText("Còn nợ: —");lines.setRowCount(0);payments.setRowCount(0);GiaoDienHoaDon.tableState(tblLines,"Chọn hóa đơn để xem chi tiết.");GiaoDienHoaDon.tableState(tblPayments,"Chọn hóa đơn để xem lịch sử thanh toán.");}
}
