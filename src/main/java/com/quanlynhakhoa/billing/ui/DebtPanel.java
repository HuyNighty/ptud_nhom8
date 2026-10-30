package com.quanlynhakhoa.billing.ui;

import com.quanlynhakhoa.billing.controller.DebtController;
import com.quanlynhakhoa.billing.dao.ConnectionProvider;
import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.features.RefreshableFeature;
import com.quanlynhakhoa.security.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class DebtPanel extends JPanel implements RefreshableFeature {
    public final JTextField txtTuKhoa=new JTextField(18),txtTuNgay=new JTextField(10),txtDenNgay=new JTextField(10);
    public final JButton btnTim=new JButton("Tìm"),btnTaiLai=new JButton("Làm mới"),btnThuNo=new JButton("Thu nợ"),btnXemChiTiet=new JButton("Xem chi tiết");
    public final JLabel lblTongCongNo=new JLabel("Tổng còn phải thu: —"),lblTrangThai=new JLabel("Chọn bệnh nhân để xem các hóa đơn còn nợ.");
    private final DefaultTableModel patientModel=BillingUi.model("Mã bệnh nhân","Bệnh nhân","Số hóa đơn","Tổng phải trả","Đã thu","Còn phải thu");
    private final DefaultTableModel invoiceModel=BillingUi.model("Mã hóa đơn","Ngày lập","Tổng tiền","Đã thu","Còn nợ","Trạng thái");
    public final JTable tblBenhNhan=BillingUi.table(patientModel),tblHoaDon=BillingUi.table(invoiceModel);
    private final InvoiceHistoryPanel pnlDetails=new InvoiceHistoryPanel();
    private final JLabel lblSoBenhNhan=new JLabel("Bệnh nhân còn nợ: —"),lblSoHoaDon=new JLabel("Hóa đơn còn nợ: —");
    private final JLabel lblHoaDonBenhNhan=new JLabel("Hóa đơn còn nợ · chưa chọn bệnh nhân");
    private boolean working,dataCurrent;private String detailId;private JDialog winDetails;
    private final boolean mayCollect;private final DebtController controller;private final DesktopSession session;
    private HoaDonPanel.PaymentNavigation paymentNavigation;
    private List<PatientDebt> patients=List.of();private List<HoaDon> invoices=List.of();
    public DebtPanel(DesktopSession session,ConnectionProvider connections){
        this.session=session;new AuthorizationService(new RolePermissionPolicy()).require(session,Permission.BILLING_READ);mayCollect=new AuthorizationService(new RolePermissionPolicy()).can(session,Permission.BILLING_MANAGE);
        setLayout(new BorderLayout(10,10));setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        JLabel lblTitle=new JLabel("CÔNG NỢ THEO BỆNH NHÂN");lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD,22f));
        JPanel pnlFilter=BillingUi.filters(BillingUi.field("Bệnh nhân / hóa đơn / SĐT",txtTuKhoa),BillingUi.dateField(txtTuNgay,"Ngày lập hóa đơn từ"),BillingUi.dateField(txtDenNgay,"Đến ngày"),BillingUi.field(" ",BillingUi.actions(btnTim,btnTaiLai)));
        JPanel pnlTop=new JPanel(new BorderLayout(0,10));pnlTop.add(lblTitle,BorderLayout.NORTH);pnlTop.add(pnlFilter,BorderLayout.CENTER);pnlTop.add(BillingUi.row(BillingUi.card(lblTongCongNo),BillingUi.card(lblSoBenhNhan),BillingUi.card(lblSoHoaDon)),BorderLayout.SOUTH);add(pnlTop,BorderLayout.NORTH);
        JPanel pnlPatientHeader=new JPanel(new BorderLayout(0,8));pnlPatientHeader.add(new JLabel("Bệnh nhân còn nợ theo bộ lọc"),BorderLayout.NORTH);JLabel lblChonBenhNhan=new JLabel("Chọn bệnh nhân để xem các hóa đơn còn nợ.");lblChonBenhNhan.setPreferredSize(new Dimension(100,34));pnlPatientHeader.add(lblChonBenhNhan,BorderLayout.CENTER);
        JPanel pnlInvoiceHeader=new JPanel(new BorderLayout(0,8));pnlInvoiceHeader.add(lblHoaDonBenhNhan,BorderLayout.NORTH);pnlInvoiceHeader.add(BillingUi.actions(btnXemChiTiet,BillingUi.primary(btnThuNo)),BorderLayout.CENTER);
        JPanel pnlPatients=new JPanel(new BorderLayout(0,8));pnlPatients.add(pnlPatientHeader,BorderLayout.NORTH);pnlPatients.add(BillingUi.scroll(tblBenhNhan));
        JPanel pnlInvoices=new JPanel(new BorderLayout(0,8));pnlInvoices.add(pnlInvoiceHeader,BorderLayout.NORTH);pnlInvoices.add(BillingUi.scroll(tblHoaDon));
        add(BillingUi.split(JSplitPane.HORIZONTAL_SPLIT,pnlPatients,pnlInvoices,.5),BorderLayout.CENTER);add(lblTrangThai,BorderLayout.SOUTH);
        btnXemChiTiet.addActionListener(new java.awt.event.ActionListener(){public void actionPerformed(java.awt.event.ActionEvent event){openDetail();}});
        controller=new DebtController(this,session,connections);busy(false);
        BillingUi.style(this);
        BillingUi.columns(tblBenhNhan,105,210,90,135,135,140);BillingUi.columns(tblHoaDon,105,105,140,140,140,170);BillingUi.dateColumn(tblHoaDon,1);tblBenhNhan.moveColumn(5,2);tblHoaDon.moveColumn(4,2);
    }
    @Override public void addNotify(){super.addNotify();refreshData();}
    @Override public void refreshData(){controller.load();}
    public void setPaymentNavigation(HoaDonPanel.PaymentNavigation navigation){paymentNavigation=navigation;}
    public void paySelected(){String id=selectedInvoice();if(id==null){lblTrangThai.setText("Chọn hóa đơn còn nợ của bệnh nhân để thu.");return;}if(!session.isActive()||!mayCollect){lblTrangThai.setText("Không có quyền thu nợ.");return;}if(paymentNavigation==null){lblTrangThai.setText("Mở trang này từ menu Tổng kết để chuyển sang Thanh toán.");return;}paymentNavigation.open(id);}
    public void showPatients(List<PatientDebt> values){
        if(winDetails!=null)winDetails.dispose();detailId=null;lblHoaDonBenhNhan.setText("Hóa đơn còn nợ · chưa chọn bệnh nhân");patients=List.of();invoices=List.of();patientModel.setRowCount(0);invoiceModel.setRowCount(0);pnlDetails.clear();patients=List.copyOf(values);BigDecimal total=BigDecimal.ZERO;int count=0;
        for(PatientDebt p:patients){patientModel.addRow(new Object[]{p.patientId(),p.name(),p.invoices().size(),p.total(),p.collected(),p.debt()});total=total.add(p.debt());count+=p.invoices().size();}lblTongCongNo.setText("Còn phải thu (bộ lọc): "+BillingUi.vnd(total));lblTongCongNo.setToolTipText("Tổng công nợ theo bộ lọc ngày lập hóa đơn hiện tại");lblSoBenhNhan.setText("Bệnh nhân còn nợ: "+patients.size());lblSoHoaDon.setText("Hóa đơn còn nợ: "+count);dataCurrent=true;updateActions();
        BillingUi.tableState(tblBenhNhan,values.isEmpty()?"Không có khoản còn phải thu phù hợp.":"");BillingUi.tableState(tblHoaDon,"Chọn bệnh nhân để xem các hóa đơn còn nợ.");
    }
    public void selectPatient(){if(winDetails!=null)winDetails.dispose();detailId=null;lblHoaDonBenhNhan.setText("Hóa đơn còn nợ · chưa chọn bệnh nhân");invoices=List.of();invoiceModel.setRowCount(0);pnlDetails.clear();updateActions();int row=tblBenhNhan.getSelectedRow();if(row<0)return;row=tblBenhNhan.convertRowIndexToModel(row);if(row>=patients.size())return;PatientDebt patient=patients.get(row);lblHoaDonBenhNhan.setText("Hóa đơn còn nợ · "+patient.patientId()+" — "+patient.name());lblHoaDonBenhNhan.setToolTipText(lblHoaDonBenhNhan.getText());invoices=patient.invoices();for(HoaDon h:invoices)invoiceModel.addRow(new Object[]{h.maHoaDon(),h.ngayLap(),h.tongTien(),h.daThu(),h.conNo(),h.trangThai()});}
    public String selectedInvoice(){int row=tblHoaDon.getSelectedRow();if(row<0)return null;row=tblHoaDon.convertRowIndexToModel(row);return row<invoices.size()?invoices.get(row).maHoaDon():null;}
    public void showDetail(ChiTietBilling detail){pnlDetails.showInvoice(detail);detailId=detail.hoaDon().maHoaDon();updateActions();}
    private void openDetail(){if(working||detailId==null||!detailId.equals(selectedInvoice()))return;if(winDetails!=null)winDetails.dispose();winDetails=new JDialog(SwingUtilities.getWindowAncestor(this),"Chi tiết hóa đơn · "+detailId,Dialog.ModalityType.MODELESS);JPanel root=new JPanel(new BorderLayout());root.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));root.add(pnlDetails);BillingUi.style(root);winDetails.setContentPane(root);winDetails.setSize(980,520);winDetails.setLocationRelativeTo(this);winDetails.setVisible(true);}
    private void updateActions(){boolean selected=dataCurrent&&selectedInvoice()!=null;boolean eligible=false;if(selected)for(HoaDon h:invoices)if(h.maHoaDon().equals(selectedInvoice()))eligible=h.conNo().signum()>0&&h.trangThai()!=TrangThaiHoaDon.DA_HUY&&h.canhBao().isEmpty();btnThuNo.setEnabled(!working&&mayCollect&&eligible);btnXemChiTiet.setEnabled(!working&&selected&&selectedInvoice().equals(detailId));}
    public void busy(boolean busy){working=busy;for(Component control:new Component[]{txtTuKhoa,txtTuNgay,txtDenNgay,btnTim,btnTaiLai,tblBenhNhan,tblHoaDon})control.setEnabled(!busy);updateActions();}
    public void loading(){detailId=null;lblTrangThai.setText("Đang tải công nợ…");BillingUi.tableState(tblBenhNhan,"Đang tải công nợ…");}
    public void failed(String message){detailId=null;dataCurrent=false;lblTrangThai.setText(message);lblTongCongNo.setText("Còn phải thu (bộ lọc): —");lblSoBenhNhan.setText("Bệnh nhân còn nợ: —");lblSoHoaDon.setText("Hóa đơn còn nợ: —");BillingUi.tableState(tblBenhNhan,"Chưa tải được công nợ. Bấm Làm mới để thử lại.");updateActions();}
    public void expired(){showPatients(List.of());busy(true);lblTrangThai.setText("Phiên đã đăng xuất.");}
}
