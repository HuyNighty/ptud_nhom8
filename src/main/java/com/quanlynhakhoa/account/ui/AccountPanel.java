package com.quanlynhakhoa.account.ui;

import com.quanlynhakhoa.account.AccountDirectoryDao;
import com.quanlynhakhoa.account.AccountDirectoryRow;
import com.quanlynhakhoa.account.AccountDirectoryService;
import com.quanlynhakhoa.account.AccountCreateService;
import com.quanlynhakhoa.account.EligibleEmployee;
import com.quanlynhakhoa.account.EligibleEmployeeDao;
import com.quanlynhakhoa.account.EligibleEmployeeService;
import com.quanlynhakhoa.account.AccountStateService;
import com.quanlynhakhoa.model.LockSource;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.Permission;
import com.quanlynhakhoa.security.RolePermissionPolicy;
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
import javax.swing.JTextArea;
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
    private final EligibleEmployeeService eligibleEmployees;
    private final AccountStateService stateService;
    private final boolean configured;
    private final AuthorizationService authorization = new AuthorizationService(new RolePermissionPolicy());
    private final JTextField txtKeyword = new JTextField(19);
    private final JComboBox<Role> cmbRoleFilter = new JComboBox<>();
    private final JComboBox<AccountStatus> cmbStatusFilter = new JComboBox<>();
    private final JButton btnSearch = new JButton("Tìm kiếm");
    private final JButton btnRefresh = new JButton("Làm mới");
    private final JButton btnCreate = new JButton("Tạo tài khoản");
    private final JButton btnLock = new JButton("Khóa tài khoản...");
    private final JButton btnUnlock = new JButton("Mở khóa");
    private final JLabel lblStatus = new JLabel("Chưa tải dữ liệu");
    private final JLabel lblDetail = new JLabel("Chọn một tài khoản để xem thông tin.");
    private final AccountTableModel model = new AccountTableModel();
    private final JTable tblAccounts = new JTable(model);
    private boolean loading;
    private boolean creating;
    private boolean reloadPending;
    private boolean changingState;
    private String stateMessage = "";

    public AccountPanel(DesktopSession session) {
        this(session, new AccountDirectoryService(session, TeamDevConnection::open),
                TeamDevConnection.hasLocalConfig());
    }

    public AccountPanel(DesktopSession session, AccountDirectoryService service) {
        this(session, service, true);
    }

    private AccountPanel(DesktopSession session, AccountDirectoryService service,
                         boolean configured) {
        this(session, service, new AccountStateService(session, TeamDevConnection::open), configured);
    }

    AccountPanel(DesktopSession session, AccountDirectoryService service, AccountStateService stateService) {
        this(session, service, stateService, true);
    }

    private AccountPanel(DesktopSession session, AccountDirectoryService service,
                         AccountStateService stateService, boolean configured) {
        this.stateService = Objects.requireNonNull(stateService, "stateService");
        this.configured = configured;
        this.session = Objects.requireNonNull(session, "session");
        this.service = Objects.requireNonNull(service, "service");
        this.creator = new AccountCreateService(session, TeamDevConnection::open);
        this.eligibleEmployees = new EligibleEmployeeService(session, TeamDevConnection::open);
        setLayout(new BorderLayout(0, 16));
        setBackground(new Color(245, 248, 252));
        setBorder(new EmptyBorder(24, 26, 24, 26));

        JPanel pnlHeading = new JPanel(new BorderLayout(0, 7));
        pnlHeading.setOpaque(false);
        JLabel lblTitle = new JLabel("Quản lý tài khoản nhân viên");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 23));
        lblTitle.setForeground(NAVY);
        JLabel lblDescription = new JLabel(
                "Danh sách, tạo và khóa/mở khóa tài khoản nhân viên");
        lblDescription.setForeground(MUTED);
        pnlHeading.add(lblTitle, BorderLayout.NORTH);
        pnlHeading.add(lblDescription, BorderLayout.SOUTH);
        add(pnlHeading, BorderLayout.NORTH);

        JPanel pnlMain = new JPanel(new BorderLayout(0, 14));
        pnlMain.setOpaque(false);
        pnlMain.add(buildFilters(), BorderLayout.NORTH);

        tblAccounts.setRowHeight(30);
        tblAccounts.setAutoCreateRowSorter(true);
        tblAccounts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblAccounts.setFillsViewportHeight(true);
        tblAccounts.getTableHeader().setReorderingAllowed(false);
        tblAccounts.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateDetail();
        });
        JScrollPane scrAccounts = new JScrollPane(tblAccounts);
        scrAccounts.setBorder(BorderFactory.createLineBorder(new Color(220, 231, 242)));
        pnlMain.add(scrAccounts, BorderLayout.CENTER);

        JPanel pnlFooter = new JPanel(new BorderLayout(0, 8));
        pnlFooter.setOpaque(false);
        lblStatus.setForeground(MUTED);
        lblDetail.setForeground(NAVY);
        pnlFooter.add(lblDetail, BorderLayout.NORTH);
        JPanel pnlStateActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
        pnlStateActions.setOpaque(false);
        pnlStateActions.add(btnLock);
        pnlStateActions.add(btnUnlock);
        JLabel lblLockScope = new JLabel("Khóa chỉ chặn đăng nhập mới; phiên đang mở không bị thu hồi.");
        lblLockScope.setForeground(MUTED);
        pnlStateActions.add(lblLockScope);
        pnlFooter.add(pnlStateActions, BorderLayout.CENTER);
        pnlFooter.add(lblStatus, BorderLayout.SOUTH);
        pnlMain.add(pnlFooter, BorderLayout.SOUTH);
        add(pnlMain, BorderLayout.CENTER);

        btnCreate.addActionListener(event -> createAccount());
        btnLock.addActionListener(event -> requestStateChange(true));
        btnUnlock.addActionListener(event -> requestStateChange(false));
        btnSearch.addActionListener(event -> load());
        btnRefresh.addActionListener(event -> load());
        txtKeyword.addActionListener(event -> load());
        // Applying either filter is an immediate search, even during an in-flight load.
        cmbRoleFilter.addActionListener(event -> load());
        cmbStatusFilter.addActionListener(event -> load());
        updateControls();
        if (configured) {
            load();
        } else {
            btnSearch.setEnabled(false);
            btnRefresh.setEnabled(false);
            btnCreate.setEnabled(false);
            lblStatus.setText("Chưa có cấu hình SQL Server development trong worktree.");
        }
    }

    private JPanel buildFilters() {
        JPanel pnlFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 8));
        pnlFilters.setBackground(Color.WHITE);
        pnlFilters.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 231, 242)),
                new EmptyBorder(7, 10, 7, 10)));
        pnlFilters.add(new JLabel("Tìm:"));
        pnlFilters.add(txtKeyword);
        cmbRoleFilter.addItem(null);
        for (Role role : Role.values()) cmbRoleFilter.addItem(role);
        cmbStatusFilter.addItem(null);
        for (AccountStatus status : AccountStatus.values()) cmbStatusFilter.addItem(status);
        cmbRoleFilter.setRenderer(roleRenderer());
        cmbStatusFilter.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> lstValues, Object value, int index,
                    boolean selected, boolean focus) {
                super.getListCellRendererComponent(lstValues, value, index, selected, focus);
                setText(value == null ? "Tất cả trạng thái" : statusLabel((AccountStatus) value));
                return this;
            }
        });
        pnlFilters.add(cmbRoleFilter);
        pnlFilters.add(cmbStatusFilter);
        pnlFilters.add(btnSearch);
        pnlFilters.add(btnRefresh);
        pnlFilters.add(btnCreate);
        return pnlFilters;
    }

    private static DefaultListCellRenderer roleRenderer() {
        return new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> lstValues, Object value, int index,
                    boolean selected, boolean focus) {
                super.getListCellRendererComponent(lstValues, value, index, selected, focus);
                setText(value == null ? "Tất cả vai trò" : roleLabel((Role) value));
                return this;
            }
        };
    }

    private void load() {
        if (!configured) {
        reloadPending = false;
        lblStatus.setText(
            "Chưa có cấu hình SQL Server development trong worktree."
        );
        return;
    }
    
        if (loading || creating || changingState) {
            reloadPending = true; // newest filter selection wins after current operation
            return;
        }
        if (!session.isActive()) {
            lblStatus.setText("Phiên đăng nhập đã kết thúc.");
            return;
        }
        String search = txtKeyword.getText().trim();
        if (search.length() > 80) {
            lblStatus.setText("Từ khóa tối đa 80 ký tự.");
            return;
        }
        Role role = (Role) cmbRoleFilter.getSelectedItem();
        AccountStatus status = (AccountStatus) cmbStatusFilter.getSelectedItem();
        loading = true;
        updateControls();
        lblStatus.setText("Đang tải từ SQL Server...");
        new SwingWorker<AccountDirectoryDao.Result, Void>() {
            @Override protected AccountDirectoryDao.Result doInBackground() throws Exception {
                return service.search(search, role, status);
            }
            @Override protected void done() {
                loading = false;
                if (!session.isActive()) {
                    reloadPending = false;
                    model.setRows(List.of());
                    lblStatus.setText("Phiên đăng nhập đã kết thúc.");
                    updateControls();
                    return;
                }
                try {
                    AccountDirectoryDao.Result result = get();
                    // A changed filter supersedes this result; never show obsolete rows.
                    if (!reloadPending) {
                        model.setRows(result.rows());
                        lblDetail.setText("Chọn một tài khoản để xem thông tin.");
                        lblStatus.setText(result.rows().size() + " tài khoản"
                                + (result.truncated() ? " (giới hạn 200; hãy lọc để tìm thêm)" : "")
                                + (stateMessage.isEmpty() ? "" : " · " + stateMessage));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (!reloadPending) lblStatus.setText("Tải dữ liệu bị gián đoạn.");
                } catch (ExecutionException | CancellationException e) {
                    if (!reloadPending) {
                        model.setRows(List.of());
                        lblDetail.setText("Không có dữ liệu hiển thị.");
                        lblStatus.setText("Không thể tải tài khoản. Kiểm tra kết nối và quyền SQL.");
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

    private void createAccount() {
        if (!configured || loading || creating || changingState ||
                !authorization.can(session, Permission.ACCOUNT_MANAGE)) return;
        creating = true;
        updateControls();
        lblStatus.setText("Đang tìm nhân viên chưa có tài khoản...");
        new SwingWorker<EligibleEmployeeDao.Result, Void>() {
            @Override protected EligibleEmployeeDao.Result doInBackground() throws Exception {
                return eligibleEmployees.listEligible();
            }
            @Override protected void done() {
                creating = false;
                updateControls();
                if (!session.isActive()) {
                    lblStatus.setText("Phiên đăng nhập đã kết thúc.");
                    return;
                }
                try {
                    EligibleEmployeeDao.Result result = get();
                    if (result.employees().isEmpty()) {
                        lblStatus.setText("Không có nhân viên đủ điều kiện cấp tài khoản.");
                        JOptionPane.showMessageDialog(AccountPanel.this,
                                "Chưa có nhân viên đang làm việc mà chưa có tài khoản.\n"
                                + "Hãy tạo nhân viên tại Tổ chức → Danh sách nhân viên trước.",
                                "Không có nhân viên", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    if (result.truncated()) {
                        JOptionPane.showMessageDialog(AccountPanel.this,
                                "Danh sách chỉ hiển thị 200 nhân viên đầu tiên.\n"
                                + "Nếu không thấy nhân viên cần cấp tài khoản, hãy liên hệ quản trị viên.",
                                "Danh sách giới hạn", JOptionPane.INFORMATION_MESSAGE);
                    }
                    showCreateAccountDialog(result.employees());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    lblStatus.setText("Không thể tải danh sách nhân viên.");
                } catch (ExecutionException | CancellationException ex) {
                    lblStatus.setText("Không thể tải nhân viên. Kiểm tra kết nối và quyền SQL.");
                    JOptionPane.showMessageDialog(AccountPanel.this,
                            "Không thể tải danh sách nhân viên đủ điều kiện.",
                            "Không thể tạo tài khoản", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }

    private void showCreateAccountDialog(List<EligibleEmployee> employees) {
        if (!authorization.can(session, Permission.ACCOUNT_MANAGE)) return;
        JComboBox<EligibleEmployee> cmbEmployee =
                new JComboBox<>(employees.toArray(new EligibleEmployee[0]));
        cmbEmployee.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                setText(value instanceof EligibleEmployee employee
                        ? employee.displayLabel() : "Chọn nhân viên");
                return this;
            }
        });
        JTextField txtUsername = new JTextField(16);
        JTextField txtDisplayName = new JTextField(16);
        JLabel lblAssignedRole = new JLabel();
        EligibleEmployee initial = (EligibleEmployee) cmbEmployee.getSelectedItem();
        if (initial == null) return;
        txtDisplayName.setText(initial.fullName());
        lblAssignedRole.setText(roleLabel(initial.role()));
        cmbEmployee.addActionListener(event -> {
            EligibleEmployee employee = (EligibleEmployee) cmbEmployee.getSelectedItem();
            if (employee != null) {
                txtDisplayName.setText(employee.fullName());
                lblAssignedRole.setText(roleLabel(employee.role()));
            }
        });
        JPasswordField txtPassword = new JPasswordField(16);
        JPasswordField txtConfirmation = new JPasswordField(16);
        JPanel pnlFields = new JPanel(new GridLayout(0, 2, 10, 9));
        pnlFields.add(new JLabel("Nhân viên chưa có tài khoản:"));
        pnlFields.add(cmbEmployee);
        pnlFields.add(new JLabel("Tên đăng nhập:"));
        pnlFields.add(txtUsername);
        pnlFields.add(new JLabel("Tên hiển thị:"));
        pnlFields.add(txtDisplayName);
        pnlFields.add(new JLabel("Vai trò theo chức danh:"));
        pnlFields.add(lblAssignedRole);
        pnlFields.add(new JLabel("Mật khẩu (12–72 byte):"));
        pnlFields.add(txtPassword);
        pnlFields.add(new JLabel("Nhập lại mật khẩu:"));
        pnlFields.add(txtConfirmation);
        int choice = JOptionPane.showConfirmDialog(this, pnlFields,
                "Tạo tài khoản nhân viên — mật khẩu do nhân viên chọn",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        char[] secret = txtPassword.getPassword();
        char[] again = txtConfirmation.getPassword();
        try {
            if (choice != JOptionPane.OK_OPTION) return;
            if (!Arrays.equals(secret, again)) {
                JOptionPane.showMessageDialog(this, "Hai mật khẩu không khớp.",
                        "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
                return;
            }
            EligibleEmployee employee = (EligibleEmployee) cmbEmployee.getSelectedItem();
            if (employee == null) return;
            String staff = employee.employeeId();
            String login = txtUsername.getText().trim();
            String display = txtDisplayName.getText().trim();
            Role role = employee.role();
            if (staff.isBlank() || login.isBlank() || display.isBlank() || secret.length == 0) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ thông tin.",
                        "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
                return;
            }
            creating = true;
            updateControls();
            lblStatus.setText("Đang tạo tài khoản trong SQL Server...");
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
                    updateControls();
                    if (!session.isActive()) {
                        lblStatus.setText("Phiên đăng nhập đã kết thúc.");
                        return;
                    }
                    try {
                        String id = get();
                        lblStatus.setText("Đã tạo tài khoản: " + id);
                        reloadPending = false;
                        load();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        lblStatus.setText("Thao tác bị gián đoạn; hãy làm mới để kiểm tra.");
                    } catch (ExecutionException ex) {
                        String message = AccountCreateFeedback.forFailure(ex.getCause());
                        lblStatus.setText(message);
                        JOptionPane.showMessageDialog(AccountPanel.this, message,
                                "Không thể tạo tài khoản", JOptionPane.WARNING_MESSAGE);
                    } catch (CancellationException ex) {
                        lblStatus.setText("Thao tác bị gián đoạn; hãy làm mới để kiểm tra.");
                    } finally {
                        if (reloadPending && session.isActive()) {
                            reloadPending = false;
                            load();
                        }
                    }
                }
            }.execute();
        } finally {
            Arrays.fill(secret, '\0');
            Arrays.fill(again, '\0');
            txtPassword.setText("");
            txtConfirmation.setText("");
        }
    }

    private AccountDirectoryRow selectedAccount() {
        int selected = tblAccounts.getSelectedRow();
        return selected < 0 ? null : model.get(tblAccounts.convertRowIndexToModel(selected));
    }

    private void updateControls() {
        boolean ready = configured && !loading && !creating && !changingState
                && authorization.can(session, Permission.ACCOUNT_MANAGE);
        btnSearch.setEnabled(ready);
        btnRefresh.setEnabled(ready);
        btnCreate.setEnabled(ready);
        AccountDirectoryRow row = selectedAccount();
        boolean targetAllowed = ready && row != null && row.role() != Role.SYSTEM_ADMIN;
        btnLock.setEnabled(targetAllowed && row.status() == AccountStatus.HOAT_DONG
                && !session.accountId().equalsIgnoreCase(row.accountId()));
        btnUnlock.setEnabled(targetAllowed && row.status() == AccountStatus.BI_KHOA
                && row.lockSource() == LockSource.ADMIN_ACTION);
    }

    private void requestStateChange(boolean locking) {
        if (!(locking ? btnLock : btnUnlock).isEnabled()) return;
        AccountDirectoryRow target = selectedAccount();
        if (target == null) return;
        String reason = null;
        if (locking) {
            JTextArea txtReason = new JTextArea(3, 32);
            txtReason.setLineWrap(true);
            txtReason.setWrapStyleWord(true);
            JPanel fields = new JPanel(new BorderLayout(0, 8));
            fields.add(new JLabel("Lý do khóa " + target.accountId() + " (1–300 ký tự):"), BorderLayout.NORTH);
            fields.add(new JScrollPane(txtReason), BorderLayout.CENTER);
            if (JOptionPane.showConfirmDialog(this, fields, "Khóa tài khoản",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) return;
            reason = txtReason.getText().strip();
            if (reason.isBlank() || reason.length() > 300) {
                lblStatus.setText("Lý do khóa phải có từ 1 đến 300 ký tự.");
                return;
            }
        } else if (JOptionPane.showConfirmDialog(this,
                "Mở khóa tài khoản " + target.accountId() + "?", "Xác nhận mở khóa",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        applyStateChange(target, locking, reason);
    }

    private void applyStateChange(AccountDirectoryRow target, boolean locking, String reason) {
        if (loading || creating || changingState || !configured
                || !authorization.can(session, Permission.ACCOUNT_MANAGE)) return;
        changingState = true;
        stateMessage = "";
        tblAccounts.setEnabled(false);
        updateControls();
        lblStatus.setText(locking ? "Đang khóa tài khoản..." : "Đang mở khóa tài khoản...");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                if (locking) stateService.lock(target.accountId(), reason);
                else stateService.unlock(target.accountId());
                return null;
            }
            @Override protected void done() {
                changingState = false;
                tblAccounts.setEnabled(true);
                if (!session.isActive()) {
                    reloadPending = false;
                    model.setRows(List.of());
                    updateControls();
                    lblStatus.setText("Phiên đăng nhập đã kết thúc.");
                    return;
                }
                try {
                    get();
                    stateMessage = (locking ? "Đã khóa tài khoản " : "Đã mở khóa tài khoản ") + target.accountId() + ".";
                    reloadPending = true;
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    stateMessage = "Thao tác bị gián đoạn; hãy làm mới để kiểm tra trạng thái.";
                } catch (ExecutionException | CancellationException ex) {
                    stateMessage = AccountStateFeedback.forFailure(ex);
                } finally {
                    lblStatus.setText(stateMessage);
                    updateControls();
                    if (reloadPending) {
                        reloadPending = false;
                        load();
                    }
                }
            }
        }.execute();
    }

    private void updateDetail() {
        updateControls();
        int selected = tblAccounts.getSelectedRow();
        if (selected < 0 || !session.isActive()) {
            lblDetail.setText("Chọn một tài khoản để xem thông tin.");
            return;
        }
        AccountDirectoryRow row = model.get(tblAccounts.convertRowIndexToModel(selected));
        lblDetail.setText("Mã: " + row.accountId() + "  |  Nhân viên: "
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
                "Vai trò", "Trạng thái", "Nguồn khóa", "Đăng nhập cuối"
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
                case 6 -> row.lockSource() == null ? "—" : switch (row.lockSource()) {
                    case ADMIN_ACTION -> "Quản trị viên";
                    case FAILED_LOGIN -> "Sai mật khẩu";
                };
                case 7 -> row.lastLoginUtc() == null ? "—" :
                        row.lastLoginUtc().atOffset(ZoneOffset.UTC)
                                .atZoneSameInstant(CLINIC_ZONE).format(DATE_TIME);
                default -> "";
            };
        }
    }
}
