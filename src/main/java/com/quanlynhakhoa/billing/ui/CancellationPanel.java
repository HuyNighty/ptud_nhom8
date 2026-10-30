package com.quanlynhakhoa.billing.ui;

import com.quanlynhakhoa.billing.controller.CancellationController;
import com.quanlynhakhoa.billing.dao.ConnectionProvider;
import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.billing.service.CancellationService;
import com.quanlynhakhoa.features.RefreshableFeature;
import com.quanlynhakhoa.security.*;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class CancellationPanel extends JPanel implements RefreshableFeature {
    public final JTextField txtTuKhoa=new JTextField(14),txtTuNgay=new JTextField(10),txtDenNgay=new JTextField(10);
    public final JComboBox<Object> cmbTrangThai=new JComboBox<>();
    public final JButton btnTim=new JButton("Tìm"),btnTaiLai=new JButton("Tải lại"),btnDuyet=new JButton("Duyệt hủy"),btnTuChoi=new JButton("Từ chối");
    public final JTextArea txaThongTin=new JTextArea(4,50),txaLyDoDuyet=new JTextArea(3,45);
    public final JLabel lblTrangThai=new JLabel("Chọn một yêu cầu để kiểm tra hóa đơn và lịch sử thu trước khi xét duyệt.");
    private final DefaultTableModel requestModel=BillingUi.model("Mã yêu cầu","Hóa đơn","Bệnh nhân","Tổng tiền","Đã thu","Người gửi","Thời điểm gửi","Trạng thái","Lý do yêu cầu");
    private final DefaultTableModel lineModel=BillingUi.model("Loại","Mã hàng","Số lượng","Giá chốt","Thành tiền");
    private final DefaultTableModel paymentModel=BillingUi.model("Mã thu","Thời gian","Số tiền","Phương thức","Tham chiếu","Trạng thái");
    public final JTable tblYeuCau=BillingUi.table(requestModel);
    private final CancellationController controller;private List<YeuCauHuyHoaDon> rows=List.of();private boolean loaded;
    public CancellationPanel(DesktopSession session,ConnectionProvider connections){
        new AuthorizationService(new RolePermissionPolicy()).require(session,Permission.INVOICE_CANCEL_APPROVE);
        setLayout(new BorderLayout(10,10));setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        JLabel lblTitle=new JLabel("DUYỆT HỦY HÓA ĐƠN");lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD,22f));
        JPanel pnlTop=new JPanel(new BorderLayout(0,8));pnlTop.add(lblTitle,BorderLayout.NORTH);
        cmbTrangThai.addItem("Tất cả");for(YeuCauHuyHoaDon.TrangThai status:YeuCauHuyHoaDon.TrangThai.values())cmbTrangThai.addItem(status);
        JPanel pnlFilter=new JPanel(new FlowLayout(FlowLayout.LEFT));pnlFilter.add(new JLabel("Yêu cầu/HĐ/BN/người gửi"));pnlFilter.add(txtTuKhoa);pnlFilter.add(new JLabel("Từ yyyy-MM-dd"));pnlFilter.add(txtTuNgay);pnlFilter.add(new JLabel("Đến"));pnlFilter.add(txtDenNgay);pnlFilter.add(cmbTrangThai);pnlFilter.add(btnTim);pnlFilter.add(btnTaiLai);pnlTop.add(pnlFilter,BorderLayout.SOUTH);add(pnlTop,BorderLayout.NORTH);
        JScrollPane scrRequests=BillingUi.scroll(tblYeuCau);scrRequests.setPreferredSize(new Dimension(1150,220));
        JTabbedPane tabDetails=new JTabbedPane();tabDetails.addTab("Chi tiết hóa đơn",BillingUi.scroll(BillingUi.table(lineModel)));tabDetails.addTab("Lịch sử thanh toán",BillingUi.scroll(BillingUi.table(paymentModel)));tabDetails.setPreferredSize(new Dimension(1150,170));
        JPanel pnlCenter=new JPanel(new BorderLayout(0,8));pnlCenter.add(scrRequests,BorderLayout.NORTH);pnlCenter.add(tabDetails,BorderLayout.CENTER);add(pnlCenter,BorderLayout.CENTER);
        txaThongTin.setEditable(false);txaThongTin.setLineWrap(true);txaThongTin.setWrapStyleWord(true);txaLyDoDuyet.setLineWrap(true);txaLyDoDuyet.setWrapStyleWord(true);
        JPanel pnlTexts=new JPanel(new GridLayout(1,2,8,0));JScrollPane scrInfo=new JScrollPane(txaThongTin),scrReason=new JScrollPane(txaLyDoDuyet);scrInfo.setBorder(BorderFactory.createTitledBorder("Lý do, thông tin yêu cầu và kết quả xử lý"));scrReason.setBorder(BorderFactory.createTitledBorder("Ý kiến xét duyệt (bắt buộc khi từ chối, tối đa 500 ký tự)"));pnlTexts.add(scrInfo);pnlTexts.add(scrReason);
        JPanel pnlActions=new JPanel(new FlowLayout(FlowLayout.RIGHT));pnlActions.add(btnTuChoi);pnlActions.add(btnDuyet);
        JPanel pnlBottom=new JPanel(new BorderLayout(0,8));pnlBottom.add(pnlTexts,BorderLayout.NORTH);pnlBottom.add(lblTrangThai,BorderLayout.CENTER);pnlBottom.add(pnlActions,BorderLayout.SOUTH);add(pnlBottom,BorderLayout.SOUTH);
        controller=new CancellationController(this,session,new CancellationService(session,connections));buttons(false,false);
    }
    @Override public void addNotify(){super.addNotify();if(!loaded){loaded=true;SwingUtilities.invokeLater(new Runnable(){public void run(){refreshData();}});}}
    @Override public void refreshData(){controller.load();}
    public String selectedId(){int index=tblYeuCau.getSelectedRow();if(index<0)return null;index=tblYeuCau.convertRowIndexToModel(index);return index<rows.size()?rows.get(index).maYeuCau():null;}
    public void showRows(List<YeuCauHuyHoaDon> values){rows=List.of();requestModel.setRowCount(0);rows=List.copyOf(values);for(var y:rows)requestModel.addRow(new Object[]{y.maYeuCau(),y.maHoaDon(),y.maBenhNhan()+" · "+y.tenBenhNhan(),y.tongTien(),y.daThu(),y.maTaiKhoanTao(),y.thoiGianTao(),y.trangThai(),y.lyDo()});clearDetail();}
    public void clearDetail(){lineModel.setRowCount(0);paymentModel.setRowCount(0);txaThongTin.setText("");txaLyDoDuyet.setText("");buttons(false,false);}
    public void showDetail(CancellationService.Detail detail){
        clearDetail();var y=detail.request();var h=detail.invoice().hoaDon();
        txaThongTin.setText("Yêu cầu "+y.maYeuCau()+" · Hóa đơn "+h.maHoaDon()+" · "+h.trangThai()+"\nTổng: "+BillingUi.vnd(h.tongTien())+" · Đã thu: "+BillingUi.vnd(h.daThu())+" · Còn nợ: "+BillingUi.vnd(h.conNo())+"\nNgười gửi: "+y.maTaiKhoanTao()+" · "+BillingUi.time(y.thoiGianTao())+"\nLý do: "+y.lyDo()+"\nKết quả: "+y.trangThai()+" · Người xử lý: "+y.maTaiKhoanDuyet()+" · "+BillingUi.time(y.thoiGianDuyet())+"\nÝ kiến: "+(y.lyDoDuyet()==null?"":y.lyDoDuyet()));
        for(var line:detail.invoice().chiTiet())lineModel.addRow(new Object[]{line.loaiHang(),line.maHang(),line.soLuong(),line.donGiaApDung(),line.thanhTien()});
        for(var payment:detail.invoice().thanhToan())paymentModel.addRow(new Object[]{payment.maThanhToan(),payment.thoiGian(),payment.soTien(),payment.phuongThuc(),payment.maGiaoDich(),payment.trangThai()});
    }
    public void buttons(boolean approve,boolean reject){btnDuyet.setEnabled(approve);btnTuChoi.setEnabled(reject);}
    public void busy(boolean busy){for(Component control:new Component[]{txtTuKhoa,txtTuNgay,txtDenNgay,cmbTrangThai,btnTim,btnTaiLai,tblYeuCau,txaLyDoDuyet})control.setEnabled(!busy);if(busy)buttons(false,false);}
    public void expired(){showRows(List.of());busy(true);lblTrangThai.setText("Phiên đã đăng xuất.");}
}
