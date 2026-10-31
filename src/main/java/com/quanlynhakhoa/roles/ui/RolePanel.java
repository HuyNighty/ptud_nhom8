package com.quanlynhakhoa.roles.ui;

import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.features.RefreshableFeature;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.roles.RoleOverview;
import com.quanlynhakhoa.roles.RoleOverviewService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/** Read-only role catalog and permission matrix. No role editing or SQL writes. */
public final class RolePanel extends JPanel implements RefreshableFeature {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color MUTED = new Color(99, 115, 134);
    private static final Color BORDER = new Color(220, 231, 242);
    private final DesktopSession session;
    private final RoleOverviewService service;
    private final JComboBox<Role> rolePicker = new JComboBox<>(Role.values());
    private final JButton refresh = new JButton("Làm mới");
    private final JLabel status = new JLabel("Chưa tải dữ liệu");
    private final JLabel roleHeading = new JLabel("Vai trò");
    private final JLabel roleDescription = new JLabel("—");
    private final JLabel roleStats = new JLabel("—");
    private final JTextArea grantedList = new JTextArea(8, 20);
    private final PermissionMatrixModel matrix = new PermissionMatrixModel();
    private final JTable table = new JTable(matrix);
    private List<RoleOverview> data = List.of();
    private boolean loading;
    private boolean reloadPending;

    public RolePanel(DesktopSession session) {
        this(session, new RoleOverviewService(session, TeamDevConnection::open), true);
    }

