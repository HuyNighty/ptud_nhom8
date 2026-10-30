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
    public final JButton btnTim=new JButton("Tìm"),btnTaiLai=new JButton("Tải lại"),btnThuNo=new JButton("Thu nợ");
    public final JLabel lblTongCongNo=new JLabel("Tổng còn phải thu: —"),lblTrangThai=new JLabel("Chọn bệnh nhân để xem các hóa đơn còn nợ.");
    private final DefaultTableModel patientModel=BillingUi.model("Mã bệnh nhân","Bệnh nhân","Số hóa đơn","Tổng phải trả","Đã thu","Còn phải thu");
    private final DefaultTableModel invoiceModel=BillingUi.model("Mã hóa đơn","Ngày lập","Tổng tiền","Đã thu","Còn nợ","Trạng thái");
    public final JTable tblBenhNhan=BillingUi.table(patientModel),tblHoaDon=BillingUi.table(invoiceModel);
    private final InvoiceHistoryPanel pnlDetails=new InvoiceHistoryPanel();
    private final boolean mayCollect;private final DebtController controller;private final DesktopSession session;
    private HoaDonPanel.PaymentNavigation paymentNavigation;
    private List<PatientDebt> patients=List.of();private List<HoaDon> invoices=List.of();
    public DebtPanel(DesktopSession session,ConnectionProvider connections){
        this.session=session;new AuthorizationService(new RolePermissionPolicy()).require(session,Permission.BILLING_READ);mayCollect=new AuthorizationService(new RolePermissionPolicy()).can(session,Permission.BILLING_MANAGE);
        setLayout(new BorderLayout(10,10));setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        JLabel lblTitle=new JLabel("CÔNG NỢ THEO BỆNH NHÂN");lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD,22f));
        JPanel pnlFilter=new JPanel(new FlowLayout(FlowLayout.LEFT));pnlFilter.add(new JLabel("Bệnh nhân / HĐ / SĐT"));pnlFilter.add(txtTuKhoa);pnlFilter.add(new JLabel("Từ yyyy-MM-dd"));pnlFilter.add(txtTuNgay);pnlFilter.add(new JLabel("Đến"));pnlFilter.add(txtDenNgay);pnlFilter.add(btnTim);pnlFilter.add(btnTaiLai);pnlFilter.add(btnThuNo);
        JPanel pnlTop=new JPanel(new BorderLayout(0,8));pnlTop.add(lblTitle,BorderLayout.NORTH);pnlTop.add(pnlFilter,BorderLayout.CENTER);pnlTop.add(lblTongCongNo,BorderLayout.SOUTH);add(pnlTop,BorderLayout.NORTH);
        JPanel pnlLists=new JPanel(new GridLayout(1,2,10,0));JScrollPane scrPatients=BillingUi.scroll(tblBenhNhan),scrInvoices=BillingUi.scroll(tblHoaDon);scrPatients.setBorder(BorderFactory.createTitledBorder("Tổng hợp bệnh nhân còn nợ"));scrInvoices.setBorder(BorderFactory.createTitledBorder("Hóa đơn còn nợ của bệnh nhân đang chọn"));pnlLists.add(scrPatients);pnlLists.add(scrInvoices);pnlLists.setPreferredSize(new Dimension(1150,230));
        JPanel pnlBody=new JPanel(new BorderLayout(0,10));pnlBody.add(pnlLists,BorderLayout.NORTH);pnlBody.add(pnlDetails,BorderLayout.CENTER);add(pnlBody,BorderLayout.CENTER);add(lblTrangThai,BorderLayout.SOUTH);
        controller=new DebtController(this,session,connections);busy(false);
        BillingUi.style(this);
    }
    @Override public void addNotify(){super.addNotify();refreshData();}
    @Override public void refreshData(){controller.load();}
    public void setPaymentNavigation(HoaDonPanel.PaymentNavigation navigation){paymentNavigation=navigation;}
    public void paySelected(){String id=selectedInvoice();if(id==null){lblTrangThai.setText("Chọn hóa đơn còn nợ của bệnh nhân để thu.");return;}if(!session.isActive()||!mayCollect){lblTrangThai.setText("Không có quyền thu nợ.");return;}if(paymentNavigation==null){lblTrangThai.setText("Mở trang này từ menu Tổng kết để chuyển sang Thanh toán.");return;}paymentNavigation.open(id);}
    public void showPatients(List<PatientDebt> values){
        patients=List.of();invoices=List.of();patientModel.setRowCount(0);invoiceModel.setRowCount(0);pnlDetails.clear();patients=List.copyOf(values);BigDecimal total=BigDecimal.ZERO;
        for(PatientDebt p:patients){patientModel.addRow(new Object[]{p.patientId(),p.name(),p.invoices().size(),p.total(),p.collected(),p.debt()});total=total.add(p.debt());}lblTongCongNo.setText("Tổng còn phải thu theo bộ lọc: "+BillingUi.vnd(total)+" · "+patients.size()+" bệnh nhân");
    }
    public void selectPatient(){invoices=List.of();invoiceModel.setRowCount(0);pnlDetails.clear();int row=tblBenhNhan.getSelectedRow();if(row<0)return;row=tblBenhNhan.convertRowIndexToModel(row);if(row>=patients.size())return;invoices=patients.get(row).invoices();for(HoaDon h:invoices)invoiceModel.addRow(new Object[]{h.maHoaDon(),h.ngayLap(),h.tongTien(),h.daThu(),h.conNo(),h.trangThai()});}
    public String selectedInvoice(){int row=tblHoaDon.getSelectedRow();if(row<0)return null;row=tblHoaDon.convertRowIndexToModel(row);return row<invoices.size()?invoices.get(row).maHoaDon():null;}
    public void showDetail(ChiTietBilling detail){pnlDetails.showInvoice(detail);}
    public void busy(boolean busy){for(Component control:new Component[]{txtTuKhoa,txtTuNgay,txtDenNgay,btnTim,btnTaiLai,tblBenhNhan,tblHoaDon})control.setEnabled(!busy);btnThuNo.setEnabled(!busy&&mayCollect);}
    public void expired(){showPatients(List.of());busy(true);lblTrangThai.setText("Phiên đã đăng xuất.");}
}
