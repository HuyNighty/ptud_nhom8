package com.quanlynhakhoa.billing.ui;

import com.quanlynhakhoa.billing.service.CancellationService;
import java.awt.*;
import javax.swing.*;

public final class CancellationRequestDialog extends JDialog {
    public final JTextArea txaLyDo=new JTextArea(5,45);
    public final JButton btnGuiYeuCau=new JButton("Gửi yêu cầu hủy"),btnTaiLai=new JButton("Kiểm tra/Làm mới");
    public final JLabel lblHoaDon=new JLabel("Đang đọc hóa đơn..."),lblTrangThai=new JLabel("Gửi yêu cầu chưa hủy hóa đơn. Lý do bắt buộc, tối đa 500 ký tự.");
    public CancellationRequestDialog(Window owner){
        super(owner,"Yêu cầu hủy hóa đơn",ModalityType.APPLICATION_MODAL);
        JPanel pnlRoot=new JPanel(new BorderLayout(10,10));pnlRoot.setBorder(BorderFactory.createEmptyBorder(14,14,14,14));pnlRoot.add(lblHoaDon,BorderLayout.NORTH);
        txaLyDo.setLineWrap(true);txaLyDo.setWrapStyleWord(true);JScrollPane scrLyDo=new JScrollPane(txaLyDo);scrLyDo.setBorder(BorderFactory.createTitledBorder("Lý do yêu cầu hủy"));pnlRoot.add(scrLyDo,BorderLayout.CENTER);
        JPanel pnlBottom=new JPanel(new BorderLayout(0,8));JPanel pnlActions=new JPanel(new FlowLayout(FlowLayout.RIGHT));pnlActions.add(btnTaiLai);pnlActions.add(btnGuiYeuCau);pnlBottom.add(pnlActions,BorderLayout.NORTH);pnlBottom.add(lblTrangThai,BorderLayout.SOUTH);pnlRoot.add(pnlBottom,BorderLayout.SOUTH);
        setContentPane(pnlRoot);setSize(1000,330);setLocationRelativeTo(owner);setDefaultCloseOperation(DISPOSE_ON_CLOSE);btnGuiYeuCau.setEnabled(false);
    }
    public void showPreview(CancellationService.Preview preview){
        var h=preview.invoice().hoaDon();lblHoaDon.setText(h.maHoaDon()+" · "+h.maBenhNhan()+" · "+h.tenBenhNhan()+" · Tổng "+BillingUi.vnd(h.tongTien())+" · Đã thu "+BillingUi.vnd(h.daThu())+" · Còn nợ "+BillingUi.vnd(h.conNo()));
        lblTrangThai.setText(preview.pending()?"Hóa đơn đã có yêu cầu chờ duyệt, không gửi thêm.":h.daThu().signum()>0?"Có khoản thu hợp lệ: chỉ gửi yêu cầu; chưa thể duyệt hủy khi chưa có xử lý khoản thu.":"Gửi yêu cầu chưa hủy hóa đơn; chờ người khác có quyền xét duyệt.");
    }
    public void busy(boolean busy,boolean canSubmit){txaLyDo.setEnabled(!busy);btnGuiYeuCau.setEnabled(!busy&&canSubmit);btnTaiLai.setEnabled(!busy);setDefaultCloseOperation(busy?DO_NOTHING_ON_CLOSE:DISPOSE_ON_CLOSE);}
}
