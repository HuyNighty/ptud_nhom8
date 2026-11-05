package com.quanlynhakhoa.nghiepvu.hoadon;

import com.quanlynhakhoa.nghiepvu.dichvu.ui.ServiceCatalogPanel;
import com.quanlynhakhoa.quantri.vaitro.model.Role;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import static com.quanlynhakhoa.nghiepvu.hoadon.ServiceBillingSelfTest.*;

/** A saved active service must remain visible after an unrelated, inactive-only search. */
public final class ServiceSavedVisibilitySelfTest {
    private static <T> T field(Object owner, String name, Class<T> type) throws Exception {
        Field f = owner.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return type.cast(f.get(owner));
    }

    public static void main(String[] args) throws Exception {
        boolean show = args.length == 1 && "--show".equals(args[0]);
        System.setProperty("java.awt.headless", Boolean.toString(!show));
        FeatureJdbcDouble jdbc = new FeatureJdbcDouble();
        jdbc.serviceRows = List.of(Map.of("maDichVu", "NEW1", "tenDichVu", "New service",
                "donGia", BigDecimal.TEN, "moTa", "", "laCanLamSang", false, "dangApDung", true));
        ServiceCatalogPanel[] panel = new ServiceCatalogPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new ServiceCatalogPanel(session(Role.SYSTEM_ADMIN), jdbc);
            try {
                field(panel[0], "txtTuKhoa", JTextField.class).setText("unrelated");
                field(panel[0], "cmbTrangThai", JComboBox.class).setSelectedIndex(2);
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        await(panel[0], "Điều chỉnh bộ lọc");
        SwingUtilities.invokeAndWait(() -> {
            try {
                field(panel[0], "txtMaDichVu", JTextField.class).setText("NEW1");
                field(panel[0], "txtTenDichVu", JTextField.class).setText("New service");
                field(panel[0], "txtDonGia", JTextField.class).setText("10");
                field(panel[0], "btnLuu", JButton.class).doClick();
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        await(panel[0], "Dịch vụ NEW1");
        SwingUtilities.invokeAndWait(() -> {
            check(panel[0].keyword().isEmpty() && panel[0].activeFilter() == null, "old filters cleared");
            check(panel[0].selected() != null && panel[0].selected().maDichVu().equals("NEW1"), "saved row selected");
            check(jdbc.commits == 1 && jdbc.audits.size() == 1, "one transactional save with audit");
            check(!jdbc.onEdt, "JDBC runs outside EDT");
        });
        System.out.println("PASS: saved service visible and selected despite previous keyword/status filters.");
        if (show) SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame("Demo: dịch vụ hiển thị sau khi lưu — dữ liệu mô phỏng");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setContentPane(panel[0]);
            frame.setSize(1360, 800);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void await(ServiceCatalogPanel panel, String text) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            boolean[] found = new boolean[1];
            SwingUtilities.invokeAndWait(() -> {
                try { found[0] = field(panel, "lblTrangThai", JLabel.class).getText().contains(text); }
                catch (Exception e) { throw new RuntimeException(e); }
            });
            if (found[0]) return;
            Thread.sleep(20);
        }
        throw new AssertionError("Missing status: " + text + "; actual=" + field(panel, "lblTrangThai", JLabel.class).getText());
    }
}