    RolePanel(DesktopSession session, RoleOverviewService service, boolean autoLoad) {
        this.session = Objects.requireNonNull(session, "session");
        this.service = Objects.requireNonNull(service, "service");
        setLayout(new BorderLayout(0, 16));
        setBackground(new Color(245, 248, 252));
        setBorder(new EmptyBorder(24, 26, 24, 26));

        JPanel heading = new JPanel(new BorderLayout(0, 6));
        heading.setOpaque(false);
        JLabel title = new JLabel("Vai trò và phân quyền");
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        title.setForeground(NAVY);
        heading.add(title, BorderLayout.NORTH);
        JLabel intro = new JLabel("6 vai trò cố định · Chính sách từ RolePermissionPolicy · Chỉ xem");
        intro.setForeground(MUTED);
        heading.add(intro, BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 9));
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(5, 9, 5, 9)));
        toolbar.add(new JLabel("Chọn vai trò:"));
        rolePicker.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                setText(value instanceof Role role ? roleText(role) : "—");
                return this;
            }
        });
        toolbar.add(rolePicker);
        toolbar.add(refresh);
        body.add(toolbar, BorderLayout.NORTH);

        JPanel details = new JPanel(new BorderLayout(0, 10));
        details.setBackground(Color.WHITE);
        details.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(16, 18, 15, 18)));
        roleHeading.setFont(new Font("SansSerif", Font.BOLD, 18));
        roleHeading.setForeground(NAVY);
        roleDescription.setForeground(MUTED);
        JPanel identity = new JPanel(new GridLayout(0, 1, 0, 7));
        identity.setOpaque(false);
        identity.add(roleHeading);
        identity.add(roleDescription);
        identity.add(roleStats);
        details.add(identity, BorderLayout.NORTH);
        grantedList.setEditable(false);
        grantedList.setBackground(Color.WHITE);
        grantedList.setLineWrap(true);
        grantedList.setWrapStyleWord(true);
        grantedList.setFont(new Font("SansSerif", Font.PLAIN, 13));
        details.add(new JScrollPane(grantedList), BorderLayout.CENTER);
        details.setPreferredSize(new java.awt.Dimension(100, 225));
        body.add(details, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 7));
        bottom.setOpaque(false);
        JLabel matrixHeading = new JLabel("Ma trận quyền thực tế (" + Permission.values().length + " quyền × " + Role.values().length + " vai trò)");
        matrixHeading.setForeground(NAVY);
        matrixHeading.setFont(new Font("SansSerif", Font.BOLD, 15));
        bottom.add(matrixHeading, BorderLayout.NORTH);
        table.setRowHeight(26);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(250);
        for (int i = 1; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(155);
            table.getColumnModel().getColumn(i).setCellRenderer(new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(JTable t, Object value,
                        boolean selected, boolean focused, int row, int column) {
                    super.getTableCellRendererComponent(t,
                            Boolean.TRUE.equals(value) ? "✓" : "—", selected, focused, row, column);
                    setHorizontalAlignment(CENTER);
                    return this;
                }
            });
        }
        JScrollPane scroller = new JScrollPane(table);
        scroller.setBorder(BorderFactory.createLineBorder(BORDER));
        scroller.setPreferredSize(new java.awt.Dimension(100, 258));
        bottom.add(scroller, BorderLayout.CENTER);
        status.setForeground(MUTED);
        bottom.add(status, BorderLayout.SOUTH);
        body.add(bottom, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        rolePicker.addActionListener(e -> showRole());
        refresh.addActionListener(e -> reload());
        setControls();
        if (autoLoad) reload();
    }

    @Override public void refreshData() {
        if (SwingUtilities.isEventDispatchThread()) reload();
        else SwingUtilities.invokeLater(this::reload);
    }

    private void reload() {
        if (!session.isActive()) {
            data = List.of();
            matrix.setRows(data);
            clearDetails();
            status.setText("Phiên đăng nhập đã kết thúc.");
            setControls();
            return;
        }
        if (loading) {
            reloadPending = true;
            return;
        }
        loading = true;
        setControls();
        status.setText("Đang tải số tài khoản theo vai trò...");
        new SwingWorker<List<RoleOverview>, Void>() {
            @Override protected List<RoleOverview> doInBackground() throws Exception {
                return service.load();
            }
            @Override protected void done() {
                loading = false;
                if (!session.isActive()) {
                    reloadPending = false;
                    data = List.of();
                    matrix.setRows(data);
                    clearDetails();
                    status.setText("Phiên đăng nhập đã kết thúc.");
                    setControls();
                    return;
                }
                try {
                    List<RoleOverview> result = get();
                    if (!reloadPending) {
                        data = result;
                        matrix.setRows(result);
                        showRole();
                        int total = result.stream().mapToInt(RoleOverview::accountCount).sum();
                        status.setText(result.size() + " vai trò · " + total
                                + " tài khoản (mọi trạng thái) · Không có thao tác sửa quyền");
                    }
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    if (!reloadPending) showLoadFailure();
                } catch (ExecutionException | CancellationException failure) {
                    if (!reloadPending) showLoadFailure();
                } finally {
                    if (reloadPending) {
                        reloadPending = false;
                        reload();
                    } else {
                        setControls();
                    }
                }
            }
        }.execute();
    }

    private void showLoadFailure() {
        data = List.of();
        matrix.setRows(data);
        clearDetails();
        status.setText("Không thể tải vai trò. Kiểm tra kết nối SQL và quyền đọc tài khoản.");
    }

    private void clearDetails() {
        roleHeading.setText("Vai trò");
        roleDescription.setText("—");
        roleStats.setText("—");
        grantedList.setText("");
    }

    private void showRole() {
        Role selected = (Role) rolePicker.getSelectedItem();
        RoleOverview overview = data.stream()
                .filter(row -> row.role() == selected).findFirst().orElse(null);
        if (overview == null) {
            clearDetails();
            return;
        }
        roleHeading.setText(roleText(overview.role()));
        roleDescription.setText(roleDescription(overview.role()));
        roleStats.setText(overview.accountCount() + " tài khoản · "
                + overview.permissions().size() + " / " + Permission.values().length + " quyền");
        StringBuilder granted = new StringBuilder("Các quyền đang được cấp:\n");
        for (Permission permission : Permission.values()) {
            if (overview.permissions().contains(permission)) {
                granted.append("• ").append(permissionText(permission)).append('\n');
            }
        }
        grantedList.setText(granted.toString());
        grantedList.setCaretPosition(0);
    }

    private void setControls() {
        refresh.setEnabled(session.isActive() && !loading);
        rolePicker.setEnabled(session.isActive() && !loading && !data.isEmpty());
    }

    private static String roleText(Role role) {
        return switch (role) {
            case RECEPTIONIST -> "Lễ tân";
            case DOCTOR -> "Bác sĩ";
            case NURSE -> "Y tá";
            case CLINIC_MANAGER -> "Quản lý phòng khám";
            case INVENTORY_MANAGER -> "Quản lý kho";
            case SYSTEM_ADMIN -> "Quản trị hệ thống";
        };
    }

    private static String roleDescription(Role role) {
        return switch (role) {
            case RECEPTIONIST -> "Tiếp nhận, lịch hẹn và nghiệp vụ lễ tân";
            case DOCTOR -> "Hồ sơ bệnh án và nghiệp vụ chuyên môn";
            case NURSE -> "Hỗ trợ điều trị và dụng cụ cấp cứu";
            case CLINIC_MANAGER -> "Điều hành nhân sự, tài khoản và hoạt động phòng khám";
            case INVENTORY_MANAGER -> "Quản lý thuốc, vật tư và kho";
            case SYSTEM_ADMIN -> "Quản trị toàn bộ ứng dụng; cấp bằng cơ chế riêng";
        };
    }

    private static String permissionText(Permission permission) {
        return switch (permission) {
            case PATIENT_READ -> "Xem bệnh nhân";
            case PATIENT_CREATE -> "Tạo bệnh nhân";
            case PATIENT_UPDATE -> "Cập nhật bệnh nhân";
            case MEDICAL_RECORD_READ -> "Xem hồ sơ bệnh án";
            case MEDICAL_RECORD_UPDATE -> "Cập nhật hồ sơ bệnh án";
            case APPOINTMENT_READ -> "Xem lịch hẹn";
            case APPOINTMENT_MANAGE -> "Quản lý lịch hẹn";
            case BILLING_READ -> "Xem hóa đơn";
            case BILLING_MANAGE -> "Quản lý hóa đơn";
            case INVENTORY_READ -> "Xem kho";
            case INVENTORY_MANAGE -> "Quản lý kho";
            case WORKFORCE_READ -> "Xem nhân viên";
            case WORKFORCE_MANAGE -> "Quản lý nhân viên";
            case ACCOUNT_MANAGE -> "Quản lý tài khoản";
            case ROLE_MANAGE -> "Xem và quản lý chính sách vai trò";
            case AUDIT_READ -> "Xem nhật ký thao tác";
            case PATIENT_CHECK_IN -> "Tiếp đón bệnh nhân";
            case REEXAM_SCHEDULE -> "Đặt lịch tái khám";
            case SUPPLY_REQUEST -> "Yêu cầu cấp phát";
            case EMERGENCY_KIT_CHECK -> "Kiểm tra hộp cấp cứu";
            case SCHEDULE_SELF_READ -> "Xem lịch làm việc cá nhân";
            case SERVICE_CATALOG_MANAGE -> "Quản lý danh mục dịch vụ";
            case INVOICE_CANCEL_REQUEST -> "Yêu cầu hủy hóa đơn";
            case INVOICE_CANCEL_APPROVE -> "Duyệt hủy hóa đơn";
        };
    }

    private static final class PermissionMatrixModel extends AbstractTableModel {
        private static final Role[] ROLES = Role.values();
        private List<RoleOverview> rows = List.of();
        void setRows(List<RoleOverview> values) {
            rows = List.copyOf(values);
            fireTableDataChanged();
        }
        @Override public int getRowCount() {
            return rows.isEmpty() ? 0 : Permission.values().length;
        }
        @Override public int getColumnCount() { return ROLES.length + 1; }
        @Override public String getColumnName(int index) {
            return index == 0 ? "Quyền nghiệp vụ" : roleText(ROLES[index - 1]);
        }
        @Override public Object getValueAt(int row, int column) {
            Permission permission = Permission.values()[row];
            if (column == 0) return permissionText(permission);
            Role role = ROLES[column - 1];
            return rows.stream().filter(item -> item.role() == role).findFirst()
                    .map(item -> item.permissions().contains(permission)).orElse(false);
        }
        @Override public Class<?> getColumnClass(int column) {
            return column == 0 ? String.class : Boolean.class;
        }
        @Override public boolean isCellEditable(int row, int column) { return false; }
    }
}
