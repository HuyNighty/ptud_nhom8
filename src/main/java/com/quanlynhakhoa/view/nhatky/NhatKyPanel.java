package com.quanlynhakhoa.view.nhatky;

import com.quanlynhakhoa.model.nhatky.KetQuaNhatKy;
import com.quanlynhakhoa.model.nhatky.BanGhiNhatKy;
import com.quanlynhakhoa.model.nhatky.BoLocNhatKy;
import com.quanlynhakhoa.service.nhatky.NhatKyService;
import com.quanlynhakhoa.config.phattrien.TeamDevConnection;
import com.quanlynhakhoa.config.tinhnang.RefreshableFeature;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;

/** Read-only Audit screen. Never edits, deletes, exports or copies raw journal content. */
public final class NhatKyPanel extends JPanel implements RefreshableFeature {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final DesktopSession session;
    private final NhatKyService service;
    private final JTextField tuNgay = new JTextField(10);
    private final JTextField denNgay = new JTextField(10);
    private final JTextField tuKhoa = new JTextField(20);
    private final JComboBox<String> ketQua = new JComboBox<>(new String[]{
            "Tất cả kết quả", "Thành công", "Bị từ chối", "Thất bại"});
    private final JButton btnTimKiem = new JButton("Tìm kiếm");
    private final JButton btnLamMoi = new JButton("Làm mới");
    private final JLabel trangThai = new JLabel("Chưa tải nhật ký");
    private final AuditTableModel model = new AuditTableModel();
    private final JTable table = new JTable(model);
    private boolean loading;
    private boolean reloadPending;

    public NhatKyPanel(DesktopSession session) {
        this(session, new NhatKyService(session, TeamDevConnection::open), true);
    }

