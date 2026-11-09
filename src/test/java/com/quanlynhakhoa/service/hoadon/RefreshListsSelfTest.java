package com.quanlynhakhoa.service.hoadon;

import com.quanlynhakhoa.view.hoadon.HuyHoaDonPanel;
import com.quanlynhakhoa.view.hoadon.HoaDonPanel;
import com.quanlynhakhoa.view.hoadon.CongNoPanel;
import com.quanlynhakhoa.view.dichvu.DanhMucDichVuPanel;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.lang.reflect.Field;
import javax.swing.*;
import static com.quanlynhakhoa.service.hoadon.ServiceBillingSelfTest.*;

/** Refresh must clear even invalid dates rather than retrying an invalid filter. */
public final class RefreshListsSelfTest {
    private static Object field(Object owner, String name) throws Exception {
        Field f=owner.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(owner);
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless","true");
        SwingUtilities.invokeAndWait(new Runnable(){public void run(){try {
            var session=session(VaiTroHeThong.CLINIC_MANAGER);
            var jdbc=new FeatureJdbcDouble();
            var invoice=new HoaDonPanel(session,jdbc);
            ((JTextField)field(invoice,"txtTuKhoa")).setText("HD-OLD");
            ((JTextField)field(invoice,"txtTuNgay")).setText("invalid");
            ((JButton)field(invoice,"btnTaiLai")).doClick();
            check(invoice.keyword().isEmpty()&&invoice.fromText().isEmpty()&&invoice.toText().isEmpty()&&invoice.statusFilter()==null,"invoice refresh clears filters");
            var debt=new CongNoPanel(session,jdbc);
            debt.txtTuKhoa.setText("old");debt.txtTuNgay.setText("invalid");debt.txtDenNgay.setText("invalid");
            debt.btnTaiLai.doClick();
            check(debt.txtTuKhoa.getText().isEmpty()&&debt.txtTuNgay.getText().isEmpty()&&debt.txtDenNgay.getText().isEmpty(),"debt refresh clears filters");
            var cancellation=new HuyHoaDonPanel(session,jdbc);
            cancellation.txtTuKhoa.setText("old");cancellation.txtTuNgay.setText("invalid");cancellation.txtDenNgay.setText("invalid");
            cancellation.btnTaiLai.doClick();
            check(cancellation.txtTuKhoa.getText().isEmpty()&&cancellation.txtTuNgay.getText().isEmpty()&&cancellation.txtDenNgay.getText().isEmpty()&&cancellation.cmbTrangThai.getSelectedIndex()==0,"cancellation refresh clears filters");
            var services=new DanhMucDichVuPanel(session,jdbc);
            ((JTextField)field(services,"txtTuKhoa")).setText("old");
            ((JButton)field(services,"btnTaiLai")).doClick();
            check(services.keyword().isEmpty()&&services.activeFilter()==null,"catalog refresh clears filters");
        }catch(Exception e){throw new RuntimeException(e);}}});
        // Drain pending SwingWorker callbacks before exiting.
        Thread.sleep(500);
        SwingUtilities.invokeAndWait(new Runnable(){public void run(){}});
        System.out.println("PASS: refresh clears keyword/date/status filters (headless Swing, synthetic JDBC).");
    }
}
