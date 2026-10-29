package com.quanlynhakhoa.account.ui;

import com.quanlynhakhoa.account.AccountDirectoryDao;
import com.quanlynhakhoa.account.AccountDirectoryRow;
import com.quanlynhakhoa.account.AccountDirectoryService;
import com.quanlynhakhoa.account.AccountCreateService;
import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.model.AccountStatus;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Arrays;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.border.EmptyBorder;

public final class AccountPanel extends JPanel {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color MUTED = new Color(99, 115, 134);
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DesktopSession session;
    private final AccountDirectoryService service;
    private final AccountCreateService creator;
    private final JTextField keyword = new JTextField(19);
    private final JComboBox<Role> roleFilter = new JComboBox<>();
    private final JComboBox<AccountStatus> statusFilter = new JComboBox<>();
    private final JButton searchButton = new JButton("Tìm kiếm");
    private final JButton refreshButton = new JButton("Làm mới");
    private final JButton createButton = new JButton("Tạo tài khoản");
    private final JLabel statusLabel = new JLabel("Chưa tải dữ liệu");
    private final JLabel detailLabel = new JLabel("Chọn một tài khoản để xem thông tin.");
    private final AccountTableModel model = new AccountTableModel();
    private final JTable table = new JTable(model);
    private boolean loading;
    private boolean creating;

    public AccountPanel(DesktopSession session) {
        this(session, new AccountDirectoryService(session, TeamDevConnection::open),
                TeamDevConnection.hasLocalConfig());
    }

    public AccountPanel(DesktopSession session, AccountDirectoryService service) {
        this(session, service, true);
    }

