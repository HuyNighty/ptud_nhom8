package com.quanlynhakhoa.view.hoadon;

import com.quanlynhakhoa.security.RolePermissionPolicy;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.enums.TrangThaiThanhToan;
import com.quanlynhakhoa.enums.TrangThaiHoaDon;
import com.quanlynhakhoa.enums.CheDoXemHoaDon;
import com.quanlynhakhoa.model.hoadon.ThongTinYeuCauHuyHoaDon;
import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import com.quanlynhakhoa.model.hoadon.DuLieuChiTietHoaDon;
import com.quanlynhakhoa.entity.ThanhToan;
import com.quanlynhakhoa.model.hoadon.ThuocKeTinhTien;
import com.quanlynhakhoa.model.hoadon.LuaChonLapHoaDon;
import com.quanlynhakhoa.model.hoadon.DongChiTietHoaDon;
import com.quanlynhakhoa.controller.hoadon.HoaDonController;
import com.quanlynhakhoa.config.ConnectionProvider;
import com.quanlynhakhoa.service.hoadon.HoaDonService;
import com.quanlynhakhoa.service.hoadon.NguonTinhPhiLamSang;
import com.quanlynhakhoa.service.hoadon.TaoMaHoaDon;
import com.quanlynhakhoa.config.phattrien.TeamDevConnection;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

public final class HoaDonPanel extends JPanel implements com.quanlynhakhoa.config.tinhnang.RefreshableFeature {

    public interface PaymentNavigation { void open(String invoiceId); }
    private final CheDoXemHoaDon mode;
    private final DesktopSession session;
    private final ConnectionProvider connections;
    private PaymentNavigation dieuHuongThanhToan;
    private boolean duocThanhToan;
    public final JButton btnTrangTruoc=new JButton("‹ Trang trước"),btnTrangSau=new JButton("Trang sau ›");
    private final JLabel lblTrang=new JLabel("Trang 1");
    public void pageState(int page,boolean next){lblTrang.setText("Trang "+(page+1));btnTrangTruoc.setEnabled(page>0);btnTrangSau.setEnabled(next);}
    public final JButton btnDieuChinh=new JButton("Điều chỉnh khoản thu");
    private final JButton btnThanhToan = new JButton("Thanh toán");
    private final JButton btnYeuCauHuy = new JButton("Yêu cầu hủy");
    private final boolean duocYeuCauHuy;
    private final JLabel lblTongCongNo = new JLabel("Tổng công nợ: —");
    private final JTextField txtTuKhoa = new JTextField(15), txtTuNgay = new JTextField(10), txtDenNgay = new JTextField(10);
    private final JComboBox<Object> cmbTrangThai = new JComboBox<>();
    private final JButton btnTim = new JButton("Tìm"), btnTaiLai = new JButton("Làm mới");
    private final JButton btnTaoHoaDon = new JButton("Tạo hóa đơn");
    private final boolean duocTao;
    private final JLabel lblTrangThai = new JLabel("Chọn hóa đơn để xem chi tiết."), lblTongTien = new JLabel("Tổng: —");
    private final JLabel lblDaThu = new JLabel("Đã thu: —"), lblConNo = new JLabel("Còn phải thu: —"), lblCongNoBenhNhan = new JLabel("Công nợ BN: —");
    private final JLabel lblCanhBao = new JLabel(" ");
    private final JLabel lblCanhBaoDanhSach=new JLabel(" ");
    private final DefaultTableModel modelHoaDon = GiaoDienHoaDon.model("Mã hóa đơn", "Mã BN", "Bệnh nhân", "Ngày lập", "Trạng thái", "Tổng tiền", "Đã thu", "Còn phải thu", "Đối soát", "Yêu cầu hủy");
    private final DefaultTableModel modelDong = GiaoDienHoaDon.model("Loại", "Mã hàng", "Tên dịch vụ / hàng", "Số lượng", "Đơn giá đã chốt", "Thành tiền");
    private final DefaultTableModel modelThanhToan = GiaoDienHoaDon.model("Mã thu", "Thời gian", "Số tiền", "Phương thức", "Mã giao dịch", "Khách đưa", "Trạng thái", "Nhân viên", "TK hủy khoản thu", "Thời gian hủy khoản thu", "Lý do hủy khoản thu");
    private final DefaultTableModel modelLichSuHuy = GiaoDienHoaDon.model("Mã yêu cầu", "Người gửi", "Thời gian gửi",
            "Lý do yêu cầu", "Kết quả", "Người duyệt", "Thời gian duyệt", "Ý kiến xét duyệt");
    private final JTable tblLichSuHuy = GiaoDienHoaDon.table(modelLichSuHuy);
    private final DefaultTableModel modelDonThuoc = GiaoDienHoaDon.model("Mã thuốc", "Tên thuốc", "Đơn vị",
            "Số lượng", "Đơn giá đã chốt", "Thành tiền");
    private final JTable tblThuocDaKe = GiaoDienHoaDon.table(modelDonThuoc);
    private final JTable tblHoaDon = GiaoDienHoaDon.table(modelHoaDon), tblChiTietHoaDon = GiaoDienHoaDon.table(modelDong), tblThanhToan = GiaoDienHoaDon.table(modelThanhToan);
    private final HoaDonController controller;
    private List<ThongTinHoaDon> danhSach = List.of();
    private boolean loaded;
    private boolean working;
    private ThongTinHoaDon hoaDonDaChon;

