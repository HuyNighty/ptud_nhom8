package com.quanlynhakhoa.billing.ui;

import com.quanlynhakhoa.billing.controller.HoaDonController;
import com.quanlynhakhoa.billing.dao.ConnectionProvider;
import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.billing.service.HoaDonService;
import com.quanlynhakhoa.billing.service.ClinicalBillingSource;
import com.quanlynhakhoa.billing.service.InvoiceIdGenerator;
import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.security.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

public final class HoaDonPanel extends JPanel implements com.quanlynhakhoa.features.RefreshableFeature {
    public enum ViewMode { INVOICES, DEBTS, PAYMENTS }
    public interface PaymentNavigation { void open(String invoiceId); }
    private final ViewMode mode;
    private final DesktopSession session;
    private final ConnectionProvider connections;
    private PaymentNavigation paymentNavigation;
    private boolean canPay;
    private final JButton btnThanhToan = new JButton("Thanh toán");
    private final JButton btnYeuCauHuy = new JButton("Yêu cầu hủy");
    private final boolean canRequestCancellation;
    private final JLabel lblTongCongNo = new JLabel("Tổng công nợ theo bộ lọc: —");
    private final JTextField txtTuKhoa = new JTextField(15), txtTuNgay = new JTextField(10), txtDenNgay = new JTextField(10);
    private final JComboBox<Object> cmbTrangThai = new JComboBox<>();
    private final JButton btnTim = new JButton("Tìm"), btnTaiLai = new JButton("Tải lại");
    private final JButton btnTaoHoaDon = new JButton("Tạo hóa đơn");
    private final boolean canCreate;
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
        this(session, connections, ViewMode.INVOICES);
    }
    public HoaDonPanel(DesktopSession session, ConnectionProvider connections, ViewMode mode) {
        this(session, connections, new com.quanlynhakhoa.billing.service.JdbcClinicalBillingSource(),
                new com.quanlynhakhoa.billing.service.RandomInvoiceIdGenerator(), mode);
    }
    public HoaDonPanel(DesktopSession session, ConnectionProvider connections, ClinicalBillingSource source, InvoiceIdGenerator ids) {
        this(session,connections,source,ids,ViewMode.INVOICES);
    }
    private HoaDonPanel(DesktopSession session, ConnectionProvider connections, ClinicalBillingSource source, InvoiceIdGenerator ids, ViewMode mode) {
        this.session=session;this.connections=connections;this.mode=mode;
        new AuthorizationService(new RolePermissionPolicy()).require(session, Permission.BILLING_READ);
        canPay = new AuthorizationService(new RolePermissionPolicy()).can(session, Permission.BILLING_MANAGE);
        canRequestCancellation=new AuthorizationService(new RolePermissionPolicy()).can(session,Permission.INVOICE_CANCEL_REQUEST);
        btnYeuCauHuy.setVisible(mode==ViewMode.INVOICES&&canRequestCancellation);
        btnYeuCauHuy.setEnabled(canRequestCancellation);
        canCreate = new AuthorizationService(new RolePermissionPolicy()).can(session, Permission.BILLING_MANAGE)
                && source != null && ids != null;
        btnTaoHoaDon.setEnabled(canCreate);
        btnTaoHoaDon.setVisible(mode==ViewMode.INVOICES);
        btnThanhToan.setText(mode==ViewMode.DEBTS?"Thu nợ":"Thanh toán");btnThanhToan.setEnabled(canPay);
        btnTaoHoaDon.setToolTipText(canCreate ? "Lập từ dịch vụ khám được phép tính phí" : "Chờ quyền BILLING_MANAGE, hợp đồng tính phí và bộ sinh mã được duyệt");
        setLayout(new BorderLayout(10, 10)); setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel pnlTop = new JPanel(new BorderLayout(0, 8));
        JLabel lblTitle = new JLabel(switch(mode){case INVOICES -> "HÓA ĐƠN";case DEBTS -> "CÔNG NỢ";case PAYMENTS -> "THANH TOÁN";});
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
        pnlFilter.add(new JLabel("Đến")); pnlFilter.add(txtDenNgay); pnlFilter.add(cmbTrangThai); pnlFilter.add(btnTim); pnlFilter.add(btnTaiLai);
        JPanel pnlActions=new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlActions.add(btnTaoHoaDon);pnlActions.add(btnThanhToan);pnlActions.add(btnYeuCauHuy);
        JPanel pnlToolbar=new JPanel(new BorderLayout());pnlToolbar.add(pnlFilter,BorderLayout.NORTH);pnlToolbar.add(pnlActions,BorderLayout.SOUTH);
        pnlTop.add(pnlToolbar, BorderLayout.SOUTH); add(pnlTop, BorderLayout.NORTH);
        JScrollPane scrHoaDon = BillingUi.scroll(tblHoaDon); scrHoaDon.setPreferredSize(new Dimension(1050, 240));
        JScrollPane scrChiTiet = BillingUi.scroll(tblChiTietHoaDon), scrThanhToan = BillingUi.scroll(tblThanhToan);
        JTabbedPane tabBilling = new JTabbedPane(); tabBilling.addTab("Chi tiết · giá tại thời điểm lập", scrChiTiet); tabBilling.addTab("Lịch sử thanh toán", scrThanhToan);
        tabBilling.setPreferredSize(new Dimension(1050, 200));
        JPanel pnlBody = new JPanel(new BorderLayout(0, 8)); pnlBody.add(scrHoaDon, BorderLayout.NORTH); pnlBody.add(tabBilling, BorderLayout.CENTER);
        add(pnlBody, BorderLayout.CENTER);
        JPanel pnlBottom = new JPanel(new GridLayout(0, 1, 0, 6));
        JPanel pnlTotals = new JPanel(new GridLayout(1, 4, 12, 0));
        pnlTotals.add(lblTongTien); pnlTotals.add(lblDaThu); pnlTotals.add(lblConNo);
        if(mode==ViewMode.DEBTS)pnlTotals.add(lblTongCongNo);
        pnlBottom.add(pnlTotals); pnlBottom.add(lblCanhBao);
        if(mode==ViewMode.DEBTS)pnlBottom.add(new JLabel("Chỉ hiện hóa đơn chưa hủy, còn phải thu > 0; tổng tính trên kết quả lọc hiện tại."));
        pnlBottom.add(new JLabel("Thu tiền mặt/chuyển khoản/phối hợp; kiểm tra đã nhận tiền thực tế trước khi xác nhận."));
        pnlBottom.add(lblTrangThai); add(pnlBottom, BorderLayout.SOUTH);
        controller = new HoaDonController(this, session, source == null || ids == null
                ? new HoaDonService(session, connections) : new HoaDonService(session, connections, source, ids));
    }
    @Override public void addNotify() {
        super.addNotify();
        if (!loaded) { loaded = true; SwingUtilities.invokeLater(new Runnable() { public void run() { controller.load(); } }); }
    }
    public void onSearch(ActionListener listener) { btnTim.addActionListener(listener); txtTuKhoa.addActionListener(listener); txtTuNgay.addActionListener(listener); txtDenNgay.addActionListener(listener); cmbTrangThai.addActionListener(listener); }
    public void onReload(ActionListener listener) { btnTaiLai.addActionListener(listener); }
    public void onCreate(ActionListener listener) { btnTaoHoaDon.addActionListener(listener); }
    public void onPay(ActionListener listener) { btnThanhToan.addActionListener(listener); }
    public void onRequestCancellation(ActionListener listener){btnYeuCauHuy.addActionListener(listener);}
    public void requestCancellation(){
        if(!new AuthorizationService(new RolePermissionPolicy()).can(session,Permission.INVOICE_CANCEL_REQUEST)){status("Không có quyền gửi yêu cầu hủy.");return;}
        String id=selectedId();if(id==null){status("Chọn một hóa đơn để gửi yêu cầu hủy.");return;}
        CancellationRequestDialog winRequest=new CancellationRequestDialog(SwingUtilities.getWindowAncestor(this));
        new com.quanlynhakhoa.billing.controller.CancellationRequestController(winRequest,session,new com.quanlynhakhoa.billing.service.CancellationService(session,connections),id,new Runnable(){public void run(){status("Yêu cầu đã gửi; hóa đơn chưa bị hủy, chờ xét duyệt.");controller.load();}});
        winRequest.setVisible(true);
    }
    public boolean outstandingOnly(){return mode!=ViewMode.INVOICES;}
    public void setPaymentNavigation(PaymentNavigation navigation){paymentNavigation=navigation;}
    public void paySelected(){
        String id=selectedId();if(id==null){status("Chọn một hóa đơn để thanh toán.");return;}
        if(!new AuthorizationService(new RolePermissionPolicy()).can(session,Permission.BILLING_MANAGE)){status("Không có quyền thu tiền.");return;}
        if(paymentNavigation!=null&&mode!=ViewMode.PAYMENTS)paymentNavigation.open(id);else payInvoice(id);
    }
    public void payInvoice(String id){
        if(!new AuthorizationService(new RolePermissionPolicy()).can(session,Permission.BILLING_MANAGE)){status("Không có quyền thu tiền.");return;}
        if(mode==ViewMode.PAYMENTS)controller.focusInvoice(id);
        PaymentDialog winPayment=new PaymentDialog(SwingUtilities.getWindowAncestor(this));
        new com.quanlynhakhoa.billing.controller.PaymentController(winPayment,session,connections,id,new Runnable(){public void run(){controller.paymentSaved();}});
        winPayment.setVisible(true);
    }
    @Override public void refreshData(){controller.load();}
    public void filterInvoice(String id) { txtTuKhoa.setText(id); txtTuNgay.setText(""); txtDenNgay.setText(""); cmbTrangThai.setSelectedIndex(0); }
    public void selectInvoice(String id) {
        for (int i=0;i<rows.size();i++) if(rows.get(i).maHoaDon().equals(id)) {
            int index=tblHoaDon.convertRowIndexToView(i);tblHoaDon.setRowSelectionInterval(index,index);return;
        }
    }
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
        java.math.BigDecimal debt=java.math.BigDecimal.ZERO;
        for(HoaDon h:values)debt=debt.add(h.noTheoDoi());
        lblTongCongNo.setText("Tổng công nợ theo bộ lọc: "+BillingUi.vnd(debt));
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
        for (Component control : new Component[]{txtTuKhoa, txtTuNgay, txtDenNgay, cmbTrangThai, btnTim, btnTaiLai, tblHoaDon}) control.setEnabled(!busy);
        btnTaoHoaDon.setEnabled(!busy && canCreate);
        btnThanhToan.setEnabled(!busy && canPay);
        btnYeuCauHuy.setEnabled(!busy&&canRequestCancellation);
    }
    public void status(String text) { lblTrangThai.setText(text); }
    public void expired() { showRows(List.of()); setBusy(true); status("Phiên đã đăng xuất."); }
}
