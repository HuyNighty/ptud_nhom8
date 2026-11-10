package com.quanlynhakhoa.view.nhanvien;

import com.quanlynhakhoa.model.nhanvien.KetQuaDanhSachNhanVien;
import com.quanlynhakhoa.config.phattrien.TeamDevConnection;
import com.quanlynhakhoa.config.tinhnang.RefreshableFeature;
import com.quanlynhakhoa.enums.TrangThaiNhanVien;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.model.nhanvien.DongDanhSachNhanVien;
import com.quanlynhakhoa.service.nhanvien.DanhSachNhanVienService;
import com.quanlynhakhoa.service.nhanvien.TaoNhanVienService;
import com.quanlynhakhoa.model.nhanvien.YeuCauTaoNhanVien;
import com.quanlynhakhoa.model.nhanvien.NhanChucDanh;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;

/** Workforce directory and scoped staff creation; account creation remains separate. */
public final class NhanVienPanel extends JPanel implements RefreshableFeature {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color MUTED = new Color(99, 115, 134);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DesktopSession session;
    private final DanhSachNhanVienService service;
    private final TaoNhanVienService boTao;
    private final AuthorizationService authorization = new AuthorizationService(new RolePermissionPolicy());
    private final JTextField txtTuKhoa = new JTextField(20);
    private final JComboBox<TrangThaiNhanVien> cmbStatus = new JComboBox<>();
    private final JButton btnTimKiem = new JButton("Tìm kiếm");
    private final JButton btnLamMoi = new JButton("Làm mới");
    private final JButton btnTao = new JButton("Thêm nhân viên");
    private final JLabel lblTrangThai = new JLabel("Chưa tải dữ liệu");
    private final JLabel lblChiTiet = new JLabel("Chọn một nhân viên để xem thông tin.");
    private final EmployeeTableModel tableModel = new EmployeeTableModel();
    private final JTable table = new JTable(tableModel);
    private boolean loading;
    private boolean creating;
    private boolean reloadPending;

    public NhanVienPanel(DesktopSession session) {
        this(session, new DanhSachNhanVienService(session, TeamDevConnection::open), true);
    }

