package com.quanlynhakhoa.desktop.admin;

import com.quanlynhakhoa.desktop.menu.AdminMenu;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Màn hình Tổng quan của Admin.
 * <ul>
 *   <li>Hàng 1: 4 chỉ số tiếp đón / thu tiền (bấm để mở màn hình tương ứng)</li>
 *   <li>Hàng 2: 4 chỉ số cảnh báo vận hành (kho, ca làm, tài khoản)</li>
 *   <li>Trái: bảng "Việc cần xử lý" theo mức ưu tiên — nhấp đúp để mở</li>
 *   <li>Phải: lịch hẹn theo trạng thái, thu tiền theo phương thức</li>
 * </ul>
 * Số liệu hiện là dữ liệu mẫu; khi nối Service thay trong {@link #reload()}.
 */
public class TongQuanPanel extends JPanel {

    private final Consumer<String> openScreen;

    /** Một dòng việc cần xử lý: mức, nội dung, phân hệ (nhóm menu), số lượng, màn hình đích. */
    public record Task(String level, String text, String module, int count, String screenKey) {
    }

    public TongQuanPanel(Consumer<String> openScreen) {
        super(new BorderLayout(0, 14));
        this.openScreen = openScreen;
        setBackground(Ui.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        reload();
    }

    /** Tải lại toàn bộ màn hình. TODO: thay dữ liệu mẫu bằng TongQuanService. */
    public final void reload() {
        removeAll();
        add(buildHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(buildKpis(), BorderLayout.NORTH);
        center.add(buildBottom(), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    // ---------------- Tiêu đề ----------------

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(Ui.label("Tổng quan", Font.BOLD, 22, Ui.STRONG));
        left.add(Ui.label("Thứ Hai, 12/10/2026  ·  số liệu tính tại thời điểm tải (07:45)", Font.PLAIN, 13, Ui.MUTED));
        p.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        right.setOpaque(false);
        JButton reload = Ui.button("Tải lại", false);
        reload.addActionListener(e -> reload());
        right.add(reload);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    // ---------------- Thẻ chỉ số ----------------

    private JPanel buildKpis() {
        JPanel rows = new JPanel(new GridLayout(2, 4, 12, 12));
        rows.setOpaque(false);

        rows.add(kpi("Lịch hẹn hôm nay", "10", "2 chờ xác nhận · 2 đã xác nhận", Ui.BLUE, AdminMenu.LICH_HEN));
        rows.add(kpi("Bệnh nhân chờ khám", "0", "1 đang khám · 2 đã kết luận", Ui.VIOLET, AdminMenu.PHIEU_KHAM));
        rows.add(kpi("Đã thu hôm nay", "700.000 đ", "Chỉ tính lần thu thành công", Ui.GREEN, AdminMenu.HOA_DON));
        rows.add(kpi("Công nợ", "3.600.000 đ", "5 hóa đơn còn nợ", Ui.RED, AdminMenu.HOA_DON));

        rows.add(kpi("Dưới định mức", "2", "mặt hàng cần bổ sung", Ui.AMBER, AdminMenu.THUOC_VAT_TU));
        rows.add(kpi("Lô sắp / đã hết hạn", "2 / 1", "còn tồn trong kho", Ui.RED, AdminMenu.THUOC_VAT_TU));
        rows.add(kpi("Ca thiếu người", "2", "Ca chiều, ca tối thiếu y tá", Ui.RED, AdminMenu.PHAN_CONG));
        rows.add(kpi("Tài khoản bị khóa", "1", "chờ mở khóa", Ui.SLATE, AdminMenu.TAI_KHOAN));
        return rows;
    }

    private JPanel kpi(String title, String value, String sub, Color color, String screenKey) {
        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setBackground(Ui.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Ui.BORDER),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 4, 0, 0, color),
                        BorderFactory.createEmptyBorder(8, 14, 8, 12))));

        JLabel t = Ui.label(title, Font.BOLD, 13, Ui.MUTED);
        JLabel v = Ui.label(value, Font.BOLD, 22, Ui.STRONG);
        JLabel s = Ui.label(sub, Font.PLAIN, 12, Ui.MUTED);
        JLabel arrow = Ui.label("›", Font.BOLD, 18, Ui.MUTED);
        arrow.setVerticalAlignment(SwingConstants.TOP);

        JPanel text = new JPanel(new GridLayout(3, 1, 0, 0));
        text.setOpaque(false);
        text.add(t);
        text.add(v);
        text.add(s);
        card.add(text, BorderLayout.CENTER);
        card.add(arrow, BorderLayout.EAST);

        card.setToolTipText("Mở màn hình " + title.toLowerCase());
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { openScreen.accept(screenKey); }
            @Override public void mouseEntered(MouseEvent e) { card.setBackground(Ui.HEADER_BG); }
            @Override public void mouseExited(MouseEvent e) { card.setBackground(Ui.CARD); }
        });
        return card;
    }

    // ---------------- Phần dưới ----------------

    private JPanel buildBottom() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy = 0;

        c.gridx = 0;
        c.weightx = 0.6;
        c.insets = new Insets(0, 0, 0, 7);
        p.add(buildTasks(), c);

        JPanel right = new JPanel(new BorderLayout(0, 14));
        right.setOpaque(false);
        right.add(buildAppointments(), BorderLayout.NORTH);
        right.add(buildPayments(), BorderLayout.CENTER);

        c.gridx = 1;
        c.weightx = 0.4;
        c.insets = new Insets(0, 7, 0, 0);
        p.add(right, c);
        return p;
    }

    private JPanel section(String title, String hint, JButton action) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Ui.CARD);
        card.setBorder(BorderFactory.createLineBorder(Ui.BORDER));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setBorder(BorderFactory.createEmptyBorder(10, 14, 8, 8));
        JPanel titles = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titles.setOpaque(false);
        titles.add(Ui.label(title, Font.BOLD, 15, Ui.STRONG));
        if (hint != null) {
            JLabel h = Ui.label("   " + hint, Font.PLAIN, 12, Ui.MUTED);
            titles.add(h);
        }
        head.add(titles, BorderLayout.CENTER);
        if (action != null) {
            head.add(action, BorderLayout.EAST);
        }
        card.add(head, BorderLayout.NORTH);
        return card;
    }

    private JPanel buildTasks() {
        List<Task> tasks = List.of(
                new Task("Khẩn", "Lô đã hết hạn còn tồn — lập phiếu hủy", "Thuốc & vật tư", 1, AdminMenu.KIEM_KE),
                new Task("Khẩn", "Hộp cấp cứu cần bổ sung", "Thuốc & vật tư", 1, AdminMenu.CAP_CUU),
                new Task("Khẩn", "Ca hôm nay thiếu y tá (ca chiều, ca tối)", "Nhân viên", 2, AdminMenu.PHAN_CONG),
                new Task("Chờ duyệt", "Yêu cầu hủy hóa đơn", "Thanh toán", 2, AdminMenu.DUYET_HUY),
                new Task("Chờ duyệt", "Phiếu kiểm kê / hao hụt", "Thuốc & vật tư", 1, AdminMenu.KIEM_KE),
                new Task("Chờ duyệt", "Biên bản bàn giao ca chờ xác nhận", "Nhân viên", 2, AdminMenu.BAN_GIAO),
                new Task("Theo dõi", "Hạng mục điều trị đến hạn / quá hạn tái khám", "Khám & điều trị", 6, AdminMenu.TAI_KHAM),
                new Task("Theo dõi", "Mặt hàng dưới định mức tồn", "Thuốc & vật tư", 2, AdminMenu.THUOC_VAT_TU),
                new Task("Theo dõi", "Tài khoản đang bị khóa", "Truy cập", 1, AdminMenu.TAI_KHOAN));

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Mức", "Việc cần xử lý", "Phân hệ", "SL"}, 0);
        tasks.forEach(t -> model.addRow(new Object[]{t.level(), t.text(), t.module(), t.count()}));

        JTable table = new JTable(model);
        Ui.styleTable(table);
        table.setRowHeight(34);
        table.getColumnModel().getColumn(0).setCellRenderer(new LevelRenderer());
        table.getColumnModel().getColumn(2).setCellRenderer(new MutedRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(new Ui.RightRenderer());
        setWidths(table, 96, 0, 130, 56);
        table.setToolTipText("Nhấp đúp để mở màn hình xử lý");
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (e.getClickCount() == 2 && row >= 0) {
                    openScreen.accept(tasks.get(row).screenKey());
                }
            }
        });

        JButton open = Ui.button("Mở", true);
        open.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                openScreen.accept(tasks.get(row).screenKey());
            }
        });
        JPanel card = section("Việc cần xử lý", "nhấp đúp vào dòng để mở", open);
        card.add(Ui.flatScroll(table), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildAppointments() {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Trạng thái", "SL"}, 0);
        Object[][] rows = {
                {"Chờ xác nhận", 2}, {"Đã xác nhận", 2}, {"Đã đến", 1}, {"Hoàn thành", 2},
                {"Vắng mặt", 1}, {"Đã hủy", 1}, {"Đã đổi lịch", 1}};
        for (Object[] r : rows) {
            model.addRow(r);
        }
        JTable table = new JTable(model);
        Ui.styleTable(table);
        table.setRowHeight(26);
        table.getColumnModel().getColumn(1).setCellRenderer(new Ui.RightRenderer());
        table.setPreferredScrollableViewportSize(new Dimension(0, 26 * rows.length));
        setWidths(table, 0, 70);

        JButton link = Ui.link("Mở lịch hẹn ›");
        link.addActionListener(e -> openScreen.accept(AdminMenu.LICH_HEN));
        JPanel card = section("Lịch hẹn theo trạng thái", null, link);
        card.add(Ui.flatScroll(table), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildPayments() {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Phương thức", "Số tiền"}, 0);
        model.addRow(new Object[]{"Tiền mặt", "700.000 đ"});
        model.addRow(new Object[]{"Chuyển khoản ngân hàng", "0 đ"});
        model.addRow(new Object[]{"Thẻ (POS)", "0 đ"});
        JTable table = new JTable(model);
        Ui.styleTable(table);
        table.setRowHeight(26);
        table.getColumnModel().getColumn(1).setCellRenderer(new Ui.RightRenderer());
        table.setPreferredScrollableViewportSize(new Dimension(0, 26 * 3));
        setWidths(table, 0, 120);

        JButton link = Ui.link("Hóa đơn ›");
        link.addActionListener(e -> openScreen.accept(AdminMenu.HOA_DON));
        JPanel card = section("Thu tiền theo phương thức", null, link);
        card.add(Ui.flatScroll(table), BorderLayout.CENTER);

        JPanel total = new JPanel(new BorderLayout());
        total.setBackground(Ui.HEADER_BG);
        total.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Ui.BORDER),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)));
        total.add(Ui.label("Tổng đã thu", Font.BOLD, 13, Ui.STRONG), BorderLayout.WEST);
        total.add(Ui.label("700.000 đ", Font.BOLD, 13, Ui.GREEN), BorderLayout.EAST);
        card.add(total, BorderLayout.SOUTH);
        return card;
    }

    /** Đặt độ rộng cột; 0 = cột co giãn. */
    private static void setWidths(JTable t, int... widths) {
        for (int i = 0; i < widths.length; i++) {
            if (widths[i] > 0) {
                t.getColumnModel().getColumn(i).setMinWidth(widths[i]);
                t.getColumnModel().getColumn(i).setMaxWidth(widths[i]);
            }
        }
    }

    /** Cột "Mức": chữ đậm có màu theo mức ưu tiên. */
    private static final class LevelRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean s, boolean f, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, "● " + value, s, false, r, c);
            Color color = switch (String.valueOf(value)) {
                case "Khẩn" -> Ui.RED;
                case "Chờ duyệt" -> Ui.AMBER;
                default -> Ui.BLUE;
            };
            l.setForeground(color);
            l.setFont(Ui.font(Font.BOLD, 12));
            l.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 6));
            return l;
        }
    }

    private static final class MutedRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean s, boolean f, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, s, false, r, c);
            l.setForeground(Ui.MUTED);
            l.setFont(Ui.font(Font.PLAIN, 12));
            l.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            return l;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(1100, 788);
    }
}
