package com.quanlynhakhoa.view.vaitro;

import com.quanlynhakhoa.model.vaitro.DongVaiTro;
import com.quanlynhakhoa.model.vaitro.DongQuyen;
import com.quanlynhakhoa.config.phattrien.TeamDevConnection;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.service.vaitro.PhanQuyenCoBanService;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/** Basic RBAC editor tab; keeps the legacy read-only permission matrix untouched. */
public final class PhanQuyenCoBanPanel extends JPanel {
    private final PhanQuyenCoBanService service;
    private final DesktopSession session;
    private final JTextField maVaiTro = new JTextField(18);
    private final JTextField tenVaiTro = new JTextField(22);
    private final JTextArea moTaVaiTro = new JTextArea(2, 24);
    private final JTextField maQuyen = new JTextField(18);
    private final JTextField tenQuyen = new JTextField(22);
    private final JLabel trangThai = new JLabel("Chưa kết nối V9.");
    private final JButton btnLuuVaiTro = new JButton("Lưu vai trò mới");
    private final JButton btnLuuQuyen = new JButton("Khai báo quyền (chưa kích hoạt)");
    private final JButton btnTaiLai = new JButton("Tải danh sách");
    private final JButton btnTaiLaiQuyen = new JButton("Tải quyền");
    private final EnumSet<Quyen> quyenDaChon = EnumSet.noneOf(Quyen.class);
    private final List<JCheckBox> danhSachChonQuyen = new ArrayList<>();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Mã vai trò", "Tên", "Nguồn", "Trạng thái"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final DefaultTableModel modelQuyen = new DefaultTableModel(
            new Object[]{"Mã quyền", "Tên quyền", "Tình trạng"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private boolean working;

    public PhanQuyenCoBanPanel(DesktopSession session) {
        this.session = session;
        this.service = new PhanQuyenCoBanService(session, TeamDevConnection::open);
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        boolean admin = session.isActive() && session.role() == VaiTroHeThong.SYSTEM_ADMIN;
        JLabel explanation = new JLabel(admin
                ? "Chỉ lưu catalog V9; chưa gán role mới cho tài khoản và chưa kích hoạt authorization động."
                : "Chỉ SYSTEM_ADMIN được tạo vai trò và khai báo quyền trong MVP.");
        add(explanation, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Tạo vai trò", createRoleTab());
        tabs.addTab("Khai báo quyền", createPermissionTab());
        tabs.addTab("Danh sách vai trò", createListTab());
        add(tabs, BorderLayout.CENTER);
        add(trangThai, BorderLayout.SOUTH);
        btnLuuVaiTro.addActionListener(e -> submitRole());
        btnLuuQuyen.addActionListener(e -> submitPermission());
        btnTaiLai.addActionListener(e -> loadRoles());
        btnTaiLaiQuyen.addActionListener(e -> loadPermissions());
        btnLuuVaiTro.setEnabled(admin);
        btnLuuQuyen.setEnabled(admin);
        btnTaiLai.setEnabled(admin);
        btnTaiLaiQuyen.setEnabled(admin);
    }

    private JPanel createRoleTab() {
        JPanel content = new JPanel(new BorderLayout(8, 8));
        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
        fields.add(new JLabel("Mã vai trò (VD: DENTAL_ASSISTANT)"));
        fields.add(maVaiTro);
        fields.add(new JLabel("Tên hiển thị"));
        fields.add(tenVaiTro);
        fields.add(new JLabel("Mô tả"));
        moTaVaiTro.setLineWrap(true);
        fields.add(new JScrollPane(moTaVaiTro));
        content.add(fields, BorderLayout.NORTH);
        JPanel checkGrid = new JPanel(new GridLayout(0, 2, 8, 4));
        for (Quyen permission : Quyen.values()) {
            JCheckBox check = new JCheckBox(permission.name());
            check.addActionListener(e -> {
                if (check.isSelected()) quyenDaChon.add(permission);
                else quyenDaChon.remove(permission);
            });
            danhSachChonQuyen.add(check);
            checkGrid.add(check);
        }
        JScrollPane scroll = new JScrollPane(checkGrid);
        scroll.setBorder(BorderFactory.createTitledBorder("Chọn từ 26 quyền nghiệp vụ có sẵn"));
        content.add(scroll, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnLuuVaiTro);
        content.add(buttons, BorderLayout.SOUTH);
        return content;
    }
    private JPanel createPermissionTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
        fields.add(new JLabel("Mã quyền (VD: REPORT_EXPORT)"));
        fields.add(maQuyen);
        fields.add(new JLabel("Tên quyền"));
        fields.add(tenQuyen);
        panel.add(fields, BorderLayout.NORTH);
        JTextArea warning = new JTextArea(
                "LƯU Ý: Quyền mới chỉ được khai báo vào bảng dbo.Quyen. " +
                "Chưa có tác dụng giới hạn/truy cập chức năng nào; muốn thực thi phải tích hợp mã quyền " +
                "vào service và authorization riêng. Không tự động cấp quyền mới cho ADMIN.");
        warning.setLineWrap(true);
        warning.setWrapStyleWord(true);
        warning.setEditable(false);
        warning.setOpaque(false);
        warning.setRows(5);
        JPanel center = new JPanel(new BorderLayout(4, 6));
        center.add(warning, BorderLayout.NORTH);
        center.add(new JScrollPane(new JTable(modelQuyen)), BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnTaiLaiQuyen);
        buttons.add(btnLuuQuyen);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }
    private JPanel createListTab() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnTaiLai);
        p.add(buttons, BorderLayout.SOUTH);
        return p;
    }
    private void loadPermissions() {
        if (working) return;
        working = true;
        setEnabledActions(false);
        trangThai.setText("Đang đọc danh mục quyền...");
        new SwingWorker<List<DongQuyen>, Void>() {
            @Override protected List<DongQuyen> doInBackground() throws Exception {
                return service.listPermissions();
            }
            @Override protected void done() {
                working = false;
                setEnabledActions(true);
                try {
                    List<DongQuyen> rows = get();
                    modelQuyen.setRowCount(0);
                    for (DongQuyen p : rows) modelQuyen.addRow(new Object[]{
                            p.code(), p.name(), p.builtIn() ? "Đã có trong ứng dụng" : "Chưa tích hợp nghiệp vụ"});
                    trangThai.setText("Đã tải " + rows.size() + " quyền V9.");
                } catch (Exception ex) { trangThai.setText(safeFailure(ex)); }
            }
        }.execute();
    }
    private void submitRole() {
        String code = maVaiTro.getText().trim();
        String name = tenVaiTro.getText().trim();
        String description = moTaVaiTro.getText();
        EnumSet<Quyen> snapshot = EnumSet.copyOf(quyenDaChon);
        run("Đang lưu vai trò...", () -> {
            service.createRole(code, name, description, snapshot);
            return "Đã tạo vai trò " + code + " (chưa thể gán vào tài khoản).";
        }, true);
    }
    private void submitPermission() {
        String code = maQuyen.getText().trim();
        String name = tenQuyen.getText().trim();
        run("Đang khai báo quyền...", () -> {
            service.declarePermission(code, name);
            return "Đã khai báo " + code + " (chưa có hiệu lực nghiệp vụ).";
        }, false);
    }
    private void loadRoles() {
        if (working) return;
        working = true;
        setEnabledActions(false);
        trangThai.setText("Đang đọc vai trò...");
        new SwingWorker<List<DongVaiTro>, Void>() {
            @Override protected List<DongVaiTro> doInBackground() throws Exception {
                return service.listRoles();
            }
            @Override protected void done() {
                working = false;
                setEnabledActions(true);
                try {
                    List<DongVaiTro> rows = get();
                    model.setRowCount(0);
                    for (DongVaiTro r : rows) model.addRow(new Object[]{
                            r.code(), r.name(), r.system() ? "Hệ thống" : "Tùy chỉnh",
                            r.active() ? "Đang bật" : "Đã tắt"});
                    trangThai.setText("Đã tải " + rows.size() + " vai trò V9.");
                } catch (Exception ex) { trangThai.setText(safeFailure(ex)); }
            }
        }.execute();
    }
    @FunctionalInterface private interface Operation { String run() throws Exception; }
    private void run(String starting, Operation operation, boolean refreshList) {
        if (working) return;
        working = true;
        setEnabledActions(false);
        trangThai.setText(starting);
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception { return operation.run(); }
            @Override protected void done() {
                working = false;
                setEnabledActions(true);
                try {
                    trangThai.setText(get());
                    if (refreshList) loadRoles(); else loadPermissions();
                } catch (Exception ex) { trangThai.setText(safeFailure(ex)); }
            }
        }.execute();
    }
    private void setEnabledActions(boolean enabled) {
        boolean admin = enabled && session.isActive() && session.role() == VaiTroHeThong.SYSTEM_ADMIN;
        btnLuuVaiTro.setEnabled(admin);
        btnLuuQuyen.setEnabled(admin);
        btnTaiLai.setEnabled(admin);
        btnTaiLaiQuyen.setEnabled(admin);
    }
    private static String safeFailure(Exception ex) {
        Throwable cause = ex;
        while (cause instanceof java.util.concurrent.ExecutionException && cause.getCause() != null)
            cause = cause.getCause();
        if (cause instanceof IllegalArgumentException) return "Không hợp lệ: " + cause.getMessage();
        if (cause instanceof com.quanlynhakhoa.security.AccessDeniedException)
            return "Từ chối: phiên admin chưa được xác minh.";
        return "Không thể lưu/đọc RBAC V9: kiểm tra migration, SQL grants và kết nối.";
    }
}
