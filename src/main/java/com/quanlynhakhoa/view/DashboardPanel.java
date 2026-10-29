package com.quanlynhakhoa.view;

import com.quanlynhakhoa.dashboard.DashboardService;
import com.quanlynhakhoa.dashboard.DashboardService.Summary;
import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

public final class DashboardPanel extends JPanel {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color MUTED = new Color(97, 115, 137);
    private static final Color BORDER = new Color(225, 233, 241);
    private static final Color BACKGROUND = new Color(245, 248, 252);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");

    private final DesktopSession session;
    private final DashboardService service;
    private final JLabel[] values = new JLabel[4];
    private final JLabel[] descriptions = new JLabel[4];
    private final JLabel status = label("Chưa tải dữ liệu", 12, MUTED, false);
    private final JButton refreshButton = new JButton("Làm mới");
    private boolean loading;

    public DashboardPanel(DesktopSession session) {
        this(session, new DashboardService(session, TeamDevConnection::open),
                TeamDevConnection.hasLocalConfig());
    }

    public DashboardPanel(DesktopSession session, DashboardService service) {
        this(session, service, true);
    }

    private DashboardPanel(DesktopSession session, DashboardService service, boolean configured) {
        this.session = java.util.Objects.requireNonNull(session, "session");
        this.service = java.util.Objects.requireNonNull(service, "service");
        setLayout(new BorderLayout(0, 22));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(22, 24, 22, 24));
        add(introduction(), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(metrics());
        body.add(Box.createVerticalStrut(18));
        body.add(details());
        body.add(Box.createVerticalStrut(18));
        body.add(footer());
        add(body, BorderLayout.CENTER);
        refreshButton.addActionListener(event -> refresh());
        if (configured) {
            refresh();
        } else {
            refreshButton.setEnabled(false);
            status.setText("Chưa có cấu hình SQL Server development trong worktree này.");
        }
    }

    private JPanel introduction() {
        JPanel panel = card(new BorderLayout(16, 8));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(23, 24, 23, 24)));
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(label("Chào mừng đến hệ thống quản lý nha khoa", 23, NAVY, true));
        copy.add(Box.createVerticalStrut(9));
        copy.add(label("Tổng quan dữ liệu phòng khám", 13, MUTED, false));
        panel.add(copy, BorderLayout.CENTER);
        JLabel account = label("Tài khoản: " + session.accountId(), 12, NAVY, true);
        account.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(account, BorderLayout.EAST);
        return panel;
    }

    private JPanel metrics() {
        JPanel row = new JPanel(new GridLayout(1, 4, 13, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 125));
        row.add(metric(0, "Bệnh nhân", "Tổng hồ sơ bệnh nhân"));
        row.add(metric(1, "Lịch hẹn hôm nay", "Không tính lịch đã hủy hoặc đổi lịch"));
        row.add(metric(2, "Đang khám", "Phiếu khám có trạng thái đang khám"));
        row.add(metric(3, "Đã thu hôm nay", "Thanh toán thành công, đơn vị VND"));
        return row;
    }

    private JPanel metric(int index, String title, String description) {
        JPanel panel = card(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(17, 17, 17, 17)));
        panel.add(label(title, 13, MUTED, true), BorderLayout.NORTH);
        values[index] = label("—", 26, NAVY, true);
        panel.add(values[index], BorderLayout.CENTER);
        descriptions[index] = label("Chưa tải", 11, MUTED, false);
        descriptions[index].setToolTipText(description);
        panel.add(descriptions[index], BorderLayout.SOUTH);
        return panel;
    }

    private JPanel details() {
        JPanel row = new JPanel(new GridLayout(1, 2, 15, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        row.add(information("Dữ liệu thực", "Chỉ thống kê từ cơ sở dữ liệu phát triển đã kết nối. Không tạo dữ liệu mẫu."));
        row.add(information("Phân quyền", "Mỗi tài khoản chỉ xem các chỉ số được cấp quyền. Các chức năng nghiệp vụ do từng nhóm triển khai."));
        return row;
    }

    private JPanel information(String title, String message) {
        JPanel panel = card(new BorderLayout(0, 14));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(23, 22, 23, 22)));
        panel.add(label(title, 17, NAVY, true), BorderLayout.NORTH);
        JTextArea description = new JTextArea(message);
        description.setEditable(false);
        description.setFocusable(false);
        description.setOpaque(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setFont(new Font("SansSerif", Font.PLAIN, 12));
        description.setForeground(MUTED);
        panel.add(description, BorderLayout.CENTER);
        return panel;
    }

    private JPanel footer() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        panel.setOpaque(false);
        panel.add(refreshButton);
        panel.add(status);
        return panel;
    }

    private void refresh() {
        if (loading || !session.isActive()) return;
        loading = true;
        refreshButton.setEnabled(false);
        status.setText("Đang tải dữ liệu từ SQL Server...");
        for (int i = 0; i < values.length; i++) {
            values[i].setText("—");
            descriptions[i].setText("Đang tải");
        }
        new SwingWorker<Summary, Void>() {
            @Override protected Summary doInBackground() throws Exception {
                return service.load();
            }

            @Override protected void done() {
                if (!session.isActive()) return;
                loading = false;
                refreshButton.setEnabled(true);
                try {
                    Summary summary = get();
                    showLong(0, summary.patients());
                    showLong(1, summary.appointments());
                    showLong(2, summary.inProgressExams());
                    showMoney(summary.collected());
                    status.setText("Dữ liệu ngày " + DATE.format(summary.date()) + " · Múi giờ Việt Nam");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showLoadError();
                } catch (ExecutionException | CancellationException ex) {
                    showLoadError();
                }
            }
        }.execute();
    }

    private void showLong(int index, Long number) {
        if (number == null) {
            values[index].setText("—");
            descriptions[index].setText("Không có quyền xem");
        } else {
            values[index].setText(NumberFormat.getIntegerInstance(VIETNAMESE).format(number));
            descriptions[index].setText("Từ SQL Server");
        }
    }

    private void showMoney(BigDecimal amount) {
        if (amount == null) {
            values[3].setText("—");
            descriptions[3].setText("Không có quyền xem");
        } else {
            values[3].setText(NumberFormat.getIntegerInstance(VIETNAMESE).format(amount));
            descriptions[3].setText("VND · Đã thanh toán");
        }
    }

    private void showLoadError() {
        status.setText("Không thể tải số liệu. Kiểm tra kết nối và thử lại.");
        for (JLabel description : descriptions) description.setText("Không thể cập nhật");
    }

    private static JPanel card(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(Color.WHITE);
        return panel;
    }

    private static JLabel label(String text, int size, Color color, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }
}
