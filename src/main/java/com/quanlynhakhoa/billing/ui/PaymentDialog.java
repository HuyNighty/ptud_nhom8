package com.quanlynhakhoa.billing.ui;

import com.quanlynhakhoa.billing.model.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class PaymentDialog extends JDialog {
    public final JTextField txtSoTien=new JTextField(),txtTienMat=new JTextField("0"),txtChuyenKhoan=new JTextField("0"),txtKhachDua=new JTextField("0"),txtMaGiaoDich=new JTextField();
    public final JCheckBox chkDaNhanChuyenKhoan=new JCheckBox("Đã kiểm tra tài khoản ngân hàng và nhận đủ phần chuyển khoản");
    public final JButton btnThuTien=new JButton("Xác nhận thu"),btnThuHet=new JButton("Điền toàn bộ còn nợ"),btnKiemTra=new JButton("Kiểm tra kết quả lần thu");
    public final JLabel lblThongTin=new JLabel("Đang tải hóa đơn..."),lblTrangThai=new JLabel("Nhập VND nguyên, không dấu phân cách. Không tạo/xác nhận QR tự động.");
    private final DefaultTableModel lineModel=BillingUi.model("Loại","Mã hàng","Số lượng","Giá chốt","Thành tiền");
    private final DefaultTableModel paymentModel=BillingUi.model("Mã thu","Thời gian","Số tiền","Phương thức","Tham chiếu","Trạng thái");
    public PaymentDialog(Window owner) {
        super(owner,"Thanh toán hóa đơn",ModalityType.APPLICATION_MODAL);
        JPanel pnlRoot=new JPanel(new BorderLayout(8,8));pnlRoot.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));pnlRoot.add(lblThongTin,BorderLayout.NORTH);
        JTabbedPane tabPayment=new JTabbedPane();tabPayment.addTab("Chi tiết hóa đơn",BillingUi.scroll(BillingUi.table(lineModel)));tabPayment.addTab("Lịch sử thu",BillingUi.scroll(BillingUi.table(paymentModel)));
        pnlRoot.add(tabPayment,BorderLayout.CENTER);
        JPanel pnlFields=new JPanel(new GridLayout(0,2,8,6));
        pnlFields.add(new JLabel("Số tiền thu"));pnlFields.add(txtSoTien);pnlFields.add(new JLabel("Phần tiền mặt"));pnlFields.add(txtTienMat);
        pnlFields.add(new JLabel("Phần chuyển khoản"));pnlFields.add(txtChuyenKhoan);pnlFields.add(new JLabel("Tiền khách đưa cho phần tiền mặt"));pnlFields.add(txtKhachDua);
        pnlFields.add(new JLabel("Mã giao dịch chuyển khoản"));pnlFields.add(txtMaGiaoDich);pnlFields.add(chkDaNhanChuyenKhoan);pnlFields.add(btnThuHet);
        pnlFields.add(btnKiemTra);pnlFields.add(btnThuTien);
        JPanel pnlBottom=new JPanel(new BorderLayout(0,8));pnlBottom.add(pnlFields,BorderLayout.CENTER);pnlBottom.add(lblTrangThai,BorderLayout.SOUTH);pnlRoot.add(pnlBottom,BorderLayout.SOUTH);
        setContentPane(pnlRoot);setSize(1080,640);setLocationRelativeTo(owner);setDefaultCloseOperation(DISPOSE_ON_CLOSE);btnKiemTra.setEnabled(false);
    }
    public void showInvoice(ChiTietBilling detail) {
        HoaDon h=detail.hoaDon();lblThongTin.setText(h.maHoaDon()+" · "+h.maBenhNhan()+" · "+h.tenBenhNhan()+" · Tổng "+BillingUi.vnd(h.tongTien())+" · Đã thu "+BillingUi.vnd(h.daThu())+" · Còn nợ "+BillingUi.vnd(h.conNo()));
        lineModel.setRowCount(0);paymentModel.setRowCount(0);
        for(ChiTietHoaDon line:detail.chiTiet())lineModel.addRow(new Object[]{line.loaiHang(),line.maHang(),line.soLuong(),line.donGiaApDung(),line.thanhTien()});
        for(ThanhToan payment:detail.thanhToan())paymentModel.addRow(new Object[]{payment.maThanhToan(),payment.thoiGian(),payment.soTien(),payment.phuongThuc(),payment.maGiaoDich(),payment.trangThai()});
    }
    public void busy(boolean busy,boolean mayCollect,boolean uncertain) {
        for(Component control:new Component[]{txtSoTien,txtTienMat,txtChuyenKhoan,txtKhachDua,txtMaGiaoDich,chkDaNhanChuyenKhoan,btnThuHet})control.setEnabled(!busy&&!uncertain&&mayCollect);
        btnThuTien.setEnabled(!busy&&!uncertain&&mayCollect);btnKiemTra.setEnabled(!busy&&uncertain);setDefaultCloseOperation(busy?DO_NOTHING_ON_CLOSE:DISPOSE_ON_CLOSE);
    }
}
