package com.quanlynhakhoa.quantri.vaitro.ui;

import com.quanlynhakhoa.model.vaitro.DongVaiTro;
import com.quanlynhakhoa.model.vaitro.DongQuyen;
import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.quantri.vaitro.catalog.BasicRbacService;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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
public final class BasicRbacPanel extends JPanel {
    private final BasicRbacService service;
    private final DesktopSession session;
    private final JTextField roleCode = new JTextField(18);
    private final JTextField roleName = new JTextField(22);
    private final JTextArea roleDescription = new JTextArea(2, 24);
    private final JTextField permissionCode = new JTextField(18);
    private final JTextField permissionName = new JTextField(22);
    private final JLabel status = new JLabel("Chưa kết nối V9.");
    private final JButton saveRole = new JButton("Lưu vai trò mới");
    private final JButton savePermission = new JButton("Khai báo quyền (chưa kích hoạt)");
    private final JButton reload = new JButton("Tải danh sách");
    private final JButton reloadPermissions = new JButton("Tải quyền");
    private final EnumSet<Quyen> chosen = EnumSet.noneOf(Quyen.class);
    private final List<JCheckBox> selection = new ArrayList<>();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Mã vai trò", "Tên", "Nguồn", "Trạng thái"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final DefaultTableModel permissionModel = new DefaultTableModel(
            new Object[]{"Mã quyền", "Tên quyền", "Tình trạng"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private boolean working;

    public BasicRbacPanel(DesktopSession session) {
        this.session = session;
        this.service = new BasicRbacService(session, TeamDevConnection::open);
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
        add(status, BorderLayout.SOUTH);
        saveRole.addActionListener(e -> submitRole());
        savePermission.addActionListener(e -> submitPermission());
        reload.addActionListener(e -> loadRoles());
        reloadPermissions.addActionListener(e -> loadPermissions());
        saveRole.setEnabled(admin);
        savePermission.setEnabled(admin);
        reload.setEnabled(admin);
        reloadPermissions.setEnabled(admin);
    }

    private JPanel createRoleTab() {
        JPanel content = new JPanel(new BorderLayout(8, 8));
        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
        fields.add(new JLabel("Mã vai trò (VD: DENTAL_ASSISTANT)"));
        fields.add(roleCode);
        fields.add(new JLabel("Tên hiển thị"));
        fields.add(roleName);
        fields.add(new JLabel("Mô tả"));
        roleDescription.setLineWrap(true);
        fields.add(new JScrollPane(roleDescription));
        content.add(fields, BorderLayout.NORTH);
        JPanel checkGrid = new JPanel(new GridLayout(0, 2, 8, 4));
        for (Quyen permission : Quyen.values()) {
            JCheckBox check = new JCheckBox(permission.name());
            check.addActionListener(e -> {
                if (check.isSelected()) chosen.add(permission);
                else chosen.remove(permission);
            });
            selection.add(check);
            checkGrid.add(check);
        }
        JScrollPane scroll = new JScrollPane(checkGrid);
        scroll.setBorder(BorderFactory.createTitledBorder("Chọn từ 26 quyền nghiệp vụ có sẵn"));
        content.add(scroll, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(saveRole);
        content.add(buttons, BorderLayout.SOUTH);
        return content;
    }
    private JPanel createPermissionTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
        fields.add(new JLabel("Mã quyền (VD: REPORT_EXPORT)"));
        fields.add(permissionCode);
        fields.add(new JLabel("Tên quyền"));
        fields.add(permissionName);
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
        center.add(new JScrollPane(new JTable(permissionModel)), BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(reloadPermissions);
        buttons.add(savePermission);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }
    private JPanel createListTab() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(reload);
        p.add(buttons, BorderLayout.SOUTH);
        return p;
    }
    private void loadPermissions() {
        if (working) return;
        working = true;
        setEnabledActions(false);
        status.setText("Đang đọc danh mục quyền...");
        new SwingWorker<List<DongQuyen>, Void>() {
            @Override protected List<DongQuyen> doInBackground() throws Exception {
                return service.listPermissions();
            }
            @Override protected void done() {
                working = false;
                setEnabledActions(true);
                try {
                    List<DongQuyen> rows = get();
                    permissionModel.setRowCount(0);
                    for (DongQuyen p : rows) permissionModel.addRow(new Object[]{
                            p.code(), p.name(), p.builtIn() ? "Đã có trong ứng dụng" : "Chưa tích hợp nghiệp vụ"});
                    status.setText("Đã tải " + rows.size() + " quyền V9.");
                } catch (Exception ex) { status.setText(safeFailure(ex)); }
            }
        }.execute();
    }
    private void submitRole() {
        String code = roleCode.getText().trim();
        String name = roleName.getText().trim();
        String description = roleDescription.getText();
        EnumSet<Quyen> snapshot = EnumSet.copyOf(chosen);
        run("Đang lưu vai trò...", () -> {
            service.createRole(code, name, description, snapshot);
            return "Đã tạo vai trò " + code + " (chưa thể gán vào tài khoản).";
        }, true);
    }
    private void submitPermission() {
        String code = permissionCode.getText().trim();
        String name = permissionName.getText().trim();
        run("Đang khai báo quyền...", () -> {
            service.declarePermission(code, name);
            return "Đã khai báo " + code + " (chưa có hiệu lực nghiệp vụ).";
        }, false);
    }
    private void loadRoles() {
        if (working) return;
        working = true;
        setEnabledActions(false);
        status.setText("Đang đọc vai trò...");
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
                    status.setText("Đã tải " + rows.size() + " vai trò V9.");
                } catch (Exception ex) { status.setText(safeFailure(ex)); }
            }
        }.execute();
    }
    @FunctionalInterface private interface Operation { String run() throws Exception; }
    private void run(String starting, Operation operation, boolean refreshList) {
        if (working) return;
        working = true;
        setEnabledActions(false);
        status.setText(starting);
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception { return operation.run(); }
            @Override protected void done() {
                working = false;
                setEnabledActions(true);
                try {
                    status.setText(get());
                    if (refreshList) loadRoles(); else loadPermissions();
                } catch (Exception ex) { status.setText(safeFailure(ex)); }
            }
        }.execute();
    }
    private void setEnabledActions(boolean enabled) {
        boolean admin = enabled && session.isActive() && session.role() == VaiTroHeThong.SYSTEM_ADMIN;
        saveRole.setEnabled(admin);
        savePermission.setEnabled(admin);
        reload.setEnabled(admin);
        reloadPermissions.setEnabled(admin);
    }
    private static String safeFailure(Exception ex) {
        Throwable cause = ex;
        while (cause instanceof java.util.concurrent.ExecutionException && cause.getCause() != null)
            cause = cause.getCause();
        if (cause instanceof IllegalArgumentException) return "Không hợp lệ: " + cause.getMessage();
        if (cause instanceof com.quanlynhakhoa.hethong.phanquyen.AccessDeniedException)
            return "Từ chối: phiên admin chưa được xác minh.";
        return "Không thể lưu/đọc RBAC V9: kiểm tra migration, SQL grants và kết nối.";
    }
}
