package com.quanlynhakhoa.service.hoadon;

import com.quanlynhakhoa.entity.DichVuNhaKhoa;
import com.quanlynhakhoa.nghiepvu.dichvu.ui.ServiceCatalogPanel;
import com.quanlynhakhoa.dao.hoadon.ConnectionProvider;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import javax.swing.*;
import static com.quanlynhakhoa.service.hoadon.ServiceBillingSelfTest.*;

/** Failure paths must preserve every form field without retrying or refreshing. */
public final class ServiceSaveFailureSelfTest {
    private static <T> T field(Object owner, String name, Class<T> type) throws Exception {
        Field f = owner.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return type.cast(f.get(owner));
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        for (String scenario : new String[]{"validation", "duplicate", "audit", "connection", "edit"}) {
            run(scenario);
        }
        System.out.println("PASS: save failures preserve create/edit fields, mode and filters; no automatic retry.");
    }

    private static void run(String scenario) throws Exception {
        FeatureJdbcDouble jdbc = new FeatureJdbcDouble();
        jdbc.duplicate = scenario.equals("duplicate");
        jdbc.auditFailure = scenario.equals("audit") || scenario.equals("edit");
        ConnectionProvider provider = scenario.equals("connection") ? new ConnectionProvider() {
            public Connection open() throws SQLException {
                jdbc.opens++;
                throw new SQLException("synthetic connection failure", "08001");
            }
        } : jdbc;
        ServiceCatalogPanel[] panel = new ServiceCatalogPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                panel[0] = new ServiceCatalogPanel(session(VaiTroHeThong.SYSTEM_ADMIN), provider);
                if (scenario.equals("edit")) panel[0].showForm(new DichVuNhaKhoa(
                        "KEEP1", "Original", BigDecimal.TEN, "Original", false, true));
                field(panel[0], "txtMaDichVu", JTextField.class).setText("KEEP1");
                field(panel[0], "txtTenDichVu", JTextField.class).setText("Tên đang sửa");
                field(panel[0], "txtDonGia", JTextField.class).setText(scenario.equals("validation") ? "abc" : "12345");
                field(panel[0], "txaMoTa", JTextArea.class).setText("Mô tả chưa lưu\nDòng thứ hai");
                field(panel[0], "chkLaCanLamSang", JCheckBox.class).setSelected(true);
                field(panel[0], "chkDangApDung", JCheckBox.class).setSelected(false);
                field(panel[0], "txtTuKhoa", JTextField.class).setText("old-filter");
                field(panel[0], "btnLuu", JButton.class).doClick();
            } catch (Exception failure) { throw new RuntimeException(failure); }
        });
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        boolean[] done = {false};
        while (!done[0] && System.nanoTime() < deadline) {
            SwingUtilities.invokeAndWait(() -> {
                try { done[0] = field(panel[0], "lblTrangThai", JLabel.class).getText().contains("Dữ liệu nhập vẫn được giữ nguyên"); }
                catch (Exception failure) { throw new RuntimeException(failure); }
            });
            if (!done[0]) Thread.sleep(20);
        }
        check(done[0], scenario + ": failure explained");
        SwingUtilities.invokeAndWait(() -> {
            try {
                check(field(panel[0], "txtMaDichVu", JTextField.class).getText().equals("KEEP1"), "id retained");
                check(field(panel[0], "txtTenDichVu", JTextField.class).getText().equals("Tên đang sửa"), "name retained");
                check(field(panel[0], "txtDonGia", JTextField.class).getText().equals(scenario.equals("validation") ? "abc" : "12345"), "price retained");
                check(field(panel[0], "txaMoTa", JTextArea.class).getText().equals("Mô tả chưa lưu\nDòng thứ hai"), "description retained");
                check(field(panel[0], "chkLaCanLamSang", JCheckBox.class).isSelected(), "clinical flag retained");
                check(!field(panel[0], "chkDangApDung", JCheckBox.class).isSelected(), "active flag retained");
                check(panel[0].keyword().equals("old-filter"), "filter retained");
                check(panel[0].creating() != scenario.equals("edit"), "create/edit mode retained");
                check(field(panel[0], "btnLuu", JButton.class).isEnabled(), "form unlocked");
                check(jdbc.opens == (scenario.equals("validation") ? 0 : 1), "no reload or automatic retry");
                check(jdbc.commits == 0 && jdbc.persisted.isEmpty(), "nothing committed");
            } catch (Exception failure) { throw new RuntimeException(failure); }
        });
    }
}
