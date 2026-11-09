package com.quanlynhakhoa.nghiepvu.hoadon.ui;

import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.enums.TrangThaiThanhToan;
import com.quanlynhakhoa.enums.TrangThaiHoaDon;
import com.quanlynhakhoa.enums.CheDoXemHoaDon;
import com.quanlynhakhoa.model.hoadon.ThongTinYeuCauHuyHoaDon;
import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import com.quanlynhakhoa.model.hoadon.DuLieuChiTietHoaDon;
import com.quanlynhakhoa.model.hoadon.ThongTinThanhToan;
import com.quanlynhakhoa.model.hoadon.ThuocKeTinhTien;
import com.quanlynhakhoa.model.hoadon.LuaChonLapHoaDon;
import com.quanlynhakhoa.model.hoadon.DongChiTietHoaDon;
import com.quanlynhakhoa.nghiepvu.hoadon.controller.InvoiceController;
import com.quanlynhakhoa.nghiepvu.hoadon.dao.ConnectionProvider;
import com.quanlynhakhoa.nghiepvu.hoadon.service.InvoiceService;
import com.quanlynhakhoa.nghiepvu.hoadon.service.ClinicalBillingSource;
import com.quanlynhakhoa.nghiepvu.hoadon.service.InvoiceIdGenerator;
import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import com.quanlynhakhoa.hethong.phanquyen.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

public final class InvoicePanel extends JPanel implements com.quanlynhakhoa.hethong.tinhnang.RefreshableFeature {

    public interface PaymentNavigation { void open(String invoiceId); }
    private final CheDoXemHoaDon mode;
    private final DesktopSession session;
    private final ConnectionProvider connections;
    private PaymentNavigation paymentNavigation;
    private boolean canPay;
    public final JButton btnTrangTruoc=new JButton("‹ Trang trước"),btnTrangSau=new JButton("Trang sau ›");
    private final JLabel lblTrang=new JLabel("Trang 1");
    public void pageState(int page,boolean next){lblTrang.setText("Trang "+(page+1));btnTrangTruoc.setEnabled(page>0);btnTrangSau.setEnabled(next);}
    public final JButton btnDieuChinh=new JButton("Điều chỉnh khoản thu");
    private final JButton btnThanhToan = new JButton("Thanh toán");
    private final JButton btnYeuCauHuy = new JButton("Yêu cầu hủy");
    private final boolean canRequestCancellation;
    private final JLabel lblTongCongNo = new JLabel("Tổng công nợ: —");
    private final JTextField txtTuKhoa = new JTextField(15), txtTuNgay = new JTextField(10), txtDenNgay = new JTextField(10);
    private final JComboBox<Object> cmbTrangThai = new JComboBox<>();
    private final JButton btnTim = new JButton("Tìm"), btnTaiLai = new JButton("Làm mới");
    private final JButton btnTaoHoaDon = new JButton("Tạo hóa đơn");
    private final boolean canCreate;
    private final JLabel lblTrangThai = new JLabel("Chọn hóa đơn để xem chi tiết."), lblTongTien = new JLabel("Tổng: —");
    private final JLabel lblDaThu = new JLabel("Đã thu: —"), lblConNo = new JLabel("Còn phải thu: —"), lblCongNoBenhNhan = new JLabel("Công nợ BN: —");
    private final JLabel lblCanhBao = new JLabel(" ");
    private final JLabel lblCanhBaoDanhSach=new JLabel(" ");
    private final DefaultTableModel invoiceModel = BillingUi.model("Mã hóa đơn", "Mã BN", "Bệnh nhân", "Ngày lập", "Trạng thái", "Tổng tiền", "Đã thu", "Còn phải thu", "Đối soát", "Yêu cầu hủy");
    private final DefaultTableModel lineModel = BillingUi.model("Loại", "Mã hàng", "Tên dịch vụ / hàng", "Số lượng", "Đơn giá đã chốt", "Thành tiền");
    private final DefaultTableModel paymentModel = BillingUi.model("Mã thu", "Thời gian", "Số tiền", "Phương thức", "Mã giao dịch", "Khách đưa", "Trạng thái", "Nhân viên", "TK hủy khoản thu", "Thời gian hủy khoản thu", "Lý do hủy khoản thu");
    private final DefaultTableModel cancellationModel = BillingUi.model("Mã yêu cầu", "Người gửi", "Thời gian gửi",
            "Lý do yêu cầu", "Kết quả", "Người duyệt", "Thời gian duyệt", "Ý kiến xét duyệt");
    private final JTable tblCancellationHistory = BillingUi.table(cancellationModel);
    private final DefaultTableModel prescriptionModel = BillingUi.model("Mã thuốc", "Tên thuốc", "Đơn vị",
            "Số lượng", "Đơn giá đã chốt", "Thành tiền");
    private final JTable tblPrescribedMedicines = BillingUi.table(prescriptionModel);
    private final JTable tblHoaDon = BillingUi.table(invoiceModel), tblChiTietHoaDon = BillingUi.table(lineModel), tblThanhToan = BillingUi.table(paymentModel);
    private final InvoiceController controller;
    private List<ThongTinHoaDon> rows = List.of();
    private boolean loaded;
    private boolean working;
    private ThongTinHoaDon selectedDetail;

