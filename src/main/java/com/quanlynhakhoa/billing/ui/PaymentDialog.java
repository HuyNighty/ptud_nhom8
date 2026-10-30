package com.quanlynhakhoa.billing.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Compatibility wrapper; the page and dialog share PaymentForm and PaymentController. */
public final class PaymentDialog extends JDialog {
    private final PaymentForm pnlForm=new PaymentForm();
    public PaymentDialog(Window owner){
        super(owner,"Thanh toán hóa đơn",ModalityType.APPLICATION_MODAL);
        pnlForm.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));setContentPane(pnlForm);setSize(1080,640);setLocationRelativeTo(owner);setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        pnlForm.onClose(new Runnable(){public void run(){dispose();}});
        addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent event){if(!pnlForm.isWorking())dispose();}});
    }
    public PaymentForm form(){return pnlForm;}
}