    public NhatKyPanel(DesktopSession session, NhatKyService service, boolean autoLoad) {
        this.session = java.util.Objects.requireNonNull(session, "session");
        new AuthorizationService(new RolePermissionPolicy()).require(session, Quyen.AUDIT_READ);
        this.service = java.util.Objects.requireNonNull(service, "service");
        setLayout(new BorderLayout(0, 14));
        setBackground(new Color(245, 248, 252));
        setBorder(new EmptyBorder(24, 26, 24, 26));

        JLabel heading = new JLabel("Nhật ký thao tác");
        heading.setFont(new Font("SansSerif", Font.BOLD, 23));
        heading.setForeground(NAVY);
        JLabel hint = new JLabel("Chỉ đọc · Dữ liệu thời gian hiển thị theo giờ Việt Nam · Tối đa 200 dòng/lần");
        hint.setForeground(new Color(95, 109, 128));
        JPanel header = new JPanel(new BorderLayout(0, 7));
        header.setOpaque(false);
        header.add(heading, BorderLayout.NORTH);
        header.add(hint, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 9));
        controls.setBackground(Color.WHITE);
        controls.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 231, 242)), new EmptyBorder(7, 10, 7, 10)));
        controls.add(new JLabel("Từ ngày (yyyy-MM-dd):"));
        controls.add(com.quanlynhakhoa.view.hethong.DateInput.iso(tuNgay,"ngày bắt đầu"));
        controls.add(new JLabel("Đến ngày:"));
        controls.add(com.quanlynhakhoa.view.hethong.DateInput.iso(denNgay,"ngày kết thúc"));
        controls.add(new JLabel("Tìm:"));
        controls.add(tuKhoa);
        controls.add(ketQua);
        controls.add(btnTimKiem);
        controls.add(btnLamMoi);

        table.setRowHeight(29);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 231, 242)));
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(controls, BorderLayout.NORTH);
        body.add(scroll, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
        trangThai.setForeground(new Color(95, 109, 128));
        add(trangThai, BorderLayout.SOUTH);

        btnTimKiem.addActionListener(event -> load());
        btnLamMoi.addActionListener(event -> { tuKhoa.setText(""); tuNgay.setText(""); denNgay.setText(""); ketQua.setSelectedIndex(0); load(); });
        tuKhoa.addActionListener(event -> load());
        if (autoLoad) load();
    }

    @Override public void refreshData() {
        if (SwingUtilities.isEventDispatchThread()) load();
        else SwingUtilities.invokeLater(this::load);
    }

    private void load() {
        if (!session.isActive()) {
            model.setRows(List.of());
            trangThai.setText("Phiên đăng nhập đã kết thúc.");
            updateControls(false);
            return;
        }
        if (loading) {
            reloadPending = true;
            return;
        }
        final BoLocNhatKy filter;
        try {
            LocalDate start = parse(tuNgay.getText());
            LocalDate finish = parse(denNgay.getText());
            String selected = switch (ketQua.getSelectedIndex()) {
                case 1 -> "SUCCESS";
                case 2 -> "DENIED";
                case 3 -> "FAILED";
                default -> null;
            };
            filter = new BoLocNhatKy(start, finish, tuKhoa.getText(), selected);
        } catch (IllegalArgumentException invalid) {
            trangThai.setText("Bộ lọc không hợp lệ. Dùng yyyy-MM-dd và tối đa 80 ký tự tìm kiếm.");
            return;
        }
        loading = true;
        updateControls(true);
        trangThai.setText("Đang tải nhật ký...");
        new SwingWorker<KetQuaNhatKy, Void>() {
            @Override protected KetQuaNhatKy doInBackground() throws Exception {
                return service.search(filter);
            }

            @Override protected void done() {
                loading = false;
                updateControls(false);
                try {
                    KetQuaNhatKy result = get();
                    if (!session.isActive()) {
                        model.setRows(List.of());
                        trangThai.setText("Phiên đăng nhập đã kết thúc.");
                        return;
                    }
                    model.setRows(result.rows());
                    trangThai.setText("Đã tải " + result.rows().size() + " dòng"
                            + (result.truncated() ? " · còn dữ liệu, hãy thu hẹp bộ lọc" : ""));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    model.setRows(List.of());
                    trangThai.setText("Đã gián đoạn tải nhật ký.");
                } catch (ExecutionException | CancellationException failure) {
                    model.setRows(List.of());
                    trangThai.setText("Không thể tải nhật ký. Kiểm tra quyền AUDIT_READ, SQL SELECT và kết nối.");
                } finally {
                    if (reloadPending && session.isActive()) {
                        reloadPending = false;
                        load();
                    }
                }
            }
        }.execute();
    }

    private static LocalDate parse(String value) {
        String normalized = value.trim();
        return normalized.isEmpty() ? null : LocalDate.parse(normalized);
    }

    private void updateControls(boolean busy) {
        tuNgay.setEnabled(!busy && session.isActive());
        denNgay.setEnabled(!busy && session.isActive());
        tuKhoa.setEnabled(!busy && session.isActive());
        ketQua.setEnabled(!busy && session.isActive());
        btnTimKiem.setEnabled(!busy && session.isActive());
        btnLamMoi.setEnabled(!busy && session.isActive());
    }

    private static final class AuditTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
                "Thời gian (VN)", "Mã nhật ký", "Tác nhân", "Tài khoản", "Chức năng",
                "Hành động", "Kết quả", "Đối tượng", "Mã đối tượng"};
        private List<BanGhiNhatKy> danhSach = List.of();
        void setRows(List<BanGhiNhatKy> values) {
            danhSach = List.copyOf(values);
            fireTableDataChanged();
        }
        @Override public int getRowCount() { return danhSach.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int column) { return COLUMNS[column]; }
        @Override public Object getValueAt(int row, int column) {
            BanGhiNhatKy entry = danhSach.get(row);
            return switch (column) {
                case 0 -> DATE_TIME.format(entry.utcTime().atZone(ZoneOffset.UTC)
                        .withZoneSameInstant(VIETNAM));
                case 1 -> entry.id();
                case 2 -> entry.actorType();
                case 3 -> entry.accountId();
                case 4 -> entry.function();
                case 5 -> entry.action();
                case 6 -> entry.result();
                case 7 -> entry.targetType();
                case 8 -> entry.targetId();
                default -> "";
            };
        }
    }
}
