package com.quanlynhakhoa.nghiepvu.hoadon;

import com.quanlynhakhoa.nghiepvu.hoadon.ui.PaymentDialog;
import java.awt.event.WindowEvent;
import javax.swing.SwingUtilities;

/** Native window lifecycle test; no visible window and no database connection. */
public final class PaymentDialogRecoverySelfTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        SwingUtilities.invokeAndWait(new Runnable(){public void run(){
            PaymentDialog dialog=new PaymentDialog(null);
            try{
                dialog.addNotify();
                dialog.form().busy(true,false,true);
                dialog.dispatchEvent(new WindowEvent(dialog,WindowEvent.WINDOW_CLOSING));
                check(dialog.isDisplayable(),"cannot close while a write is running");
                dialog.form().busy(false,false,true);
                dialog.dispatchEvent(new WindowEvent(dialog,WindowEvent.WINDOW_CLOSING));
                check(dialog.isDisplayable(),"cannot close an unresolved attempt after the worker finishes");
                dialog.form().busy(false,true,false);
                dialog.dispatchEvent(new WindowEvent(dialog,WindowEvent.WINDOW_CLOSING));
                check(!dialog.isDisplayable(),"normal idle dialog can close after reconciliation");
            }finally{dialog.dispose();}
        }});
        System.out.println("PASS: payment dialog blocks closing during writes and uncertain outcomes; closes normally after reconciliation.");
    }
}
