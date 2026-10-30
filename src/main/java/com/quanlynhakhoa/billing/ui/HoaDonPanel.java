package com.quanlynhakhoa.billing.ui;

import com.quanlynhakhoa.billing.controller.HoaDonController;
import com.quanlynhakhoa.billing.dao.ConnectionProvider;
import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.billing.service.HoaDonService;
import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.security.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

public final class HoaDonPanel extends JPanel {
    private final JTextField txtTuKhoa = new JTextField(15), txtTuNgay = new JTextField(10), txtDenNgay = new JTextField(10);
    private final JComboBox<Object> cmbTrangThai = new JComboBox<>();
    private final JButton btnTim = new JButton("Tìm / Tải lại");
    private final JLabel lblTrangThai = new JLabel("Chọn hóa đơn để xem chi tiết."), lblTongTien = new JLabel("Tổng: —");
    private final JLabel lblDaThu = new JLabel("Đã thu: —"), lblConNo = new JLabel("Còn nợ: —"), lblCongNoBenhNhan = new JLabel("Công nợ BN: —");
    private final JLabel lblCanhBao = new JLabel(" ");
    private final DefaultTableModel invoiceModel = BillingUi.model("Mã hóa đơn", "Mã BN", "Bệnh nhân", "Ngày lập", "Trạng thái", "Tổng tiền", "Đã thu", "Còn nợ", "Đối soát");
    private final DefaultTableModel lineModel = BillingUi.model("Mã dòng", "Loại", "Mã hàng", "Số lượng", "Giá chốt", "Thành tiền");
    private final DefaultTableModel paymentModel = BillingUi.model("Mã thu", "Thời gian", "Số tiền", "Phương thức", "Mã giao dịch", "Khách đưa", "Trạng thái", "Nhân viên", "TK hủy", "Lúc hủy", "Lý do hủy");
    private final JTable tblHoaDon = BillingUi.table(invoiceModel), tblChiTietHoaDon = BillingUi.table(lineModel), tblThanhToan = BillingUi.table(paymentModel);
    private final HoaDonController controller;
    private List<HoaDon> rows = List.of();
    private boolean loaded;

