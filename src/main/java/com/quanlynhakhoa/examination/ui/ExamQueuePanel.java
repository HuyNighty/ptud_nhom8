package com.quanlynhakhoa.examination.ui;

import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.examination.ExamQueueDao;
import com.quanlynhakhoa.examination.ExamQueueRow;
import com.quanlynhakhoa.examination.ExamQueueService;
import com.quanlynhakhoa.features.RefreshableFeature;
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
public final class ExamQueuePanel extends JPanel implements RefreshableFeature {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color MUTED = new Color(99, 115, 134);
    private static final DateTimeFormatter DAY = DateTimeFormatter
            .ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DesktopSession session;
    private final ExamQueueService service;
    private final JTextField txtDay = new JTextField(DAY.format(LocalDate.now()), 11);
    private final JTextField txtKeyword = new JTextField(20);
    private final JButton btnRefresh = new JButton("Tải hàng chờ");
    private final JLabel lblStatus = new JLabel("Chưa tải dữ liệu");
    private final JLabel lblDetails = new JLabel("Chọn phiếu khám để xem thông tin.");
    private final Model model = new Model();
    private final JTable table = new JTable(model);
    private List<ExamQueueRow> source = List.of();
    private boolean truncated;
    private boolean loading;
    private boolean reloadPending;

    public ExamQueuePanel(DesktopSession session) {
        this(session, new ExamQueueService(session, TeamDevConnection::open), true);
    }

    public ExamQueuePanel(DesktopSession session, ExamQueueService service, boolean autoLoad) {
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
        JLabel scope = new JLabel("INT4-A · Chỉ xem phiếu đã tiếp nhận · Chưa hỗ trợ bắt đầu khám");
        scope.setForeground(MUTED);
        heading.add(scope, BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 9));
        filters.setBackground(Color.WHITE);
        filters.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 231, 242)),
                new EmptyBorder(8, 10, 8, 10)));
        filters.add(new JLabel("Ngày tiếp nhận (dd/MM/yyyy):"));
        filters.add(txtDay);
        filters.add(new JLabel("Mã / tên bệnh nhân:"));
        filters.add(txtKeyword);
        filters.add(btnRefresh);
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
        lblStatus.setForeground(MUTED);
        lblDetails.setForeground(NAVY);
        footer.add(lblDetails, BorderLayout.NORTH);
        footer.add(lblStatus, BorderLayout.SOUTH);
        body.add(footer, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        btnRefresh.addActionListener(e -> reload());
        txtDay.addActionListener(e -> reload());
        txtKeyword.getDocument().addDocumentListener(new DocumentListener() {
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
            day = LocalDate.parse(txtDay.getText().trim(), DAY);
        } catch (DateTimeParseException invalid) {
            lblStatus.setText("Ngày không hợp lệ; nhập dd/MM/yyyy.");
            return;
        }
        loading = true;
        btnRefresh.setEnabled(false);
        txtDay.setEnabled(false);
        lblStatus.setText("Đang tải hàng chờ từ SQL Server...");
        new SwingWorker<ExamQueueDao.Result, Void>() {
            @Override protected ExamQueueDao.Result doInBackground() throws Exception {
                return service.load(day);
            }
            @Override protected void done() {
                loading = false;
                txtDay.setEnabled(session.isActive());
                btnRefresh.setEnabled(session.isActive());
                if (!session.isActive()) {
                    reloadPending = false;
                    clear("Phiên đăng nhập đã kết thúc.");
                    return;
                }
                try {
                    ExamQueueDao.Result result = get();
                    if (!reloadPending) {
                        source = result.rows();
                        truncated = result.truncated();
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
        String keyword = txtKeyword.getText().trim().toLowerCase(Locale.ROOT);
        List<ExamQueueRow> filtered = source.stream().filter(row -> keyword.isEmpty()
                || row.patientName().toLowerCase(Locale.ROOT).contains(keyword)
                || row.patientId().toLowerCase(Locale.ROOT).contains(keyword)
                || row.examinationId().toLowerCase(Locale.ROOT).contains(keyword)).toList();
        model.setRows(filtered);
        lblDetails.setText("Chọn phiếu khám để xem thông tin.");
        lblStatus.setText(filtered.size() + " phiếu (ngày đã chọn)"
                + (truncated ? " · Chỉ hiển thị 200 phiếu đầu, hãy chọn ngày phù hợp" : "")
                + " · Dữ liệu chỉ đọc");
    }

    private void clear(String text) {
        source = List.of();
        truncated = false;
        model.setRows(List.of());
        lblDetails.setText("—");
        lblStatus.setText(text);
        if (!session.isActive()) {
            btnRefresh.setEnabled(false);
            txtDay.setEnabled(false);
        }
    }

    private void showSelection() {
        int selected = table.getSelectedRow();
        if (selected < 0 || !session.isActive()) return;
        ExamQueueRow row = model.get(table.convertRowIndexToModel(selected));
        lblDetails.setText("Phiếu " + row.examinationId() + " · Bác sĩ "
                + Objects.toString(row.doctorName(), row.doctorId())
                + " · Lịch hẹn " + Objects.toString(row.appointmentId(), "Không có"));
    }

    private static final class Model extends AbstractTableModel {
        private static final String[] COLUMNS = {
                "Mã phiếu", "Mã bệnh nhân", "Bệnh nhân", "Bác sĩ", "Tiếp nhận lúc", "Trạng thái"
        };
        private List<ExamQueueRow> rows = List.of();
        void setRows(List<ExamQueueRow> value) {
            rows = List.copyOf(value);
            fireTableDataChanged();
        }
        ExamQueueRow get(int index) { return rows.get(index); }
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int index) { return COLUMNS[index]; }
        @Override public Object getValueAt(int row, int column) {
            ExamQueueRow item = rows.get(row);
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
