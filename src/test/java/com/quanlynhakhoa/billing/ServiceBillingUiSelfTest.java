package com.quanlynhakhoa.billing;

import com.quanlynhakhoa.billing.ui.HoaDonPanel;
import com.quanlynhakhoa.dichvu.model.DichVu;
import com.quanlynhakhoa.dichvu.ui.DichVuPanel;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.DesktopSession;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;
import com.quanlynhakhoa.billing.ServiceBillingSelfTest.Action;
import static com.quanlynhakhoa.billing.ServiceBillingSelfTest.*;

/** Headless Swing interaction tests; does not claim visual/real SQL acceptance. */
public final class ServiceBillingUiSelfTest {
    private static <T> T field(Object owner, String name, Class<T> type) throws Exception {
        Field field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return type.cast(field.get(owner));
    }
    private static void edt(final Action action) throws Exception {
        final Throwable[] error = new Throwable[1];
        SwingUtilities.invokeAndWait(new Runnable() { public void run() { try { action.run(); } catch (Throwable failure) { error[0] = failure; } } });
        if (error[0] != null) throw new AssertionError("EDT failure", error[0]);
    }
    private static void waitFor(final JLabel lblStatus, final String text) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        final boolean[] found = new boolean[1];
        while (System.nanoTime() < deadline) {
            edt(new Action() { public void run() { found[0] = lblStatus.getText().contains(text); } });
            if (found[0]) return;
            Thread.sleep(20);
        }
        throw new AssertionError("Status did not contain " + text);
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        final FeatureJdbcDouble jdbc = new FeatureJdbcDouble();
        final DesktopSession session = session(Role.CLINIC_MANAGER);
        final DichVuPanel[] panel = new DichVuPanel[1];
        final HoaDonPanel[] billing = new HoaDonPanel[1];
        edt(new Action() { public void run() {
            panel[0] = new DichVuPanel(session, jdbc); billing[0] = new HoaDonPanel(session, jdbc);
        } });
        check(jdbc.opens == 0, "constructors must not open JDBC");
        edt(new Action() { public void run() throws Exception {
            panel[0].showRows(List.of(new DichVu("B", "B", new BigDecimal("20"), null, false, true),
                    new DichVu("A", "A", new BigDecimal("100"), null, true, false)));
            JTable tblDichVu = field(panel[0], "tblDichVu", JTable.class);
            tblDichVu.getRowSorter().toggleSortOrder(0); tblDichVu.setRowSelectionInterval(0, 0);
            check(panel[0].selected().maDichVu().equals("A"), "view to model index after sort");
            check(!field(panel[0], "txtMaDichVu", JTextField.class).isEditable(), "saved code locked");
            check(!tblDichVu.isCellEditable(0, 0), "read only table");
            check(field(panel[0], "btnApDungLai", JButton.class).isEnabled(), "reactivate stopped service");
            field(panel[0], "btnThem", JButton.class).doClick();
            check(panel[0].creating() && field(panel[0], "txtMaDichVu", JTextField.class).isEditable(), "new clears form");
        } });
        jdbc.release = new CountDownLatch(1);
        edt(new Action() { public void run() throws Exception {
            field(panel[0], "txtMaDichVu", JTextField.class).setText("TST1");
            field(panel[0], "txtTenDichVu", JTextField.class).setText("Synthetic");
            field(panel[0], "txtDonGia", JTextField.class).setText("1.500.000");
            JButton btnLuu = field(panel[0], "btnLuu", JButton.class);
            btnLuu.doClick(); btnLuu.doClick();
            check(!btnLuu.isEnabled(), "save disabled during write");
        } });
        check(jdbc.opened.await(5, TimeUnit.SECONDS), "background task started");
        check(jdbc.opens == 1 && !jdbc.onEdt, "one JDBC invocation, off EDT");
        jdbc.release.countDown();
        waitFor(field(panel[0], "lblTrangThai", JLabel.class), "Đã lưu và tải lại");
        check(jdbc.persisted.size() == 1 && jdbc.audits.size() == 1, "double click saved once; cached views may commit read transactions");
        check(new BigDecimal("1500000").equals(jdbc.persisted.getFirst().get(2)),"Vietnamese grouped input persists the exact price");
        final FeatureJdbcDouble savedJdbc = new FeatureJdbcDouble();
        final DichVuPanel[] savedPanel = new DichVuPanel[1];
        edt(new Action() { public void run() throws Exception {
            savedPanel[0] = new DichVuPanel(session, new com.quanlynhakhoa.billing.dao.ConnectionProvider() {
                int calls;
                public java.sql.Connection open() throws java.sql.SQLException {
                    if (++calls > 1) throw new java.sql.SQLException("reload", "08001", 0);
                    return savedJdbc.open();
                }
            });
            field(savedPanel[0], "txtMaDichVu", JTextField.class).setText("TST2");
            field(savedPanel[0], "txtTenDichVu", JTextField.class).setText("Synthetic");
            field(savedPanel[0], "txtDonGia", JTextField.class).setText("0");
            field(savedPanel[0], "btnLuu", JButton.class).doClick();
        } });
        waitFor(field(savedPanel[0], "lblTrangThai", JLabel.class), "Đã lưu thành công, nhưng tải lại thất bại");
        edt(new Action() { public void run() {
            check(savedJdbc.commits == 1 && !savedPanel[0].creating(), "save success survives reload failure without duplicate insert mode");
        } });
        // Force a load/connection failure and verify honest UI, without a modal blocking the test.
        final DichVuPanel[] errorPanel = new DichVuPanel[1];
        edt(new Action() { public void run() throws Exception {
            errorPanel[0] = new DichVuPanel(session, new com.quanlynhakhoa.billing.dao.ConnectionProvider() {
                public java.sql.Connection open() throws java.sql.SQLException { throw new java.sql.SQLException("synthetic", "08001", 0); }
            });
            field(errorPanel[0], "btnTaiLai", JButton.class).doClick();
        } });
        waitFor(field(errorPanel[0], "lblTrangThai", JLabel.class), "Kiểm tra kết nối");
        final com.quanlynhakhoa.billing.ui.CancellationPanel[] cancellation=new com.quanlynhakhoa.billing.ui.CancellationPanel[1];
        final java.util.concurrent.atomic.AtomicInteger retries=new java.util.concurrent.atomic.AtomicInteger();
        edt(new Action(){public void run(){
            cancellation[0]=new com.quanlynhakhoa.billing.ui.CancellationPanel(session,new com.quanlynhakhoa.billing.dao.ConnectionProvider(){public java.sql.Connection open()throws java.sql.SQLException{check(!SwingUtilities.isEventDispatchThread(),"SQL retry outside EDT");retries.incrementAndGet();throw new java.sql.SQLException("SENSITIVE SQL PARAMETERS", "S0005",229);}});
            cancellation[0].refreshData();
        }});
        waitFor(cancellation[0].lblTrangThai,"quyền");
        edt(new Action(){public void run(){check(cancellation[0].btnThuLai.isVisible()&&!cancellation[0].btnDuyet.isEnabled()&&!cancellation[0].btnTuChoi.isEnabled(),"Permission failure exposes retry and disables decisions");check(!cancellation[0].lblTrangThai.getText().contains("S0005")&&!cancellation[0].lblTrangThai.getText().contains("229"),"Technical SQL details absent from UI");cancellation[0].btnThuLai.doClick();}});
        waitFor(cancellation[0].lblTrangThai,"quyền");check(retries.get()==2,"Explicit retry performs one additional read");
        final FeatureJdbcDouble delayed = new FeatureJdbcDouble(); delayed.release = new CountDownLatch(1);
        final DesktopSession expiredSession = session(Role.CLINIC_MANAGER);
        final DichVuPanel[] expiredPanel = new DichVuPanel[1];
        edt(new Action() { public void run() throws Exception {
            expiredPanel[0] = new DichVuPanel(expiredSession, delayed);
            expiredPanel[0].showForm(new DichVu("TST1", "Synthetic", BigDecimal.ONE, null, false, true));
            field(expiredPanel[0], "btnTaiLai", JButton.class).doClick();
        } });
        check(delayed.opened.await(5, TimeUnit.SECONDS), "delayed query started");
        expiredSession.logout(); delayed.release.countDown();
        waitFor(field(expiredPanel[0], "lblTrangThai", JLabel.class), "đăng xuất");
        edt(new Action() { public void run() throws Exception {
            check(field(expiredPanel[0], "txtTenDichVu", JTextField.class).getText().isEmpty(), "sensitive form cleared on expiry");
            check(!field(expiredPanel[0], "btnLuu", JButton.class).isEnabled(), "expired form disabled");
            billing[0].showRows(List.of());
            check(field(billing[0], "tblHoaDon", JTable.class).getRowCount() == 0, "empty billing stays empty");
            field(billing[0], "txtTuNgay", JTextField.class).setText("30/02/2026");
            field(billing[0], "btnTim", JButton.class).doClick();
            check(field(billing[0], "lblTrangThai", JLabel.class).getText().contains("dd/MM/yyyy"), "invalid date shown");
        } });
        System.out.println("PASS: ServiceBillingUiSelfTest (headless interactions, no real DB/UI visual acceptance).");
    }
}
