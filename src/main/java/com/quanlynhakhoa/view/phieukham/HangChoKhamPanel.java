package com.quanlynhakhoa.view.phieukham;

import com.quanlynhakhoa.model.phieukham.KetQuaHangChoKham;
import com.quanlynhakhoa.config.phattrien.TeamDevConnection;
import com.quanlynhakhoa.model.phieukham.DongHangChoKham;
import com.quanlynhakhoa.service.phieukham.HangChoKhamService;
import com.quanlynhakhoa.config.tinhnang.RefreshableFeature;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;

/** M1-INT4-A: read-only exam queue; no start/complete/cancel actions. */
public final class HangChoKhamPanel extends JPanel implements RefreshableFeature {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color MUTED = new Color(99, 115, 134);
    private static final DateTimeFormatter DAY = DateTimeFormatter
            .ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DesktopSession session;
    private final HangChoKhamService service;
    private final JTextField txtNgay = new JTextField(DAY.format(LocalDate.now()), 11);
    private final JTextField txtTuKhoa = new JTextField(20);
    private final JButton btnLamMoi = new JButton("Tải hàng chờ");
    private final JLabel lblTrangThai = new JLabel("Chưa tải dữ liệu");
    private final JLabel lblChiTiet = new JLabel("Chọn phiếu khám để xem thông tin.");
    private final Model model = new Model();
    private final JTable table = new JTable(model);
    private List<DongHangChoKham> nguon = List.of();
    private boolean biGioiHan;
    private boolean loading;
    private boolean reloadPending;

    public HangChoKhamPanel(DesktopSession session) {
        this(session, new HangChoKhamService(session, TeamDevConnection::open), true);
    }

