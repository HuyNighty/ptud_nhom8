package com.quanlynhakhoa.workforce.ui;

import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.features.RefreshableFeature;
import com.quanlynhakhoa.model.EmployeeStatus;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.workforce.EmployeeDirectoryDao;
import com.quanlynhakhoa.workforce.EmployeeDirectoryRow;
import com.quanlynhakhoa.workforce.EmployeeDirectoryService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;

/** Workforce list only. Creating/editing staff is explicitly deferred to CORE-W1B. */
public final class EmployeePanel extends JPanel implements RefreshableFeature {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color MUTED = new Color(99, 115, 134);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DesktopSession session;
    private final EmployeeDirectoryService service;
    private final JTextField txtKeyword = new JTextField(20);
    private final JComboBox<EmployeeStatus> cmbStatus = new JComboBox<>();
    private final JButton btnSearch = new JButton("Tìm kiếm");
    private final JButton btnRefresh = new JButton("Làm mới");
    private final JLabel lblStatus = new JLabel("Chưa tải dữ liệu");
    private final JLabel lblDetail = new JLabel("Chọn một nhân viên để xem thông tin.");
    private final EmployeeTableModel tableModel = new EmployeeTableModel();
    private final JTable table = new JTable(tableModel);
    private boolean loading;
    private boolean reloadPending;

    public EmployeePanel(DesktopSession session) {
        this(session, new EmployeeDirectoryService(session, TeamDevConnection::open), true);
    }