    public InvoicePanel(DesktopSession session) { this(session, TeamDevConnection::open); }
    public InvoicePanel(DesktopSession session, ConnectionProvider connections) {
        this(session, connections, CheDoXemHoaDon.INVOICES);
    }
    public InvoicePanel(DesktopSession session, ConnectionProvider connections, CheDoXemHoaDon mode) {
        this(session, connections, new com.quanlynhakhoa.nghiepvu.hoadon.service.JdbcClinicalBillingSource(),
                new com.quanlynhakhoa.nghiepvu.hoadon.service.RandomInvoiceIdGenerator(), mode);
    }
    public InvoicePanel(DesktopSession session, ConnectionProvider connections, ClinicalBillingSource source, InvoiceIdGenerator ids) {
        this(session,connections,source,ids,CheDoXemHoaDon.INVOICES);
    }
    private InvoicePanel(DesktopSession session, ConnectionProvider connections, ClinicalBillingSource source, InvoiceIdGenerator ids, CheDoXemHoaDon mode) {
        this.session=session;this.connections=connections;this.mode=mode;
        new AuthorizationService(new RolePermissionPolicy()).require(session, Quyen.BILLING_READ);
        canPay = new AuthorizationService(new RolePermissionPolicy()).can(session, Quyen.BILLING_MANAGE);
        canRequestCancellation=new AuthorizationService(new RolePermissionPolicy()).can(session,Quyen.INVOICE_CANCEL_REQUEST);
        btnYeuCauHuy.setVisible(mode==CheDoXemHoaDon.INVOICES&&canRequestCancellation);
        btnYeuCauHuy.setEnabled(canRequestCancellation);
        canCreate = new AuthorizationService(new RolePermissionPolicy()).can(session, Quyen.BILLING_MANAGE)
                && source != null && ids != null;
        btnTaoHoaDon.setEnabled(canCreate);
        btnTaoHoaDon.setVisible(mode==CheDoXemHoaDon.INVOICES);
        btnThanhToan.setText(mode==CheDoXemHoaDon.DEBTS?"Thu nợ":"Thanh toán");btnThanhToan.setEnabled(canPay);
        btnTaoHoaDon.setToolTipText(canCreate ? "Lập từ dịch vụ khám được phép tính phí" : "Chờ quyền BILLING_MANAGE, hợp đồng tính phí và bộ sinh mã được duyệt");
        setLayout(new BorderLayout(10, 10)); setBorder(BorderFactory.createEmptyBorder(24, 26, 24, 26));
        JPanel pnlTop = new JPanel(new BorderLayout(0, 8));
        JLabel lblTitle = new JLabel(switch(mode){case INVOICES -> "Hóa đơn";case DEBTS -> "Công nợ";case PAYMENTS -> "Thanh toán";});
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 22f)); pnlTop.add(lblTitle, BorderLayout.NORTH);
        cmbTrangThai.addItem("Tất cả");
        for (TrangThaiHoaDon value : TrangThaiHoaDon.values()) cmbTrangThai.addItem(value);
        tblThanhToan.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int column = 0; column < tblThanhToan.getColumnCount(); column++) {
            tblThanhToan.getColumnModel().getColumn(column).setPreferredWidth(column == 10 ? 260 : 150);
        }
        JPanel pnlFilter = BillingUi.filters(BillingUi.field("Hóa đơn / bệnh nhân / SĐT",txtTuKhoa),BillingUi.dateField(txtTuNgay,"Ngày lập từ"),BillingUi.dateField(txtDenNgay,"Đến ngày"),BillingUi.field("Trạng thái",cmbTrangThai),BillingUi.field(" ",BillingUi.actions(btnTim,btnTaiLai)));
        JPanel pnlActions=BillingUi.actions(BillingUi.primary(btnTaoHoaDon),btnThanhToan,BillingUi.danger(btnYeuCauHuy),btnDieuChinh);
        // Keep reconciliation in the model/service; show an explained warning only when needed.
        tblHoaDon.removeColumn(tblHoaDon.getColumnModel().getColumn(8));
        JPanel pnlToolbar=new JPanel(new BorderLayout(0,8));pnlToolbar.add(pnlFilter,BorderLayout.NORTH);pnlToolbar.add(pnlActions,BorderLayout.SOUTH);lblCanhBaoDanhSach.setVisible(false);pnlToolbar.add(lblCanhBaoDanhSach,BorderLayout.CENTER);
        pnlTop.add(pnlToolbar, BorderLayout.SOUTH); add(pnlTop, BorderLayout.NORTH);
        JScrollPane scrHoaDon = BillingUi.scroll(tblHoaDon);
        JScrollPane scrChiTiet = BillingUi.scroll(tblChiTietHoaDon), scrThanhToan = BillingUi.scroll(tblThanhToan);
        JTabbedPane tabBilling = new JTabbedPane();
        JPanel prescriptionPanel = new JPanel(new BorderLayout(0,4));
        JLabel prescriptionTitle = new JLabel("Thuốc chưa chốt giá không tính vào tổng tiền.");
        prescriptionPanel.add(prescriptionTitle,BorderLayout.NORTH);
        prescriptionPanel.add(BillingUi.scroll(tblPrescribedMedicines),BorderLayout.CENTER);
        tblPrescribedMedicines.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabBilling.addTab("Chi tiết hóa đơn", scrChiTiet);
        tabBilling.addTab("Thuốc trong đơn", prescriptionPanel);
        tabBilling.addTab("Lịch sử thanh toán", scrThanhToan);
        tabBilling.addTab("Lịch sử yêu cầu hủy", BillingUi.scroll(tblCancellationHistory));
        add(BillingUi.split(JSplitPane.VERTICAL_SPLIT,scrHoaDon,tabBilling,.58), BorderLayout.CENTER);
        JPanel pnlBottom = new JPanel(new BorderLayout(0,6));
        JPanel pnlTotals = new JPanel(new GridLayout(1, 2, 12, 0));
        pnlTotals.add(BillingUi.card("Đã thu",lblDaThu)); pnlTotals.add(BillingUi.card("Còn phải thu",lblConNo));
        if(mode==CheDoXemHoaDon.DEBTS)pnlTotals.add(lblTongCongNo);
        pnlBottom.add(pnlTotals,BorderLayout.NORTH);pnlBottom.add(lblCanhBao,BorderLayout.CENTER);
        JPanel paging=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));paging.add(btnTrangTruoc);paging.add(lblTrang);paging.add(btnTrangSau);pnlBottom.add(paging,BorderLayout.EAST);
        pnlBottom.add(lblTrangThai,BorderLayout.SOUTH); add(pnlBottom, BorderLayout.SOUTH);
        controller = new InvoiceController(this, session, source == null || ids == null
                ? new InvoiceService(session, connections) : new InvoiceService(session, connections, source, ids));
        boolean manager=new AuthorizationService(new RolePermissionPolicy()).can(session,Quyen.INVOICE_CANCEL_APPROVE);
        btnDieuChinh.setVisible(manager);
        new com.quanlynhakhoa.nghiepvu.hoadon.controller.InvoiceAdjustmentController(this,session,connections);
        BillingUi.style(this);
        BillingUi.columns(tblHoaDon,110,85,290,110,180,150,150,150,160);BillingUi.dateColumn(tblHoaDon,3);
        BillingUi.columns(tblPrescribedMedicines,110,300,100,100,170,170);
        BillingUi.tableState(tblPrescribedMedicines,"Chọn hóa đơn để xem thuốc được kê.");
        BillingUi.columns(tblCancellationHistory,120,140,180,300,150,140,180,300);
        BillingUi.tableState(tblCancellationHistory,"Chọn hóa đơn để xem lịch sử yêu cầu hủy.");
        BillingUi.columns(tblChiTietHoaDon,85,100,330,85,160,165);
        BillingUi.tableState(tblChiTietHoaDon,"Chọn hóa đơn để xem chi tiết.");BillingUi.tableState(tblThanhToan,"Chọn hóa đơn để xem lịch sử thanh toán.");
        updateActions();
    }
    @Override public void addNotify() {
        super.addNotify();
        if (!loaded) { loaded = true; SwingUtilities.invokeLater(new Runnable() { public void run() { controller.load(); } }); }
    }
    public String choosePaymentForAdjustment(){
        if(selectedDetail==null||!selectedDetail.maHoaDon().equals(selectedId())){
            BillingUi.result(this,"Chi tiết hóa đơn chưa tải xong. Chọn hóa đơn và chờ tải xong rồi thử lại.");return null;
        }
        java.util.List<LuaChonLapHoaDon> choices=new java.util.ArrayList<>();
        for(int row=0;row<paymentModel.getRowCount();row++){
            if(paymentModel.getValueAt(row,6)==TrangThaiThanhToan.THANH_CONG){
                String id=String.valueOf(paymentModel.getValueAt(row,0));
                choices.add(new LuaChonLapHoaDon(id,BillingUi.vnd((java.math.BigDecimal)paymentModel.getValueAt(row,2))+" · "+paymentModel.getValueAt(row,3)));
            }
        }
        if(choices.isEmpty()){BillingUi.result(this,"Hóa đơn này không có khoản thu thành công để điều chỉnh.");return null;}
        LuaChonLapHoaDon initial=choices.getFirst();String selected=selectedPaymentId();
        for(LuaChonLapHoaDon item:choices)if(item.id().equals(selected))initial=item;
        Object chosen=JOptionPane.showInputDialog(this,"Chọn khoản thu cần điều chỉnh:","Điều chỉnh khoản thu",JOptionPane.QUESTION_MESSAGE,null,choices.toArray(),initial);
        return chosen instanceof LuaChonLapHoaDon item?item.id():null;
    }

    public String selectedPaymentId(){
        int row=tblThanhToan.getSelectedRow();if(row<0)return null;
        return String.valueOf(tblThanhToan.getModel().getValueAt(tblThanhToan.convertRowIndexToModel(row),0));
    }

    public void onSearch(ActionListener listener) { btnTim.addActionListener(listener); txtTuKhoa.addActionListener(listener); txtTuNgay.addActionListener(listener); txtDenNgay.addActionListener(listener); cmbTrangThai.addActionListener(listener); }
    public void onReload(ActionListener listener) { btnTaiLai.addActionListener(listener); }
    public void onCreate(ActionListener listener) { btnTaoHoaDon.addActionListener(listener); }
    public void onPay(ActionListener listener) { btnThanhToan.addActionListener(listener); }
    public void onRequestCancellation(ActionListener listener){btnYeuCauHuy.addActionListener(listener);}
    public void requestCancellation(){
        if(!new AuthorizationService(new RolePermissionPolicy()).can(session,Quyen.INVOICE_CANCEL_REQUEST)){status("Không có quyền gửi yêu cầu hủy.");return;}
        String id=selectedId();if(id==null){status("Chọn một hóa đơn để gửi yêu cầu hủy.");return;}
        CancellationRequestDialog winRequest=new CancellationRequestDialog(SwingUtilities.getWindowAncestor(this));
        new com.quanlynhakhoa.nghiepvu.hoadon.controller.CancellationRequestController(winRequest,session,new com.quanlynhakhoa.nghiepvu.hoadon.service.CancellationService(session,connections),id,new Runnable(){public void run(){status("Yêu cầu đã gửi; hóa đơn chưa bị hủy, chờ xét duyệt.");controller.load();}});
        winRequest.setVisible(true);
    }
    public boolean outstandingOnly(){return mode!=CheDoXemHoaDon.INVOICES;}
    public void setPaymentNavigation(PaymentNavigation navigation){paymentNavigation=navigation;}
    public void paySelected(){
        String id=selectedId();if(id==null){status("Chọn một hóa đơn để thanh toán.");return;}
        if(!new AuthorizationService(new RolePermissionPolicy()).can(session,Quyen.BILLING_MANAGE)){status("Không có quyền thu tiền.");return;}
        if(paymentNavigation!=null&&mode!=CheDoXemHoaDon.PAYMENTS)paymentNavigation.open(id);else payInvoice(id);
    }
    public void payInvoice(String id){
        if(!new AuthorizationService(new RolePermissionPolicy()).can(session,Quyen.BILLING_MANAGE)){status("Không có quyền thu tiền.");return;}
        if(mode==CheDoXemHoaDon.PAYMENTS)controller.focusInvoice(id);
        PaymentDialog winPayment=new PaymentDialog(SwingUtilities.getWindowAncestor(this));
        new com.quanlynhakhoa.nghiepvu.hoadon.controller.PaymentController(winPayment,session,connections,id,new Runnable(){public void run(){controller.paymentSaved();}});
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
    public void showRows(List<ThongTinHoaDon> values) {
        rows = List.of(); invoiceModel.setRowCount(0); rows = List.copyOf(values);
        for (ThongTinHoaDon value : rows) invoiceModel.addRow(new Object[]{value.maHoaDon(), value.maBenhNhan(), value.tenBenhNhan(),
                value.ngayLap(), value.trangThai(), value.tongTien(), value.daThu(), value.noTheoDoi(), value.canhBao(), value.cancellationLabel()});
        clearDetail();
        int warnings=0;for(ThongTinHoaDon invoice:values)if(invoice.canhBao().startsWith("BẤT NHẤT"))warnings++;
        lblCanhBaoDanhSach.setText(warnings+" hóa đơn có số tiền hoặc trạng thái cần đối soát. Chọn hóa đơn để xem cảnh báo.");lblCanhBaoDanhSach.setForeground(new Color(147,69,20));lblCanhBaoDanhSach.setVisible(warnings>0);
        java.math.BigDecimal debt=java.math.BigDecimal.ZERO;
        for(ThongTinHoaDon h:values)debt=debt.add(h.noTheoDoi());
        lblTongCongNo.setText("Tổng công nợ: "+BillingUi.vnd(debt));
    }
    public void clearDetail() {
        selectedDetail=null;updateActions();
        lineModel.setRowCount(0); paymentModel.setRowCount(0); cancellationModel.setRowCount(0);prescriptionModel.setRowCount(0);
        BillingUi.tableState(tblPrescribedMedicines,"Chọn hóa đơn để xem thuốc được kê.");
        BillingUi.tableState(tblCancellationHistory,"Chọn hóa đơn để xem lịch sử yêu cầu hủy.");
        BillingUi.tableState(tblChiTietHoaDon,"Chọn hóa đơn để xem chi tiết.");BillingUi.tableState(tblThanhToan,"Chọn hóa đơn để xem lịch sử thanh toán.");
        lblTongTien.setText("Tổng: —"); lblDaThu.setText("Đã thu: —"); lblConNo.setText("Còn phải thu: —"); lblCongNoBenhNhan.setText("Công nợ BN: —"); lblCanhBao.setText(" ");lblCanhBao.setVisible(false);
    }
    public void showDetail(DuLieuChiTietHoaDon value) {
        clearDetail();
        for (DongChiTietHoaDon line : value.chiTiet()) lineModel.addRow(new Object[]{line.loaiHang(), line.maHang(), line.tenHienThi(), line.soLuong(), line.donGiaApDung(), line.thanhTien()});
        for (ThuocKeTinhTien medicine : value.prescribedMedicines()) {
            prescriptionModel.addRow(new Object[]{medicine.medicineId(),medicine.name(),medicine.unit(),medicine.quantity(),
                    medicine.unitPrice()==null?"Chưa chốt":medicine.unitPrice(),medicine.total()});
        }
        BillingUi.tableState(tblPrescribedMedicines,"Lượt khám của hóa đơn chưa có thuốc được kê.");
        for (ThongTinThanhToan payment : value.thanhToan()) paymentModel.addRow(new Object[]{payment.maThanhToan(), payment.thoiGian(),
                payment.soTien(), payment.phuongThuc(), payment.maGiaoDich(), payment.tienKhachDua(), payment.trangThai(),
                payment.maNhanVien(), payment.maTaiKhoanHuy(), BillingUi.time(payment.thoiGianHuy()), payment.lyDoHuy()});
        for (ThongTinYeuCauHuyHoaDon request : value.cancellationHistory()) {
            cancellationModel.addRow(new Object[]{request.maYeuCau(), request.maTaiKhoanTao(), request.thoiGianTao(),
                    request.lyDo(), request.trangThai(), request.maTaiKhoanDuyet(), request.thoiGianDuyet(), request.lyDoDuyet()});
        }
        BillingUi.tableState(tblCancellationHistory,"Hóa đơn chưa có yêu cầu hủy.");
        ThongTinHoaDon invoice = value.hoaDon();
        lblTongTien.setText("Tổng: " + BillingUi.vnd(invoice.tongTien())); lblDaThu.setText("Đã thu: " + BillingUi.vnd(invoice.daThu()));
        lblConNo.setText("Còn phải thu: " + BillingUi.vnd(invoice.noTheoDoi())); lblCongNoBenhNhan.setText("Công nợ BN: " + BillingUi.vnd(invoice.congNoBenhNhan()));
        lblCanhBao.setText(invoice.canhBao());
        lblCanhBao.setToolTipText("Đã thu và tổng chi tiết được đối chiếu với trạng thái chứng từ. Cần kiểm tra trước khi thu nếu có bất nhất.");
        lblCanhBao.setVisible(!invoice.canhBao().isBlank());
        selectedDetail=invoice;updateActions();
        BillingUi.tableState(tblChiTietHoaDon,"Hóa đơn chưa có dòng chi tiết.");BillingUi.tableState(tblThanhToan,"Chưa có khoản thanh toán.");
    }
    public void setBusy(boolean busy) {
        working=busy;
        for (Component control : new Component[]{txtTuKhoa, txtTuNgay, txtDenNgay, cmbTrangThai, btnTim, btnTaiLai, tblHoaDon,btnDieuChinh}) control.setEnabled(!busy);
        btnTaoHoaDon.setEnabled(!busy && canCreate);
        updateActions();
    }
    private void updateActions(){boolean valid=selectedDetail!=null&&selectedDetail.maHoaDon().equals(selectedId())&&selectedDetail.trangThai()!=TrangThaiHoaDon.DA_HUY;btnThanhToan.setEnabled(!working&&valid&&canPay&&selectedDetail.conNo().signum()>0&&selectedDetail.canhBao().isEmpty());btnYeuCauHuy.setEnabled(!working&&valid&&canRequestCancellation&&selectedDetail.daThu().signum()==0&&!selectedDetail.cancellationPending());}
    public void status(String text) { lblTrangThai.setVisible(!text.isBlank());lblTrangThai.setText(text);lblTrangThai.setToolTipText(text);BillingUi.tableState(tblHoaDon,text);if(invoiceModel.getRowCount()==0&&(text.startsWith("Chưa có hóa đơn")||text.startsWith("Không tìm thấy")))lblTrangThai.setText("Tìm kiếm hoặc tạo hóa đơn để tiếp tục."); }
    public void expired() { showRows(List.of()); setBusy(true); status("Phiên đã đăng xuất."); }
}
