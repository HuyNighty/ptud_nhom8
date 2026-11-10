package com.quanlynhakhoa.view.hoadon;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Compatibility wrapper; the page and dialog share PaymentForm and PaymentController. */
public final class ThanhToanDialog extends JDialog {
    private final ThanhToanForm pnlForm=new ThanhToanForm();
    public ThanhToanDialog(Window owner){
        super(owner,"Thanh toán hóa đơn",ModalityType.APPLICATION_MODAL);
        pnlForm.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));setContentPane(pnlForm);setSize(1080,640);setLocationRelativeTo(owner);setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        pnlForm.onClose(new Runnable(){public void run(){dispose();}});
        addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent event){if(!pnlForm.isWorking()&&!pnlForm.hasUncertainResult())dispose();else if(pnlForm.hasUncertainResult())pnlForm.lblTrangThai.setText("Kiểm tra kết quả lần thu trước khi đóng cửa sổ.");}});
    }
    public ThanhToanForm form(){return pnlForm;}
}