    public HoaDonPanel(DesktopSession session) { this(session, TeamDevConnection::open); }
    public HoaDonPanel(DesktopSession session, ConnectionProvider connections) {
        new AuthorizationService(new RolePermissionPolicy()).require(session, Permission.BILLING_READ);
        setLayout(new BorderLayout(10, 10)); setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel pnlTop = new JPanel(new BorderLayout(0, 8));
        JLabel lblTitle = new JLabel("HÓA ĐƠN & CÔNG NỢ · M1 CHỈ ĐỌC");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 22f)); pnlTop.add(lblTitle, BorderLayout.NORTH);
        cmbTrangThai.addItem("Tất cả");
        for (TrangThaiHoaDon value : TrangThaiHoaDon.values()) cmbTrangThai.addItem(value);
        tblThanhToan.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int column = 0; column < tblThanhToan.getColumnCount(); column++) {
            tblThanhToan.getColumnModel().getColumn(column).setPreferredWidth(column == 10 ? 260 : 150);
        }
        JPanel pnlFilter = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlFilter.add(new JLabel("Mã HĐ / mã, tên BN")); pnlFilter.add(txtTuKhoa);
        pnlFilter.add(new JLabel("Từ (yyyy-MM-dd)")); pnlFilter.add(txtTuNgay);
        pnlFilter.add(new JLabel("Đến")); pnlFilter.add(txtDenNgay); pnlFilter.add(cmbTrangThai); pnlFilter.add(btnTim);
        pnlTop.add(pnlFilter, BorderLayout.SOUTH); add(pnlTop, BorderLayout.NORTH);
        JScrollPane scrHoaDon = BillingUi.scroll(tblHoaDon); scrHoaDon.setPreferredSize(new Dimension(1050, 240));
        JScrollPane scrChiTiet = BillingUi.scroll(tblChiTietHoaDon), scrThanhToan = BillingUi.scroll(tblThanhToan);
        JTabbedPane tabBilling = new JTabbedPane(); tabBilling.addTab("Chi tiết · giá tại thời điểm lập", scrChiTiet); tabBilling.addTab("Lịch sử thanh toán", scrThanhToan);
        tabBilling.setPreferredSize(new Dimension(1050, 200));
        JPanel pnlBody = new JPanel(new BorderLayout(0, 8)); pnlBody.add(scrHoaDon, BorderLayout.NORTH); pnlBody.add(tabBilling, BorderLayout.CENTER);
        add(pnlBody, BorderLayout.CENTER);
        JPanel pnlBottom = new JPanel(new GridLayout(0, 1, 0, 6));
        JPanel pnlTotals = new JPanel(new GridLayout(1, 4, 12, 0));
        pnlTotals.add(lblTongTien); pnlTotals.add(lblDaThu); pnlTotals.add(lblConNo); pnlTotals.add(lblCongNoBenhNhan);
        pnlBottom.add(pnlTotals); pnlBottom.add(lblCanhBao);
        pnlBottom.add(new JLabel("Công nợ BN tính trên mọi hóa đơn chưa hủy, không phụ thuộc bộ lọc; giữ số âm để đối soát."));
        pnlBottom.add(new JLabel("M2: lập hóa đơn, thu tiền và quy trình hủy chờ hợp đồng dữ liệu khám và quyền SQL được duyệt."));
        pnlBottom.add(lblTrangThai); add(pnlBottom, BorderLayout.SOUTH);
        controller = new HoaDonController(this, session, new HoaDonService(session, connections));
    }
    @Override public void addNotify() {
        super.addNotify();
        if (!loaded) { loaded = true; SwingUtilities.invokeLater(new Runnable() { public void run() { controller.load(); } }); }
    }
    public void onSearch(ActionListener listener) { btnTim.addActionListener(listener); txtTuKhoa.addActionListener(listener); txtTuNgay.addActionListener(listener); txtDenNgay.addActionListener(listener); cmbTrangThai.addActionListener(listener); }
    public void onSelect(ListSelectionListener listener) { tblHoaDon.getSelectionModel().addListSelectionListener(listener); }
    public String keyword() { return txtTuKhoa.getText(); }
    public String fromText() { return txtTuNgay.getText(); }
    public String toText() { return txtDenNgay.getText(); }
    public TrangThaiHoaDon statusFilter() { return cmbTrangThai.getSelectedItem() instanceof TrangThaiHoaDon status ? status : null; }
    public String selectedId() {
        int index = tblHoaDon.getSelectedRow();
        if (index < 0) return null;
        index = tblHoaDon.convertRowIndexToModel(index);
        return index >= 0 && index < rows.size() ? rows.get(index).maHoaDon() : null;
    }
    public void showRows(List<HoaDon> values) {
        rows = List.of(); invoiceModel.setRowCount(0); rows = List.copyOf(values);
        for (HoaDon value : rows) invoiceModel.addRow(new Object[]{value.maHoaDon(), value.maBenhNhan(), value.tenBenhNhan(),
                value.ngayLap(), value.trangThai(), value.tongTien(), value.daThu(), value.conNo(), value.canhBao()});
        clearDetail();
    }
    public void clearDetail() {
        lineModel.setRowCount(0); paymentModel.setRowCount(0);
        lblTongTien.setText("Tổng: —"); lblDaThu.setText("Đã thu: —"); lblConNo.setText("Còn nợ: —"); lblCongNoBenhNhan.setText("Công nợ BN: —"); lblCanhBao.setText(" ");
    }
    public void showDetail(ChiTietBilling value) {
        clearDetail();
        for (ChiTietHoaDon line : value.chiTiet()) lineModel.addRow(new Object[]{line.maChiTiet(), line.loaiHang(), line.maHang(), line.soLuong(), line.donGiaApDung(), line.thanhTien()});
        for (ThanhToan payment : value.thanhToan()) paymentModel.addRow(new Object[]{payment.maThanhToan(), payment.thoiGian(),
                payment.soTien(), payment.phuongThuc(), payment.maGiaoDich(), payment.tienKhachDua(), payment.trangThai(),
                payment.maNhanVien(), payment.maTaiKhoanHuy(), BillingUi.time(payment.thoiGianHuy()), payment.lyDoHuy()});
        HoaDon invoice = value.hoaDon();
        lblTongTien.setText("Tổng: " + BillingUi.vnd(invoice.tongTien())); lblDaThu.setText("Đã thu: " + BillingUi.vnd(invoice.daThu()));
        lblConNo.setText("Còn nợ: " + BillingUi.vnd(invoice.conNo())); lblCongNoBenhNhan.setText("Công nợ BN: " + BillingUi.vnd(invoice.congNoBenhNhan()));
        lblCanhBao.setText(invoice.canhBao());
    }
    public void setBusy(boolean busy) {
        for (Component control : new Component[]{txtTuKhoa, txtTuNgay, txtDenNgay, cmbTrangThai, btnTim, tblHoaDon}) control.setEnabled(!busy);
    }
    public void status(String text) { lblTrangThai.setText(text); }
    public void expired() { showRows(List.of()); setBusy(true); status("Phiên đã đăng xuất."); }
}