    public HangChoKhamPanel(DesktopSession session, HangChoKhamService service, boolean autoLoad) {
        this.session = Objects.requireNonNull(session, "session");
        this.service = Objects.requireNonNull(service, "service");
        setLayout(new BorderLayout(0, 16));
        setBackground(new Color(245, 248, 252));
        setBorder(new EmptyBorder(24, 26, 24, 26));

        JPanel heading = new JPanel(new BorderLayout(0, 5));
        heading.setOpaque(false);
        JLabel title = new JLabel("Hàng chờ phiếu khám");
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        title.setForeground(NAVY);
        heading.add(title, BorderLayout.NORTH);
        add(heading, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 9));
        filters.setBackground(Color.WHITE);
        filters.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 231, 242)),
                new EmptyBorder(8, 10, 8, 10)));
        filters.add(new JLabel("Ngày tiếp nhận (dd/MM/yyyy):"));
        filters.add(com.quanlynhakhoa.view.hethong.DateInput.wrap(txtNgay,"ngày tiếp nhận",DAY,LocalDate.of(1900,1,1),LocalDate.of(9999,12,31)));
        filters.add(new JLabel("Mã / tên bệnh nhân:"));
        filters.add(txtTuKhoa);
        filters.add(btnLamMoi);
        body.add(filters, BorderLayout.NORTH);

        table.setRowHeight(30);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSelection();
        });
        body.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 7));
        footer.setOpaque(false);
        lblTrangThai.setForeground(MUTED);
        lblChiTiet.setForeground(NAVY);
        footer.add(lblChiTiet, BorderLayout.NORTH);
        footer.add(lblTrangThai, BorderLayout.SOUTH);
        body.add(footer, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        btnLamMoi.addActionListener(e -> { txtTuKhoa.setText(""); reload(); });
        txtNgay.addActionListener(e -> reload());
        txtTuKhoa.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyLocalFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyLocalFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyLocalFilter(); }
        });
        if (autoLoad) reload();
    }

    @Override public void refreshData() {
        if (SwingUtilities.isEventDispatchThread()) reload();
        else SwingUtilities.invokeLater(this::reload);
    }

    private void reload() {
        if (!session.isActive()) {
            clear("Phiên đăng nhập đã kết thúc.");
            return;
        }
        if (loading) {
            reloadPending = true;
            return;
        }
        final LocalDate day;
        try {
            day = LocalDate.parse(txtNgay.getText().trim(), DAY);
        } catch (DateTimeParseException invalid) {
            lblTrangThai.setText("Ngày không hợp lệ; nhập dd/MM/yyyy.");
            return;
        }
        loading = true;
        btnLamMoi.setEnabled(false);
        txtNgay.setEnabled(false);
        lblTrangThai.setText("Đang tải hàng chờ...");
        new SwingWorker<KetQuaHangChoKham, Void>() {
            @Override protected KetQuaHangChoKham doInBackground() throws Exception {
                return service.load(day);
            }
            @Override protected void done() {
                loading = false;
                txtNgay.setEnabled(session.isActive());
                btnLamMoi.setEnabled(session.isActive());
                if (!session.isActive()) {
                    reloadPending = false;
                    clear("Phiên đăng nhập đã kết thúc.");
                    return;
                }
                try {
                    KetQuaHangChoKham result = get();
                    if (!reloadPending) {
                        nguon = result.rows();
                        biGioiHan = result.truncated();
                        applyLocalFilter();
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    if (!reloadPending) clear("Tải hàng chờ bị gián đoạn.");
                } catch (ExecutionException | CancellationException failure) {
                    if (!reloadPending) clear("Không thể tải phiếu khám; kiểm tra quyền đọc và kết nối SQL.");
                } finally {
                    if (reloadPending) {
                        reloadPending = false;
                        reload();
                    }
                }
            }
        }.execute();
    }

    private void applyLocalFilter() {
        if (!session.isActive()) {
            clear("Phiên đăng nhập đã kết thúc.");
            return;
        }
        String keyword = txtTuKhoa.getText().trim().toLowerCase(Locale.ROOT);
        List<DongHangChoKham> filtered = nguon.stream().filter(row -> keyword.isEmpty()
                || row.patientName().toLowerCase(Locale.ROOT).contains(keyword)
                || row.patientId().toLowerCase(Locale.ROOT).contains(keyword)
                || row.examinationId().toLowerCase(Locale.ROOT).contains(keyword)).toList();
        model.setRows(filtered);
        lblChiTiet.setText("Chọn phiếu khám để xem thông tin.");
        lblTrangThai.setText(filtered.size() + " phiếu (ngày đã chọn)"
                + (biGioiHan ? " · Chỉ hiển thị 200 phiếu đầu, hãy chọn ngày phù hợp" : "")
                + " · Dữ liệu chỉ đọc");
    }

    private void clear(String text) {
        nguon = List.of();
        biGioiHan = false;
        model.setRows(List.of());
        lblChiTiet.setText("—");
        lblTrangThai.setText(text);
        if (!session.isActive()) {
            btnLamMoi.setEnabled(false);
            txtNgay.setEnabled(false);
        }
    }

    private void showSelection() {
        int selected = table.getSelectedRow();
        if (selected < 0 || !session.isActive()) return;
        DongHangChoKham row = model.get(table.convertRowIndexToModel(selected));
        lblChiTiet.setText("Phiếu " + row.examinationId() + " · Bác sĩ "
                + Objects.toString(row.doctorName(), row.doctorId())
                + " · Lịch hẹn " + Objects.toString(row.appointmentId(), "Không có"));
    }

    private static final class Model extends AbstractTableModel {
        private static final String[] COLUMNS = {
                "Mã phiếu", "Mã bệnh nhân", "Bệnh nhân", "Bác sĩ", "Tiếp nhận lúc", "Trạng thái"
        };
        private List<DongHangChoKham> danhSach = List.of();
        void setRows(List<DongHangChoKham> value) {
            danhSach = List.copyOf(value);
            fireTableDataChanged();
        }
        DongHangChoKham get(int index) { return danhSach.get(index); }
        @Override public int getRowCount() { return danhSach.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int index) { return COLUMNS[index]; }
        @Override public Object getValueAt(int row, int column) {
            DongHangChoKham item = danhSach.get(row);
            return switch (column) {
                case 0 -> item.examinationId();
                case 1 -> item.patientId();
                case 2 -> item.patientName();
                case 3 -> Objects.toString(item.doctorName(), item.doctorId());
                case 4 -> DATE_TIME.format(item.arrivedAt());
                case 5 -> item.statusText();
                default -> "";
            };
        }
    }
}
