package com.quanlynhakhoa.billing.ui;

import com.quanlynhakhoa.billing.model.ChiTietBilling;
import java.awt.*;
import javax.swing.*;

/** Extracted collection form shared by the page and existing modal callers. */
public final class PaymentForm extends JPanel {
    public final JTextField txtSoTien=new JTextField(),txtTienMat=new JTextField("0"),txtChuyenKhoan=new JTextField("0"),txtKhachDua=new JTextField("0"),txtMaGiaoDich=new JTextField();
    public final JCheckBox chkDaNhanChuyenKhoan=new JCheckBox("Đã kiểm tra và nhận đủ phần chuyển khoản");
    public final JButton btnThuTien=new JButton("Xác nhận thu"),btnThuHet=new JButton("Điền toàn bộ còn nợ"),btnKiemTra=new JButton("Kiểm tra kết quả lần thu");
    public final JLabel lblTrangThai=new JLabel("VND nguyên, không dấu phân cách. Chuyển khoản cần xác nhận nhận tiền thực tế.");
    private final InvoiceHistoryPanel pnlHistory=new InvoiceHistoryPanel();
    private Runnable closeAction;private boolean busy,uncertain;private Runnable stateListener;
    public PaymentForm(){
        setLayout(new BorderLayout(10,10));add(pnlHistory,BorderLayout.CENTER);
        JPanel pnlFields=new JPanel(new GridLayout(0,2,8,6));
        pnlFields.add(new JLabel("Số tiền thu"));pnlFields.add(txtSoTien);pnlFields.add(new JLabel("Phần tiền mặt"));pnlFields.add(txtTienMat);
        pnlFields.add(new JLabel("Phần chuyển khoản"));pnlFields.add(txtChuyenKhoan);pnlFields.add(new JLabel("Tiền khách đưa (tiền mặt)"));pnlFields.add(txtKhachDua);
        pnlFields.add(new JLabel("Mã giao dịch chuyển khoản"));pnlFields.add(txtMaGiaoDich);pnlFields.add(chkDaNhanChuyenKhoan);pnlFields.add(btnThuHet);pnlFields.add(btnKiemTra);pnlFields.add(btnThuTien);
        JPanel pnlBottom=new JPanel(new BorderLayout(0,8));pnlBottom.add(pnlFields,BorderLayout.CENTER);pnlBottom.add(lblTrangThai,BorderLayout.SOUTH);add(pnlBottom,BorderLayout.SOUTH);busy(false,false,false);BillingUi.style(this);
    }
    public void showInvoice(ChiTietBilling detail){pnlHistory.showInvoice(detail);}
    public void onClose(Runnable callback){closeAction=callback;}
    public void onStateChange(Runnable callback){stateListener=callback;}
    public boolean finish(){if(closeAction==null)return false;closeAction.run();return true;}
    public void closeView(){if(closeAction!=null)closeAction.run();else{pnlHistory.clear();clearAmounts();busy(false,false,true);lblTrangThai.setText("Phiên đã đăng xuất.");}}
    public boolean isWorking(){return busy;}
    public boolean hasUncertainResult(){return uncertain;}
    public void clearAmounts(){txtSoTien.setText("");txtTienMat.setText("0");txtChuyenKhoan.setText("0");txtKhachDua.setText("0");txtMaGiaoDich.setText("");chkDaNhanChuyenKhoan.setSelected(false);}
    public void busy(boolean working,boolean mayCollect,boolean uncertainResult){
        busy=working;uncertain=uncertainResult;
        for(Component control:new Component[]{txtSoTien,txtTienMat,txtChuyenKhoan,txtKhachDua,txtMaGiaoDich,chkDaNhanChuyenKhoan,btnThuHet})control.setEnabled(!working&&!uncertainResult&&mayCollect);
        btnThuTien.setEnabled(!working&&!uncertainResult&&mayCollect);btnKiemTra.setEnabled(!working&&uncertainResult);if(stateListener!=null)stateListener.run();
    }
}
