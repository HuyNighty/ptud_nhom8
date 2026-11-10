package com.quanlynhakhoa.view.hoadon;

import com.quanlynhakhoa.security.RolePermissionPolicy;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.enums.TrangThaiHoaDon;
import com.quanlynhakhoa.model.hoadon.TongHopCongNoBenhNhan;
import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import com.quanlynhakhoa.model.hoadon.DuLieuChiTietHoaDon;
import com.quanlynhakhoa.controller.hoadon.CongNoController;
import com.quanlynhakhoa.config.ConnectionProvider;
import com.quanlynhakhoa.config.tinhnang.RefreshableFeature;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class CongNoPanel extends JPanel implements RefreshableFeature {
    public final JTextField txtTuKhoa=new JTextField(18),txtTuNgay=new JTextField(10),txtDenNgay=new JTextField(10);
    public final JButton btnTim=new JButton("Tìm"),btnTaiLai=new JButton("Làm mới"),btnThuNo=new JButton("Thu nợ"),btnXemChiTiet=new JButton("Xem chi tiết");
    public final JLabel lblTongCongNo=new JLabel("Tổng còn phải thu: —"),lblTrangThai=new JLabel("Chọn bệnh nhân để xem các hóa đơn còn nợ.");
    private final DefaultTableModel modelBenhNhan=GiaoDienHoaDon.model("Mã bệnh nhân","Bệnh nhân","Số hóa đơn","Tổng phải trả","Đã thu","Còn phải thu");
    private final DefaultTableModel modelHoaDon=GiaoDienHoaDon.model("Mã hóa đơn","Ngày lập","Tổng tiền","Đã thu","Còn nợ","Trạng thái");
    public final JTable tblBenhNhan=GiaoDienHoaDon.table(modelBenhNhan),tblHoaDon=GiaoDienHoaDon.table(modelHoaDon);
    private final LichSuHoaDonPanel pnlChiTiet=new LichSuHoaDonPanel();
    private final JLabel lblSoBenhNhan=new JLabel("Bệnh nhân còn nợ: —"),lblSoHoaDon=new JLabel("Hóa đơn còn nợ: —");
    private final JLabel lblHoaDonBenhNhan=new JLabel("Hóa đơn còn nợ · chưa chọn bệnh nhân");
    private boolean working,duLieuConHieuLuc;private String maChiTiet;private JDialog winChiTiet;
    private final boolean duocThuTien;private final CongNoController controller;private final DesktopSession session;
    private HoaDonPanel.PaymentNavigation dieuHuongThanhToan;
    private List<TongHopCongNoBenhNhan> benhNhan=List.of();private List<ThongTinHoaDon> hoaDon=List.of();
    public CongNoPanel(DesktopSession session,ConnectionProvider connections){
        this.session=session;new AuthorizationService(new RolePermissionPolicy()).require(session,Quyen.BILLING_READ);duocThuTien=new AuthorizationService(new RolePermissionPolicy()).can(session,Quyen.BILLING_MANAGE);
        setLayout(new BorderLayout(10,10));setBorder(BorderFactory.createEmptyBorder(24,26,24,26));
        JLabel lblTitle=new JLabel("Công nợ theo bệnh nhân");lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD,22f));
        JPanel pnlFilter=GiaoDienHoaDon.filters(GiaoDienHoaDon.field("Bệnh nhân / hóa đơn / SĐT",txtTuKhoa),GiaoDienHoaDon.dateField(txtTuNgay,"Ngày lập hóa đơn từ"),GiaoDienHoaDon.dateField(txtDenNgay,"Đến ngày"),GiaoDienHoaDon.field(" ",GiaoDienHoaDon.actions(btnTim,btnTaiLai)));
        JPanel pnlTop=new JPanel(new BorderLayout(0,10));pnlTop.add(lblTitle,BorderLayout.NORTH);pnlTop.add(pnlFilter,BorderLayout.CENTER);pnlTop.add(GiaoDienHoaDon.row(GiaoDienHoaDon.card(lblTongCongNo),GiaoDienHoaDon.card(lblSoBenhNhan),GiaoDienHoaDon.card(lblSoHoaDon)),BorderLayout.SOUTH);add(pnlTop,BorderLayout.NORTH);
        JPanel pnlPatientHeader=new JPanel(new BorderLayout(0,8));pnlPatientHeader.add(new JLabel("Bệnh nhân còn nợ"),BorderLayout.NORTH);JLabel lblChonBenhNhan=new JLabel("Chọn bệnh nhân để xem các hóa đơn còn nợ.");lblChonBenhNhan.setPreferredSize(new Dimension(100,34));pnlPatientHeader.add(lblChonBenhNhan,BorderLayout.CENTER);
        JPanel pnlInvoiceHeader=new JPanel(new BorderLayout(0,8));pnlInvoiceHeader.add(lblHoaDonBenhNhan,BorderLayout.NORTH);pnlInvoiceHeader.add(GiaoDienHoaDon.actions(btnXemChiTiet,GiaoDienHoaDon.primary(btnThuNo)),BorderLayout.CENTER);
        JPanel pnlPatients=new JPanel(new BorderLayout(0,8));pnlPatients.add(pnlPatientHeader,BorderLayout.NORTH);pnlPatients.add(GiaoDienHoaDon.scroll(tblBenhNhan));
        JPanel pnlInvoices=new JPanel(new BorderLayout(0,8));pnlInvoices.add(pnlInvoiceHeader,BorderLayout.NORTH);pnlInvoices.add(GiaoDienHoaDon.scroll(tblHoaDon));
        add(GiaoDienHoaDon.split(JSplitPane.HORIZONTAL_SPLIT,pnlPatients,pnlInvoices,.5),BorderLayout.CENTER);add(lblTrangThai,BorderLayout.SOUTH);
        btnXemChiTiet.addActionListener(new java.awt.event.ActionListener(){public void actionPerformed(java.awt.event.ActionEvent event){openDetail();}});
        controller=new CongNoController(this,session,connections);busy(false);
        GiaoDienHoaDon.style(this);
        GiaoDienHoaDon.columns(tblBenhNhan,105,210,90,135,135,140);GiaoDienHoaDon.columns(tblHoaDon,105,105,140,140,140,170);GiaoDienHoaDon.dateColumn(tblHoaDon,1);tblBenhNhan.moveColumn(5,2);tblHoaDon.moveColumn(4,2);
    }
    @Override public void addNotify(){super.addNotify();refreshData();}
    @Override public void refreshData(){controller.load();}
    public void setPaymentNavigation(HoaDonPanel.PaymentNavigation navigation){dieuHuongThanhToan=navigation;}
    public void paySelected(){String id=selectedInvoice();if(id==null){lblTrangThai.setText("Chọn hóa đơn còn nợ của bệnh nhân để thu.");return;}if(!session.isActive()||!duocThuTien){lblTrangThai.setText("Không có quyền thu nợ.");return;}if(dieuHuongThanhToan==null){lblTrangThai.setText("Mở trang này từ menu Tổng kết để chuyển sang Thanh toán.");return;}dieuHuongThanhToan.open(id);}
    public void showPatients(List<TongHopCongNoBenhNhan> values){
        if(winChiTiet!=null)winChiTiet.dispose();maChiTiet=null;lblHoaDonBenhNhan.setText("Hóa đơn còn nợ · chưa chọn bệnh nhân");benhNhan=List.of();hoaDon=List.of();modelBenhNhan.setRowCount(0);modelHoaDon.setRowCount(0);pnlChiTiet.clear();benhNhan=List.copyOf(values);BigDecimal total=BigDecimal.ZERO;int count=0;
        for(TongHopCongNoBenhNhan p:benhNhan){modelBenhNhan.addRow(new Object[]{p.patientId(),p.name(),p.invoices().size(),p.total(),p.collected(),p.debt()});total=total.add(p.debt());count+=p.invoices().size();}lblTongCongNo.setText("Còn phải thu: "+GiaoDienHoaDon.vnd(total));lblTongCongNo.setToolTipText("Tổng công nợ ngày lập hóa đơn hiện tại");lblSoBenhNhan.setText("Bệnh nhân còn nợ: "+benhNhan.size());lblSoHoaDon.setText("Hóa đơn còn nợ: "+count);duLieuConHieuLuc=true;updateActions();
        GiaoDienHoaDon.tableState(tblBenhNhan,values.isEmpty()?"Không có khoản còn phải thu phù hợp.":"");GiaoDienHoaDon.tableState(tblHoaDon,"Chọn bệnh nhân để xem các hóa đơn còn nợ.");
    }
    public void selectPatient(){if(winChiTiet!=null)winChiTiet.dispose();maChiTiet=null;lblHoaDonBenhNhan.setText("Hóa đơn còn nợ · chưa chọn bệnh nhân");hoaDon=List.of();modelHoaDon.setRowCount(0);pnlChiTiet.clear();updateActions();int row=tblBenhNhan.getSelectedRow();if(row<0)return;row=tblBenhNhan.convertRowIndexToModel(row);if(row>=benhNhan.size())return;TongHopCongNoBenhNhan patient=benhNhan.get(row);lblHoaDonBenhNhan.setText("Hóa đơn còn nợ · "+patient.patientId()+" — "+patient.name());lblHoaDonBenhNhan.setToolTipText(lblHoaDonBenhNhan.getText());hoaDon=patient.invoices();for(ThongTinHoaDon h:hoaDon)modelHoaDon.addRow(new Object[]{h.maHoaDon(),h.ngayLap(),h.tongTien(),h.daThu(),h.conNo(),h.trangThai()});}
    public String selectedInvoice(){int row=tblHoaDon.getSelectedRow();if(row<0)return null;row=tblHoaDon.convertRowIndexToModel(row);return row<hoaDon.size()?hoaDon.get(row).maHoaDon():null;}
    public void showDetail(DuLieuChiTietHoaDon detail){pnlChiTiet.showInvoice(detail);maChiTiet=detail.hoaDon().maHoaDon();updateActions();}
    private void openDetail(){if(working||maChiTiet==null||!maChiTiet.equals(selectedInvoice()))return;if(winChiTiet!=null)winChiTiet.dispose();winChiTiet=new JDialog(SwingUtilities.getWindowAncestor(this),"Chi tiết hóa đơn · "+maChiTiet,Dialog.ModalityType.MODELESS);JPanel root=new JPanel(new BorderLayout());root.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));root.add(pnlChiTiet);GiaoDienHoaDon.style(root);winChiTiet.setContentPane(root);winChiTiet.setSize(980,520);winChiTiet.setLocationRelativeTo(this);winChiTiet.setVisible(true);}
    private void updateActions(){boolean selected=duLieuConHieuLuc&&selectedInvoice()!=null;boolean eligible=false;if(selected)for(ThongTinHoaDon h:hoaDon)if(h.maHoaDon().equals(selectedInvoice()))eligible=h.conNo().signum()>0&&h.trangThai()!=TrangThaiHoaDon.DA_HUY&&h.canhBao().isEmpty();btnThuNo.setEnabled(!working&&duocThuTien&&eligible);btnXemChiTiet.setEnabled(!working&&selected&&selectedInvoice().equals(maChiTiet));}
    public void busy(boolean busy){working=busy;for(Component control:new Component[]{txtTuKhoa,txtTuNgay,txtDenNgay,btnTim,btnTaiLai,tblBenhNhan,tblHoaDon})control.setEnabled(!busy);updateActions();}
    public void loading(){maChiTiet=null;lblTrangThai.setText("Đang tải công nợ…");GiaoDienHoaDon.tableState(tblBenhNhan,"Đang tải công nợ…");}
    public void failed(String message){maChiTiet=null;duLieuConHieuLuc=false;lblTrangThai.setText(message);lblTongCongNo.setText("Còn phải thu: —");lblSoBenhNhan.setText("Bệnh nhân còn nợ: —");lblSoHoaDon.setText("Hóa đơn còn nợ: —");GiaoDienHoaDon.tableState(tblBenhNhan,"Chưa tải được công nợ. Bấm Làm mới để thử lại.");updateActions();}
    public void expired(){showPatients(List.of());busy(true);lblTrangThai.setText("Phiên đã đăng xuất.");}
}