    NhanVienPanel(DesktopSession session, DanhSachNhanVienService service, boolean autoLoad) {
        this.session = Objects.requireNonNull(session, "session");
        this.service = Objects.requireNonNull(service, "service");
        this.boTao = new TaoNhanVienService(session, TeamDevConnection::open);
        setLayout(new BorderLayout(0, 16));
        setBackground(new Color(245, 248, 252));
        setBorder(new EmptyBorder(24, 26, 24, 26));

        JPanel heading = new JPanel(new BorderLayout(0, 7));
        heading.setOpaque(false);
        JLabel title = new JLabel("Danh sách nhân viên");
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        title.setForeground(NAVY);
        heading.add(title, BorderLayout.NORTH);
        add(heading, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 8));
        filters.setBackground(Color.WHITE);
        filters.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 231, 242)),
                new EmptyBorder(7, 10, 7, 10)));
        filters.add(new JLabel("Mã / tên nhân viên:"));
        filters.add(txtTuKhoa);
        cmbStatus.addItem(null);
        for (TrangThaiNhanVien status : TrangThaiNhanVien.values()) cmbStatus.addItem(status);
        cmbStatus.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                setText(value == null ? "Tất cả trạng thái" : statusText((TrangThaiNhanVien) value));
                return this;
            }
        });
        filters.add(cmbStatus);
        filters.add(btnTimKiem);
        filters.add(btnLamMoi);
        filters.add(btnTao);
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
        lblChiTiet.setForeground(NAVY);
        lblTrangThai.setForeground(MUTED);
        footer.add(lblChiTiet, BorderLayout.NORTH);
        footer.add(lblTrangThai, BorderLayout.SOUTH);
        body.add(footer, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        btnTao.addActionListener(event -> createEmployee());
        btnTimKiem.addActionListener(event -> load());
        btnLamMoi.addActionListener(event -> { txtTuKhoa.setText(""); cmbStatus.setSelectedIndex(0); load(); });
        txtTuKhoa.addActionListener(event -> load());
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
            lblTrangThai.setText("Phiên đăng nhập đã kết thúc.");
            updateControls();
            return;
        }
        if (loading || creating) {
            reloadPending = true;
            return;
        }
        String keyword = txtTuKhoa.getText().trim();
        if (keyword.length() > 80) {
            lblTrangThai.setText("Từ khóa tìm kiếm tối đa 80 ký tự.");
            return;
        }
        TrangThaiNhanVien status = (TrangThaiNhanVien) cmbStatus.getSelectedItem();
        loading = true;
        updateControls();
        lblTrangThai.setText("Đang tải nhân viên...");
        new SwingWorker<KetQuaDanhSachNhanVien, Void>() {
            @Override protected KetQuaDanhSachNhanVien doInBackground() throws Exception {
                return service.search(keyword, status);
            }
            @Override protected void done() {
                loading = false;
                if (!session.isActive()) {
                    reloadPending = false;
                    tableModel.setRows(List.of());
                    lblTrangThai.setText("Phiên đăng nhập đã kết thúc.");
                    updateControls();
                    return;
                }
                try {
                    KetQuaDanhSachNhanVien result = get();
                    if (!reloadPending) {
                        tableModel.setRows(result.rows());
                        lblChiTiet.setText("Chọn một nhân viên để xem thông tin.");
                        lblTrangThai.setText(result.rows().size() + " nhân viên"
                                + (result.truncated() ? " (giới hạn 200, hãy lọc thêm)" : ""));
                    }
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    if (!reloadPending) lblTrangThai.setText("Tải dữ liệu bị gián đoạn.");
                } catch (ExecutionException | CancellationException failure) {
                    if (!reloadPending) {
                        tableModel.setRows(List.of());
                        lblChiTiet.setText("Không có dữ liệu hiển thị.");
                        lblTrangThai.setText("Không thể tải nhân viên. Kiểm tra kết nối hoặc liên hệ quản trị viên.");
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
        boolean ready = session.isActive() && !loading && !creating;
        btnTimKiem.setEnabled(ready);
        btnLamMoi.setEnabled(ready);
        txtTuKhoa.setEnabled(ready);
        cmbStatus.setEnabled(ready);
        btnTao.setEnabled(ready && authorization.can(session, Quyen.WORKFORCE_MANAGE));
    }

    private void createEmployee() {
        if (!session.isActive() || loading || creating ||
                !authorization.can(session, Quyen.WORKFORCE_MANAGE)) return;

        JTextField txtId = new JTextField(16);
        JTextField txtName = new JTextField(24);
        JTextField txtPhone = new JTextField(16);
        JComboBox<String> cmbJob = createJobPicker();
        JTextField txtBirth = new JTextField(10);
        JComboBox<String> cmbGender = new JComboBox<>(new String[] {"", "NAM", "NU", "KHAC"});
        JTextField txtEmail = new JTextField(24);
        JTextField txtJoined = new JTextField(LocalDate.now().toString(), 10);
        JTextField txtSpecialty = new JTextField(24);

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 9));
        form.add(new JLabel("Mã nhân viên (*):")); form.add(txtId);
        form.add(new JLabel("Họ tên (*):")); form.add(txtName);
        form.add(new JLabel("Số điện thoại (*):")); form.add(txtPhone);
        form.add(new JLabel("Chức danh (*):")); form.add(cmbJob);
        form.add(new JLabel("Ngày sinh (yyyy-MM-dd):")); form.add(com.quanlynhakhoa.view.hethong.DateInput.wrap(txtBirth,"ngày sinh",java.time.format.DateTimeFormatter.ISO_LOCAL_DATE,LocalDate.now().minusYears(120),LocalDate.now()));
        form.add(new JLabel("Giới tính:")); form.add(cmbGender);
        form.add(new JLabel("Email:")); form.add(txtEmail);
        form.add(new JLabel("Ngày vào làm (yyyy-MM-dd):")); form.add(com.quanlynhakhoa.view.hethong.DateInput.iso(txtJoined,"ngày vào làm"));
        form.add(new JLabel("Chuyên môn:")); form.add(txtSpecialty);

        int choice = JOptionPane.showConfirmDialog(this, form, "Thêm nhân viên (chưa cấp tài khoản)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        final YeuCauTaoNhanVien request;
        try {
            request = TaoNhanVienService.validate(new YeuCauTaoNhanVien(
                    txtId.getText(), txtName.getText(), txtPhone.getText(),
                    (String) cmbJob.getSelectedItem(), parseOptionalDate(txtBirth.getText()),
                    (String) cmbGender.getSelectedItem(), txtEmail.getText(),
                    parseOptionalDate(txtJoined.getText()), txtSpecialty.getText()));
        } catch (IllegalArgumentException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(),
                    "Dữ liệu nhân viên chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
            return;
        }

        creating = true;
        updateControls();
        lblTrangThai.setText("Đang tạo nhân viên...");
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                return boTao.create(request);
            }

            @Override protected void done() {
                creating = false;
                updateControls();
                if (!session.isActive()) {
                    lblTrangThai.setText("Phiên đăng nhập đã kết thúc. Hãy tải lại ở phiên mới.");
                    return;
                }
                try {
                    String employeeId = get();
                    JOptionPane.showMessageDialog(NhanVienPanel.this,
                            "Đã tạo nhân viên " + employeeId
                                    + ".\nChuyển sang Tài khoản để cấp tài khoản cho nhân viên này.",
                            "Tạo nhân viên thành công", JOptionPane.INFORMATION_MESSAGE);
                    reloadPending = false;
                    load();
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    lblTrangThai.setText("Thao tác bị gián đoạn; hãy làm mới để xác minh.");
                } catch (CancellationException failure) {
                    lblTrangThai.setText("Thao tác bị gián đoạn; hãy làm mới để xác minh.");
                } catch (ExecutionException failure) {
                    Throwable cause = failure.getCause();
                    String message = cause instanceof IllegalArgumentException
                            ? cause.getMessage()
                            : "Không thể tạo nhân viên. Kiểm tra quyền INSERT, kết nối SQL và thử lại.";
                    lblTrangThai.setText(message);
                    JOptionPane.showMessageDialog(NhanVienPanel.this, message,
                            "Tạo nhân viên thất bại", JOptionPane.WARNING_MESSAGE);
                } finally {
                    if (reloadPending && session.isActive()) {
                        reloadPending = false;
                        load();
                    }
                }
            }
        }.execute();
    }

    private static LocalDate parseOptionalDate(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException failure) {
            throw new IllegalArgumentException("Ngày tháng phải có dạng yyyy-MM-dd.");
        }
    }

    private void showSelected() {
        int viewIndex = table.getSelectedRow();
        if (viewIndex < 0) return;
        DongDanhSachNhanVien row = tableModel.rowAt(table.convertRowIndexToModel(viewIndex));
        lblChiTiet.setText("Nhân viên " + row.employeeId() + " · " + row.fullName()
                + " · " + (row.hasAccount() ? "Tài khoản " + row.accountId() : "Chưa có tài khoản"));
    }

    private static String jobText(String job) {
        return NhanChucDanh.label(job);
    }

    static JComboBox<String> createJobPicker() {
        JComboBox<String> picker = new JComboBox<>(new String[] {"LT", "BS", "YT", "QL", "KHO"});
        picker.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index, boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                setText(NhanChucDanh.label((String) value));
                return this;
            }
        });
        return picker;
    }

    private static String statusText(TrangThaiNhanVien status) {
        return status == TrangThaiNhanVien.DANG_LAM_VIEC ? "Đang làm việc" : "Nghỉ việc";
    }

    private static String roleText(VaiTroHeThong role) {
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
        private List<DongDanhSachNhanVien> danhSach = new ArrayList<>();

        void setRows(List<DongDanhSachNhanVien> values) {
            danhSach = List.copyOf(values);
            fireTableDataChanged();
        }

        DongDanhSachNhanVien rowAt(int index) {
            return danhSach.get(index);
        }

        @Override public int getRowCount() { return danhSach.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int column) { return COLUMNS[column]; }
        @Override public Object getValueAt(int rowIndex, int columnIndex) {
            DongDanhSachNhanVien row = danhSach.get(rowIndex);
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