    private AccountPanel(DesktopSession session, AccountDirectoryService service,
                         boolean configured) {
        this.session = Objects.requireNonNull(session, "session");
        this.service = Objects.requireNonNull(service, "service");
        this.creator = new AccountCreateService(session, TeamDevConnection::open);
        setLayout(new BorderLayout(0, 16));
        setBackground(new Color(245, 248, 252));
        setBorder(new EmptyBorder(24, 26, 24, 26));

        JPanel heading = new JPanel(new BorderLayout(0, 7));
        heading.setOpaque(false);
        JLabel title = new JLabel("Quản lý tài khoản nhân viên");
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        title.setForeground(NAVY);
        JLabel description = new JLabel(
                "Danh sách và tạo tài khoản nhân viên · SQL Server development");
        description.setForeground(MUTED);
        heading.add(title, BorderLayout.NORTH);
        heading.add(description, BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout(0, 14));
        main.setOpaque(false);
        main.add(buildFilters(), BorderLayout.NORTH);

        table.setRowHeight(30);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateDetail();
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 231, 242)));
        main.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        statusLabel.setForeground(MUTED);
        detailLabel.setForeground(NAVY);
        footer.add(detailLabel, BorderLayout.NORTH);
        footer.add(statusLabel, BorderLayout.SOUTH);
        main.add(footer, BorderLayout.SOUTH);
        add(main, BorderLayout.CENTER);

        createButton.addActionListener(event -> createAccount());
        searchButton.addActionListener(event -> load());
        refreshButton.addActionListener(event -> load());
        keyword.addActionListener(event -> load());
        if (configured) {
            load();
        } else {
            searchButton.setEnabled(false);
            refreshButton.setEnabled(false);
            createButton.setEnabled(false);
            statusLabel.setText("Chưa có cấu hình SQL Server development trong worktree.");
        }
    }

    private JPanel buildFilters() {
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 8));
        filters.setBackground(Color.WHITE);
        filters.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 231, 242)),
                new EmptyBorder(7, 10, 7, 10)));
        filters.add(new JLabel("Tìm:"));
        filters.add(keyword);
        roleFilter.addItem(null);
        for (Role role : Role.values()) roleFilter.addItem(role);
        statusFilter.addItem(null);
        for (AccountStatus status : AccountStatus.values()) statusFilter.addItem(status);
        roleFilter.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setText(value == null ? "Tất cả vai trò" : roleLabel((Role) value));
                return this;
            }
        });
        statusFilter.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setText(value == null ? "Tất cả trạng thái" : statusLabel((AccountStatus) value));
                return this;
            }
        });
        filters.add(roleFilter);
        filters.add(statusFilter);
        filters.add(searchButton);
        filters.add(refreshButton);
        filters.add(createButton);
        return filters;
    }

    private void load() {
        if (loading || creating) return;
        if (!session.isActive()) {
            statusLabel.setText("Phiên đăng nhập đã kết thúc.");
            return;
        }
        String search = keyword.getText().trim();
        if (search.length() > 80) {
            statusLabel.setText("Từ khóa tối đa 80 ký tự.");
            return;
        }
        Role role = (Role) roleFilter.getSelectedItem();
        AccountStatus status = (AccountStatus) statusFilter.getSelectedItem();
        loading = true;
        searchButton.setEnabled(false);
        refreshButton.setEnabled(false);
        createButton.setEnabled(false);
        statusLabel.setText("Đang tải từ SQL Server...");
        new SwingWorker<AccountDirectoryDao.Result, Void>() {
            @Override protected AccountDirectoryDao.Result doInBackground() throws Exception {
                return service.search(search, role, status);
            }
            @Override protected void done() {
                loading = false;
                searchButton.setEnabled(true);
                refreshButton.setEnabled(true);
                createButton.setEnabled(true);
                if (!session.isActive()) {
                    model.setRows(List.of());
                    statusLabel.setText("Phiên đăng nhập đã kết thúc.");
                    return;
                }
                try {
                    AccountDirectoryDao.Result result = get();
                    model.setRows(result.rows());
                    detailLabel.setText("Chọn một tài khoản để xem thông tin.");
                    statusLabel.setText(result.rows().size() + " tài khoản"
                            + (result.truncated() ? " (giới hạn 200; hãy lọc để tìm thêm)" : "")
                            + " · Chỉ đọc danh sách; tạo tài khoản bằng biểu mẫu riêng.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("Tải dữ liệu bị gián đoạn.");
                } catch (ExecutionException | CancellationException e) {
                    model.setRows(List.of());
                    detailLabel.setText("Không có dữ liệu hiển thị.");
                    statusLabel.setText("Không thể tải tài khoản. Kiểm tra kết nối và quyền SQL.");
                }
            }
        }.execute();
    }

    private void createAccount() {
        if (loading || creating || !session.isActive()) return;

        JTextField employeeId = new JTextField(16);
        JTextField username = new JTextField(16);
        JTextField displayName = new JTextField(16);
        JComboBox<Role> newRole = new JComboBox<>();
        for (Role role : Role.values()) {
            if (role != Role.SYSTEM_ADMIN) newRole.addItem(role);
        }
        JPasswordField password = new JPasswordField(16);
        JPasswordField confirmation = new JPasswordField(16);
        JPanel fields = new JPanel(new GridLayout(0, 2, 10, 9));
        fields.add(new JLabel("Mã nhân viên đang làm việc:"));
        fields.add(employeeId);
        fields.add(new JLabel("Tên đăng nhập:"));
        fields.add(username);
        fields.add(new JLabel("Tên hiển thị:"));
        fields.add(displayName);
        fields.add(new JLabel("Vai trò:"));
        fields.add(newRole);
        fields.add(new JLabel("Mật khẩu (12–72 byte):"));
        fields.add(password);
        fields.add(new JLabel("Nhập lại mật khẩu:"));
        fields.add(confirmation);
        int choice = JOptionPane.showConfirmDialog(this, fields,
                "Tạo tài khoản nhân viên — mật khẩu do nhân viên chọn",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        char[] secret = password.getPassword();
        char[] again = confirmation.getPassword();
        try {
            if (choice != JOptionPane.OK_OPTION) return;
            if (!Arrays.equals(secret, again)) {
                JOptionPane.showMessageDialog(this, "Hai mật khẩu không khớp.",
                        "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String staff = employeeId.getText().trim();
            String login = username.getText().trim();
            String display = displayName.getText().trim();
            Role role = (Role) newRole.getSelectedItem();
            if (staff.isBlank() || login.isBlank() || display.isBlank() || secret.length == 0) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ thông tin.",
                        "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
                return;
            }
            creating = true;
            createButton.setEnabled(false);
            searchButton.setEnabled(false);
            refreshButton.setEnabled(false);
            statusLabel.setText("Đang tạo tài khoản trong SQL Server...");
            char[] workerSecret = secret.clone();
            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() throws Exception {
                    try {
                        return creator.create(staff, login, display, role, workerSecret);
                    } finally {
                        Arrays.fill(workerSecret, '\0');
                    }
                }
                @Override protected void done() {
                    creating = false;
                    createButton.setEnabled(true);
                    searchButton.setEnabled(true);
                    refreshButton.setEnabled(true);
                    if (!session.isActive()) {
                        statusLabel.setText("Phiên đăng nhập đã kết thúc.");
                        return;
                    }
                    try {
                        String id = get();
                        statusLabel.setText("Đã tạo tài khoản: " + id);
                        load();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        statusLabel.setText("Thao tác bị gián đoạn; hãy làm mới để kiểm tra.");
                    } catch (ExecutionException ex) {
                        Throwable root = ex.getCause();
                        String message = root instanceof IllegalArgumentException
                                ? root.getMessage()
                                : "Không thể tạo tài khoản. Kiểm tra dữ liệu và quyền SQL.";
                        statusLabel.setText(message);
                    } catch (CancellationException ex) {
                        statusLabel.setText("Thao tác bị gián đoạn; hãy làm mới để kiểm tra.");
                    }
                }
            }.execute();
        } finally {
            Arrays.fill(secret, '\0');
            Arrays.fill(again, '\0');
            password.setText("");
            confirmation.setText("");
        }
    }

    private void updateDetail() {
        int selected = table.getSelectedRow();
        if (selected < 0 || !session.isActive()) {
            detailLabel.setText("Chọn một tài khoản để xem thông tin.");
            return;
        }
        AccountDirectoryRow row = model.get(table.convertRowIndexToModel(selected));
        detailLabel.setText("Mã: " + row.accountId() + "  |  Nhân viên: "
                + show(row.employeeId()) + "  |  Đổi mật khẩu bắt buộc: "
                + (row.passwordChangeRequired() ? "Có" : "Không"));
    }

    private static String show(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String roleLabel(Role role) {
        return switch (role) {
            case SYSTEM_ADMIN -> "Quản trị hệ thống";
            case CLINIC_MANAGER -> "Quản lý phòng khám";
            case RECEPTIONIST -> "Lễ tân";
            case DOCTOR -> "Bác sĩ";
            case NURSE -> "Y tá";
            case INVENTORY_MANAGER -> "Quản lý kho";
        };
    }

    private static String statusLabel(AccountStatus status) {
        return switch (status) {
            case HOAT_DONG -> "Hoạt động";
            case BI_KHOA -> "Bị khóa";
            case VO_HIEU -> "Vô hiệu";
        };
    }

    private static final class AccountTableModel extends AbstractTableModel {
        private final String[] columns = {
                "Mã TK", "Tên đăng nhập", "Tên hiển thị", "Nhân viên",
                "Vai trò", "Trạng thái", "Đăng nhập cuối"
        };
        private List<AccountDirectoryRow> rows = new ArrayList<>();

        void setRows(List<AccountDirectoryRow> next) {
            rows = List.copyOf(next);
            fireTableDataChanged();
        }
        AccountDirectoryRow get(int row) { return rows.get(row); }
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }
        @Override public boolean isCellEditable(int row, int column) { return false; }
        @Override public Object getValueAt(int index, int column) {
            AccountDirectoryRow row = rows.get(index);
            return switch (column) {
                case 0 -> row.accountId();
                case 1 -> row.username();
                case 2 -> row.displayName();
                case 3 -> show(row.employeeName());
                case 4 -> roleLabel(row.role());
                case 5 -> statusLabel(row.status());
                case 6 -> row.lastLoginUtc() == null ? "—" :
                        row.lastLoginUtc().atOffset(ZoneOffset.UTC)
                                .atZoneSameInstant(CLINIC_ZONE).format(DATE_TIME);
                default -> "";
            };
        }
    }
}
