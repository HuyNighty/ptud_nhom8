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
    private final JLabel[] lblValues = new JLabel[4];
    private final JLabel[] lblDescriptions = new JLabel[4];
    private final JLabel lblStatus = label("Chưa tải dữ liệu", 12, MUTED, false);
    private final JButton btnRefresh = new JButton("Làm mới");
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

        JPanel pnlBody = new JPanel();
        pnlBody.setOpaque(false);
        pnlBody.setLayout(new BoxLayout(pnlBody, BoxLayout.Y_AXIS));
        pnlBody.add(metrics());
        pnlBody.add(Box.createVerticalStrut(18));
        pnlBody.add(details());
        pnlBody.add(Box.createVerticalStrut(18));
        pnlBody.add(footer());
        add(pnlBody, BorderLayout.CENTER);
        btnRefresh.addActionListener(event -> refresh());
        if (configured) {
            refresh();
        } else {
            btnRefresh.setEnabled(false);
            lblStatus.setText("Chưa có cấu hình SQL Server development trong worktree này.");
        }
    }

    private JPanel introduction() {
        JPanel pnlPanel = card(new BorderLayout(16, 8));
        pnlPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(23, 24, 23, 24)));
        JPanel pnlCopy = new JPanel();
        pnlCopy.setOpaque(false);
        pnlCopy.setLayout(new BoxLayout(pnlCopy, BoxLayout.Y_AXIS));
        pnlCopy.add(label("Chào mừng đến hệ thống quản lý nha khoa", 23, NAVY, true));
        pnlCopy.add(Box.createVerticalStrut(9));
        pnlCopy.add(label("Tổng quan dữ liệu phòng khám", 13, MUTED, false));
        pnlPanel.add(pnlCopy, BorderLayout.CENTER);
        JLabel lblAccount = label("Tài khoản: " + session.accountId(), 12, NAVY, true);
        lblAccount.setHorizontalAlignment(SwingConstants.RIGHT);
        pnlPanel.add(lblAccount, BorderLayout.EAST);
        return pnlPanel;
    }

    private JPanel metrics() {
        JPanel pnlRow = new JPanel(new GridLayout(1, 4, 13, 0));
        pnlRow.setOpaque(false);
        pnlRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 125));
        pnlRow.add(metric(0, "Bệnh nhân", "Tổng hồ sơ bệnh nhân"));
        pnlRow.add(metric(1, "Lịch hẹn hôm nay", "Không tính lịch đã hủy hoặc đổi lịch"));
        pnlRow.add(metric(2, "Đang khám", "Phiếu khám có trạng thái đang khám"));
        pnlRow.add(metric(3, "Đã thu hôm nay", "Thanh toán thành công, đơn vị VND"));
        return pnlRow;
    }

    private JPanel metric(int index, String title, String description) {
        JPanel pnlPanel = card(new BorderLayout(0, 10));
        pnlPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(17, 17, 17, 17)));
        pnlPanel.add(label(title, 13, MUTED, true), BorderLayout.NORTH);
        lblValues[index] = label("—", 26, NAVY, true);
        pnlPanel.add(lblValues[index], BorderLayout.CENTER);
        lblDescriptions[index] = label("Chưa tải", 11, MUTED, false);
        lblDescriptions[index].setToolTipText(description);
        pnlPanel.add(lblDescriptions[index], BorderLayout.SOUTH);
        return pnlPanel;
    }

    private JPanel details() {
        JPanel pnlRow = new JPanel(new GridLayout(1, 2, 15, 0));
        pnlRow.setOpaque(false);
        pnlRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        pnlRow.add(information("Dữ liệu thực", "Chỉ thống kê từ cơ sở dữ liệu phát triển đã kết nối. Không tạo dữ liệu mẫu."));
        pnlRow.add(information("Phân quyền", "Mỗi tài khoản chỉ xem các chỉ số được cấp quyền. Các chức năng nghiệp vụ do từng nhóm triển khai."));
        return pnlRow;
    }

    private JPanel information(String title, String message) {
        JPanel pnlPanel = card(new BorderLayout(0, 14));
        pnlPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(23, 22, 23, 22)));
        pnlPanel.add(label(title, 17, NAVY, true), BorderLayout.NORTH);
        JTextArea txaDescription = new JTextArea(message);
        txaDescription.setEditable(false);
        txaDescription.setFocusable(false);
        txaDescription.setOpaque(false);
        txaDescription.setLineWrap(true);
        txaDescription.setWrapStyleWord(true);
        txaDescription.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txaDescription.setForeground(MUTED);
        pnlPanel.add(txaDescription, BorderLayout.CENTER);
        return pnlPanel;
    }

    private JPanel footer() {
        JPanel pnlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        pnlPanel.setOpaque(false);
        pnlPanel.add(btnRefresh);
        pnlPanel.add(lblStatus);
        return pnlPanel;
    }

    private void refresh() {
        if (loading || !session.isActive()) return;
        loading = true;
        btnRefresh.setEnabled(false);
        lblStatus.setText("Đang tải dữ liệu từ SQL Server...");
        for (int i = 0; i < lblValues.length; i++) {
            lblValues[i].setText("—");
            lblDescriptions[i].setText("Đang tải");
        }
        new SwingWorker<Summary, Void>() {
            @Override protected Summary doInBackground() throws Exception {
                return service.load();
            }

            @Override protected void done() {
                if (!session.isActive()) return;
                loading = false;
                btnRefresh.setEnabled(true);
                try {
                    Summary summary = get();
                    showLong(0, summary.patients());
                    showLong(1, summary.appointments());
                    showLong(2, summary.inProgressExams());
                    showMoney(summary.collected());
                    lblStatus.setText("Dữ liệu ngày " + DATE.format(summary.date()) + " · Múi giờ Việt Nam");
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
            lblValues[index].setText("—");
            lblDescriptions[index].setText("Không có quyền xem");
        } else {
            lblValues[index].setText(NumberFormat.getIntegerInstance(VIETNAMESE).format(number));
            lblDescriptions[index].setText("Từ SQL Server");
        }
    }

    private void showMoney(BigDecimal amount) {
        if (amount == null) {
            lblValues[3].setText("—");
            lblDescriptions[3].setText("Không có quyền xem");
        } else {
            lblValues[3].setText(NumberFormat.getIntegerInstance(VIETNAMESE).format(amount));
            lblDescriptions[3].setText("VND · Đã thanh toán");
        }
    }

    private void showLoadError() {
        lblStatus.setText("Không thể tải số liệu. Kiểm tra kết nối và thử lại.");
        for (JLabel lblDescription : lblDescriptions) lblDescription.setText("Không thể cập nhật");
    }

    private static JPanel card(java.awt.LayoutManager layout) {
        JPanel pnlPanel = new JPanel(layout);
        pnlPanel.setBackground(Color.WHITE);
        return pnlPanel;
    }

    private static JLabel label(String text, int size, Color color, boolean bold) {
        JLabel lblLabel = new JLabel(text);
        lblLabel.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
        lblLabel.setForeground(color);
        return lblLabel;
    }
}