    EmployeePanel(DesktopSession session, EmployeeDirectoryService service, boolean autoLoad) {
        this.session = Objects.requireNonNull(session, "session");
        this.service = Objects.requireNonNull(service, "service");
        setLayout(new BorderLayout(0, 16));
        setBackground(new Color(245, 248, 252));
        setBorder(new EmptyBorder(24, 26, 24, 26));

        JPanel heading = new JPanel(new BorderLayout(0, 7));
        heading.setOpaque(false);
        JLabel title = new JLabel("Danh sách nhân viên");
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        title.setForeground(NAVY);
        heading.add(title, BorderLayout.NORTH);
        JLabel description = new JLabel("Thông tin nhân sự và tài khoản đã liên kết · Chỉ xem");
        description.setForeground(MUTED);
        heading.add(description, BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 8));
        filters.setBackground(Color.WHITE);
        filters.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 231, 242)),
                new EmptyBorder(7, 10, 7, 10)));
        filters.add(new JLabel("Mã / tên nhân viên:"));
        filters.add(txtKeyword);
        cmbStatus.addItem(null);
        for (EmployeeStatus status : EmployeeStatus.values()) cmbStatus.addItem(status);
        cmbStatus.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                setText(value == null ? "Tất cả trạng thái" : statusText((EmployeeStatus) value));
                return this;
            }
        });
        filters.add(cmbStatus);
        filters.add(btnSearch);
        filters.add(btnRefresh);
        body.add(filters, BorderLayout.NORTH);

        table.setRowHeight(30);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) showSelected();
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 231, 242)));
        body.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 7));
        footer.setOpaque(false);
        lblDetail.setForeground(NAVY);
        lblStatus.setForeground(MUTED);
        footer.add(lblDetail, BorderLayout.NORTH);
        footer.add(lblStatus, BorderLayout.SOUTH);
        body.add(footer, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        btnSearch.addActionListener(event -> load());
        btnRefresh.addActionListener(event -> load());
        txtKeyword.addActionListener(event -> load());
        cmbStatus.addActionListener(event -> load());
        if (autoLoad) load();
    }

    @Override public void refreshData() {
        if (SwingUtilities.isEventDispatchThread()) {
            load();
        } else {
            SwingUtilities.invokeLater(this::load);
        }
    }

    private void load() {
        if (!session.isActive()) {
            tableModel.setRows(List.of());
            lblStatus.setText("Phiên đăng nhập đã kết thúc.");
            updateControls();
            return;
        }
        if (loading) {
            reloadPending = true;
            return;
        }
        String keyword = txtKeyword.getText().trim();
        if (keyword.length() > 80) {
            lblStatus.setText("Từ khóa tìm kiếm tối đa 80 ký tự.");
            return;
        }
        EmployeeStatus status = (EmployeeStatus) cmbStatus.getSelectedItem();
        loading = true;
        updateControls();
        lblStatus.setText("Đang tải nhân viên từ SQL Server...");
        new SwingWorker<EmployeeDirectoryDao.Result, Void>() {
            @Override protected EmployeeDirectoryDao.Result doInBackground() throws Exception {
                return service.search(keyword, status);
            }
            @Override protected void done() {
                loading = false;
                if (!session.isActive()) {
                    reloadPending = false;
                    tableModel.setRows(List.of());
                    lblStatus.setText("Phiên đăng nhập đã kết thúc.");
                    updateControls();
                    return;
                }
                try {
                    EmployeeDirectoryDao.Result result = get();
                    if (!reloadPending) {
                        tableModel.setRows(result.rows());
                        lblDetail.setText("Chọn một nhân viên để xem thông tin.");
                        lblStatus.setText(result.rows().size() + " nhân viên"
                                + (result.truncated() ? " (giới hạn 200, hãy lọc thêm)" : "")
                                + " · Chức năng tạo nhân viên sẽ có ở W1B.");
                    }
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    if (!reloadPending) lblStatus.setText("Tải dữ liệu bị gián đoạn.");
                } catch (ExecutionException | CancellationException failure) {
                    if (!reloadPending) {
                        tableModel.setRows(List.of());
                        lblDetail.setText("Không có dữ liệu hiển thị.");
                        lblStatus.setText("Không thể tải nhân viên. Kiểm tra kết nối và quyền SQL.");
                    }
                } finally {
                    if (reloadPending) {
                        reloadPending = false;
                        load();
                    } else {
                        updateControls();
                    }
                }
            }
        }.execute();
    }

    private void updateControls() {
        boolean canSearch = session.isActive() && !loading;
        btnSearch.setEnabled(canSearch);
        btnRefresh.setEnabled(canSearch);
        txtKeyword.setEnabled(canSearch);
        cmbStatus.setEnabled(canSearch);
    }

    private void showSelected() {
        int viewIndex = table.getSelectedRow();
        if (viewIndex < 0) return;
        EmployeeDirectoryRow row = tableModel.rowAt(table.convertRowIndexToModel(viewIndex));
        lblDetail.setText("Nhân viên " + row.employeeId() + " · " + row.fullName()
                + " · " + (row.hasAccount() ? "Tài khoản " + row.accountId() : "Chưa có tài khoản"));
    }

    private static String jobText(String job) {
        if (job == null) return "—";
        return switch (job) {
            case "AD" -> "Quản trị hệ thống";
            case "LT" -> "Lễ tân";
            case "BS" -> "Bác sĩ";
            case "YT" -> "Y tá";
            case "QL" -> "Quản lý phòng khám";
            case "KHO" -> "Quản lý kho";
            default -> job;
        };
    }

    private static String statusText(EmployeeStatus status) {
        return status == EmployeeStatus.DANG_LAM_VIEC ? "Đang làm việc" : "Nghỉ việc";
    }

    private static String roleText(Role role) {
        if (role == null) return "—";
        return switch (role) {
            case SYSTEM_ADMIN -> "Quản trị hệ thống";
            case RECEPTIONIST -> "Lễ tân";
            case DOCTOR -> "Bác sĩ";
            case NURSE -> "Y tá";
            case CLINIC_MANAGER -> "Quản lý phòng khám";
            case INVENTORY_MANAGER -> "Quản lý kho";
        };
    }

    private static final class EmployeeTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
                "Mã NV", "Họ tên", "Chức danh", "Trạng thái", "Ngày vào làm", "Tài khoản", "Vai trò"
        };
        private List<EmployeeDirectoryRow> rows = new ArrayList<>();

        void setRows(List<EmployeeDirectoryRow> values) {
            rows = List.copyOf(values);
            fireTableDataChanged();
        }

        EmployeeDirectoryRow rowAt(int index) {
            return rows.get(index);
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int column) { return COLUMNS[column]; }
        @Override public Object getValueAt(int rowIndex, int columnIndex) {
            EmployeeDirectoryRow row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.employeeId();
                case 1 -> row.fullName();
                case 2 -> jobText(row.jobTitle());
                case 3 -> statusText(row.status());
                case 4 -> row.joinedAt() == null ? "—" : DATE.format(row.joinedAt());
                case 5 -> row.hasAccount() ? row.accountId() : "Chưa có";
                case 6 -> roleText(row.accountRole());
                default -> "";
            };
        }
    }
}