    public HoaDonPanel(DesktopSession session) { this(session, TeamDevConnection::open); }
    public HoaDonPanel(DesktopSession session, ConnectionProvider connections) {
        this(session, connections, CheDoXemHoaDon.INVOICES);
    }
    public HoaDonPanel(DesktopSession session, ConnectionProvider connections, CheDoXemHoaDon mode) {
        this(session, connections, new com.quanlynhakhoa.service.hoadon.JdbcNguonTinhPhiLamSang(),
                new com.quanlynhakhoa.service.hoadon.TaoMaHoaDonNgauNhien(), mode);
    }
    public HoaDonPanel(DesktopSession session, ConnectionProvider connections, NguonTinhPhiLamSang source, TaoMaHoaDon ids) {
        this(session,connections,source,ids,CheDoXemHoaDon.INVOICES);
    }
    private HoaDonPanel(DesktopSession session, ConnectionProvider connections, NguonTinhPhiLamSang source, TaoMaHoaDon ids, CheDoXemHoaDon mode) {
        this.session=session;this.connections=connections;this.mode=mode;
        new AuthorizationService(new RolePermissionPolicy()).require(session, Quyen.BILLING_READ);
        duocThanhToan = new AuthorizationService(new RolePermissionPolicy()).can(session, Quyen.BILLING_MANAGE);
        duocYeuCauHuy=new AuthorizationService(new RolePermissionPolicy()).can(session,Quyen.INVOICE_CANCEL_REQUEST);
        btnYeuCauHuy.setVisible(mode==CheDoXemHoaDon.INVOICES&&duocYeuCauHuy);
        btnYeuCauHuy.setEnabled(duocYeuCauHuy);
        duocTao = new AuthorizationService(new RolePermissionPolicy()).can(session, Quyen.BILLING_MANAGE)
                && source != null && ids != null;
        btnTaoHoaDon.setEnabled(duocTao);
        btnTaoHoaDon.setVisible(mode==CheDoXemHoaDon.INVOICES);
        btnThanhToan.setText(mode==CheDoXemHoaDon.DEBTS?"Thu nợ":"Thanh toán");btnThanhToan.setEnabled(duocThanhToan);
        btnTaoHoaDon.setToolTipText(duocTao ? "Lập từ dịch vụ khám được phép tính phí" : "Chờ quyền BILLING_MANAGE, hợp đồng tính phí và bộ sinh mã được duyệt");
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
        JPanel pnlFilter = GiaoDienHoaDon.filters(GiaoDienHoaDon.field("Hóa đơn / bệnh nhân / SĐT",txtTuKhoa),GiaoDienHoaDon.dateField(txtTuNgay,"Ngày lập từ"),GiaoDienHoaDon.dateField(txtDenNgay,"Đến ngày"),GiaoDienHoaDon.field("Trạng thái",cmbTrangThai),GiaoDienHoaDon.field(" ",GiaoDienHoaDon.actions(btnTim,btnTaiLai)));
        JPanel pnlActions=GiaoDienHoaDon.actions(GiaoDienHoaDon.primary(btnTaoHoaDon),btnThanhToan,GiaoDienHoaDon.danger(btnYeuCauHuy),btnDieuChinh);
        // Keep reconciliation in the model/service; show an explained warning only when needed.
        tblHoaDon.removeColumn(tblHoaDon.getColumnModel().getColumn(8));
        JPanel pnlToolbar=new JPanel(new BorderLayout(0,8));pnlToolbar.add(pnlFilter,BorderLayout.NORTH);pnlToolbar.add(pnlActions,BorderLayout.SOUTH);lblCanhBaoDanhSach.setVisible(false);pnlToolbar.add(lblCanhBaoDanhSach,BorderLayout.CENTER);
        pnlTop.add(pnlToolbar, BorderLayout.SOUTH); add(pnlTop, BorderLayout.NORTH);
        JScrollPane scrHoaDon = GiaoDienHoaDon.scroll(tblHoaDon);
        JScrollPane scrChiTiet = GiaoDienHoaDon.scroll(tblChiTietHoaDon), scrThanhToan = GiaoDienHoaDon.scroll(tblThanhToan);
        JTabbedPane tabBilling = new JTabbedPane();
        JPanel prescriptionPanel = new JPanel(new BorderLayout(0,4));
        JLabel prescriptionTitle = new JLabel("Thuốc chưa chốt giá không tính vào tổng tiền.");
        prescriptionPanel.add(prescriptionTitle,BorderLayout.NORTH);
        prescriptionPanel.add(GiaoDienHoaDon.scroll(tblThuocDaKe),BorderLayout.CENTER);
        tblThuocDaKe.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabBilling.addTab("Chi tiết hóa đơn", scrChiTiet);
        tabBilling.addTab("Thuốc trong đơn", prescriptionPanel);
        tabBilling.addTab("Lịch sử thanh toán", scrThanhToan);
        tabBilling.addTab("Lịch sử yêu cầu hủy", GiaoDienHoaDon.scroll(tblLichSuHuy));
        add(GiaoDienHoaDon.split(JSplitPane.VERTICAL_SPLIT,scrHoaDon,tabBilling,.58), BorderLayout.CENTER);
        JPanel pnlBottom = new JPanel(new BorderLayout(0,6));
        JPanel pnlTotals = new JPanel(new GridLayout(1, 2, 12, 0));
        pnlTotals.add(GiaoDienHoaDon.card("Đã thu",lblDaThu)); pnlTotals.add(GiaoDienHoaDon.card("Còn phải thu",lblConNo));
        if(mode==CheDoXemHoaDon.DEBTS)pnlTotals.add(lblTongCongNo);
        pnlBottom.add(pnlTotals,BorderLayout.NORTH);pnlBottom.add(lblCanhBao,BorderLayout.CENTER);
        JPanel paging=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));paging.add(btnTrangTruoc);paging.add(lblTrang);paging.add(btnTrangSau);pnlBottom.add(paging,BorderLayout.EAST);
        pnlBottom.add(lblTrangThai,BorderLayout.SOUTH); add(pnlBottom, BorderLayout.SOUTH);
        controller = new HoaDonController(this, session, source == null || ids == null
                ? new HoaDonService(session, connections) : new HoaDonService(session, connections, source, ids));
        boolean manager=new AuthorizationService(new RolePermissionPolicy()).can(session,Quyen.INVOICE_CANCEL_APPROVE);
        btnDieuChinh.setVisible(manager);
        new com.quanlynhakhoa.controller.hoadon.DieuChinhHoaDonController(this,session,connections);
        GiaoDienHoaDon.style(this);
        GiaoDienHoaDon.columns(tblHoaDon,110,85,290,110,180,150,150,150,160);GiaoDienHoaDon.dateColumn(tblHoaDon,3);
        GiaoDienHoaDon.columns(tblThuocDaKe,110,300,100,100,170,170);
        GiaoDienHoaDon.tableState(tblThuocDaKe,"Chọn hóa đơn để xem thuốc được kê.");
        GiaoDienHoaDon.columns(tblLichSuHuy,120,140,180,300,150,140,180,300);
        GiaoDienHoaDon.tableState(tblLichSuHuy,"Chọn hóa đơn để xem lịch sử yêu cầu hủy.");
        GiaoDienHoaDon.columns(tblChiTietHoaDon,85,100,330,85,160,165);
        GiaoDienHoaDon.tableState(tblChiTietHoaDon,"Chọn hóa đơn để xem chi tiết.");GiaoDienHoaDon.tableState(tblThanhToan,"Chọn hóa đơn để xem lịch sử thanh toán.");
        updateActions();
    }
    @Override public void addNotify() {
        super.addNotify();
        if (!loaded) { loaded = true; SwingUtilities.invokeLater(new Runnable() { public void run() { controller.load(); } }); }
    }
    public String choosePaymentForAdjustment(){
        if(hoaDonDaChon==null||!hoaDonDaChon.maHoaDon().equals(selectedId())){
            GiaoDienHoaDon.result(this,"Chi tiết hóa đơn chưa tải xong. Chọn hóa đơn và chờ tải xong rồi thử lại.");return null;
        }
        java.util.List<LuaChonLapHoaDon> choices=new java.util.ArrayList<>();
        for(int row=0;row<modelThanhToan.getRowCount();row++){
            if(modelThanhToan.getValueAt(row,6)==TrangThaiThanhToan.THANH_CONG){
                String id=String.valueOf(modelThanhToan.getValueAt(row,0));
                choices.add(new LuaChonLapHoaDon(id,GiaoDienHoaDon.vnd((java.math.BigDecimal)modelThanhToan.getValueAt(row,2))+" · "+modelThanhToan.getValueAt(row,3)));
            }
        }
        if(choices.isEmpty()){GiaoDienHoaDon.result(this,"Hóa đơn này không có khoản thu thành công để điều chỉnh.");return null;}
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
        YeuCauHuyHoaDonDialog winRequest=new YeuCauHuyHoaDonDialog(SwingUtilities.getWindowAncestor(this));
        new com.quanlynhakhoa.controller.hoadon.YeuCauHuyHoaDonController(winRequest,session,new com.quanlynhakhoa.service.hoadon.HuyHoaDonService(session,connections),id,new Runnable(){public void run(){status("Yêu cầu đã gửi; hóa đơn chưa bị hủy, chờ xét duyệt.");controller.load();}});
        winRequest.setVisible(true);
    }
    public boolean outstandingOnly(){return mode!=CheDoXemHoaDon.INVOICES;}
    public void setPaymentNavigation(PaymentNavigation navigation){dieuHuongThanhToan=navigation;}
    public void paySelected(){
        String id=selectedId();if(id==null){status("Chọn một hóa đơn để thanh toán.");return;}
        if(!new AuthorizationService(new RolePermissionPolicy()).can(session,Quyen.BILLING_MANAGE)){status("Không có quyền thu tiền.");return;}
        if(dieuHuongThanhToan!=null&&mode!=CheDoXemHoaDon.PAYMENTS)dieuHuongThanhToan.open(id);else payInvoice(id);
    }
    public void payInvoice(String id){
        if(!new AuthorizationService(new RolePermissionPolicy()).can(session,Quyen.BILLING_MANAGE)){status("Không có quyền thu tiền.");return;}
        if(mode==CheDoXemHoaDon.PAYMENTS)controller.focusInvoice(id);
        ThanhToanDialog winPayment=new ThanhToanDialog(SwingUtilities.getWindowAncestor(this));
        new com.quanlynhakhoa.controller.hoadon.ThanhToanController(winPayment,session,connections,id,new Runnable(){public void run(){controller.paymentSaved();}});
        winPayment.setVisible(true);
    }
    @Override public void refreshData(){controller.load();}
    public void filterInvoice(String id) { txtTuKhoa.setText(id); txtTuNgay.setText(""); txtDenNgay.setText(""); cmbTrangThai.setSelectedIndex(0); }
    public void selectInvoice(String id) {
        for (int i=0;i<danhSach.size();i++) if(danhSach.get(i).maHoaDon().equals(id)) {
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
        return index >= 0 && index < danhSach.size() ? danhSach.get(index).maHoaDon() : null;
    }
    public void showRows(List<ThongTinHoaDon> values) {
        danhSach = List.of(); modelHoaDon.setRowCount(0); danhSach = List.copyOf(values);
        for (ThongTinHoaDon value : danhSach) modelHoaDon.addRow(new Object[]{value.maHoaDon(), value.maBenhNhan(), value.tenBenhNhan(),
                value.ngayLap(), value.trangThai(), value.tongTien(), value.daThu(), value.noTheoDoi(), value.canhBao(), value.cancellationLabel()});
        clearDetail();
        int warnings=0;for(ThongTinHoaDon invoice:values)if(invoice.canhBao().startsWith("BẤT NHẤT"))warnings++;
        lblCanhBaoDanhSach.setText(warnings+" hóa đơn có số tiền hoặc trạng thái cần đối soát. Chọn hóa đơn để xem cảnh báo.");lblCanhBaoDanhSach.setForeground(new Color(147,69,20));lblCanhBaoDanhSach.setVisible(warnings>0);
        java.math.BigDecimal debt=java.math.BigDecimal.ZERO;
        for(ThongTinHoaDon h:values)debt=debt.add(h.noTheoDoi());
        lblTongCongNo.setText("Tổng công nợ: "+GiaoDienHoaDon.vnd(debt));
    }
    public void clearDetail() {
        hoaDonDaChon=null;updateActions();
        modelDong.setRowCount(0); modelThanhToan.setRowCount(0); modelLichSuHuy.setRowCount(0);modelDonThuoc.setRowCount(0);
        GiaoDienHoaDon.tableState(tblThuocDaKe,"Chọn hóa đơn để xem thuốc được kê.");
        GiaoDienHoaDon.tableState(tblLichSuHuy,"Chọn hóa đơn để xem lịch sử yêu cầu hủy.");
        GiaoDienHoaDon.tableState(tblChiTietHoaDon,"Chọn hóa đơn để xem chi tiết.");GiaoDienHoaDon.tableState(tblThanhToan,"Chọn hóa đơn để xem lịch sử thanh toán.");
        lblTongTien.setText("Tổng: —"); lblDaThu.setText("Đã thu: —"); lblConNo.setText("Còn phải thu: —"); lblCongNoBenhNhan.setText("Công nợ BN: —"); lblCanhBao.setText(" ");lblCanhBao.setVisible(false);
    }
    public void showDetail(DuLieuChiTietHoaDon value) {
        clearDetail();
        for (DongChiTietHoaDon line : value.chiTiet()) modelDong.addRow(new Object[]{line.loaiHang(), line.maHang(), line.tenHienThi(), line.soLuong(), line.donGiaApDung(), line.thanhTien()});
        for (ThuocKeTinhTien medicine : value.prescribedMedicines()) {
            modelDonThuoc.addRow(new Object[]{medicine.medicineId(),medicine.name(),medicine.unit(),medicine.quantity(),
                    medicine.unitPrice()==null?"Chưa chốt":medicine.unitPrice(),medicine.total()});
        }
        GiaoDienHoaDon.tableState(tblThuocDaKe,"Lượt khám của hóa đơn chưa có thuốc được kê.");
        for (ThanhToan payment : value.thanhToan()) modelThanhToan.addRow(new Object[]{payment.maThanhToan(), payment.thoiGian(),
                payment.soTien(), payment.phuongThuc(), payment.maGiaoDich(), payment.tienKhachDua(), payment.trangThai(),
                payment.maNhanVien(), payment.maTaiKhoanHuy(), GiaoDienHoaDon.time(payment.thoiGianHuy()), payment.lyDoHuy()});
        for (ThongTinYeuCauHuyHoaDon request : value.cancellationHistory()) {
            modelLichSuHuy.addRow(new Object[]{request.maYeuCau(), request.maTaiKhoanTao(), request.thoiGianTao(),
                    request.lyDo(), request.trangThai(), request.maTaiKhoanDuyet(), request.thoiGianDuyet(), request.lyDoDuyet()});
        }
        GiaoDienHoaDon.tableState(tblLichSuHuy,"Hóa đơn chưa có yêu cầu hủy.");
        ThongTinHoaDon invoice = value.hoaDon();
        lblTongTien.setText("Tổng: " + GiaoDienHoaDon.vnd(invoice.tongTien())); lblDaThu.setText("Đã thu: " + GiaoDienHoaDon.vnd(invoice.daThu()));
        lblConNo.setText("Còn phải thu: " + GiaoDienHoaDon.vnd(invoice.noTheoDoi())); lblCongNoBenhNhan.setText("Công nợ BN: " + GiaoDienHoaDon.vnd(invoice.congNoBenhNhan()));
        lblCanhBao.setText(invoice.canhBao());
        lblCanhBao.setToolTipText("Đã thu và tổng chi tiết được đối chiếu với trạng thái chứng từ. Cần kiểm tra trước khi thu nếu có bất nhất.");
        lblCanhBao.setVisible(!invoice.canhBao().isBlank());
        hoaDonDaChon=invoice;updateActions();
        GiaoDienHoaDon.tableState(tblChiTietHoaDon,"Hóa đơn chưa có dòng chi tiết.");GiaoDienHoaDon.tableState(tblThanhToan,"Chưa có khoản thanh toán.");
    }
    public void setBusy(boolean busy) {
        working=busy;
        for (Component control : new Component[]{txtTuKhoa, txtTuNgay, txtDenNgay, cmbTrangThai, btnTim, btnTaiLai, tblHoaDon,btnDieuChinh}) control.setEnabled(!busy);
        btnTaoHoaDon.setEnabled(!busy && duocTao);
        updateActions();
    }
    private void updateActions(){boolean valid=hoaDonDaChon!=null&&hoaDonDaChon.maHoaDon().equals(selectedId())&&hoaDonDaChon.trangThai()!=TrangThaiHoaDon.DA_HUY;btnThanhToan.setEnabled(!working&&valid&&duocThanhToan&&hoaDonDaChon.conNo().signum()>0&&hoaDonDaChon.canhBao().isEmpty());btnYeuCauHuy.setEnabled(!working&&valid&&duocYeuCauHuy&&hoaDonDaChon.daThu().signum()==0&&!hoaDonDaChon.cancellationPending());}
    public void status(String text) { lblTrangThai.setVisible(!text.isBlank());lblTrangThai.setText(text);lblTrangThai.setToolTipText(text);GiaoDienHoaDon.tableState(tblHoaDon,text);if(modelHoaDon.getRowCount()==0&&(text.startsWith("Chưa có hóa đơn")||text.startsWith("Không tìm thấy")))lblTrangThai.setText("Tìm kiếm hoặc tạo hóa đơn để tiếp tục."); }
    public void expired() { showRows(List.of()); setBusy(true); status("Phiên đã đăng xuất."); }
}
