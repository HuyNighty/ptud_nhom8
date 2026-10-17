package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.Scrollable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/** Thành phần dùng chung cho các màn danh sách phiếu kho (nhập, xuất, …). */
final class VoucherTable {
    private static final Color LINK = new Color(0x2F5BD3);

    private VoucherTable() {
    }

    /** Khoảng thời gian lọc theo ngày chứng từ. */
    enum TimeRange {
        ALL("Thời gian"),
        TODAY("Hôm nay"),
        LAST_7("7 ngày qua"),
        LAST_30("30 ngày qua"),
        THIS_MONTH("Tháng này"),
        LAST_MONTH("Tháng trước"),
        THIS_YEAR("Năm nay");

        final String label;

        TimeRange(String label) {
            this.label = label;
        }

        boolean contains(LocalDate date, LocalDate today) {
            return switch (this) {
                case ALL -> true;
                case TODAY -> date.equals(today);
                case LAST_7 -> !date.isBefore(today.minusDays(6)) && !date.isAfter(today);
                case LAST_30 -> !date.isBefore(today.minusDays(29)) && !date.isAfter(today);
                case THIS_MONTH -> YearMonth.from(date).equals(YearMonth.from(today));
                case LAST_MONTH -> YearMonth.from(date).equals(YearMonth.from(today).minusMonths(1));
                case THIS_YEAR -> date.getYear() == today.getYear();
            };
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Thao tác trên một dòng phiếu; PROCESS = "Nhập kho" / "Xuất kho" tùy màn hình. */
    enum RowAction {
        VIEW("Xem"), PRINT("In"), EDIT("Sửa"), PROCESS(null);

        final String label;

        RowAction(String label) {
            this.label = label;
        }

        static List<RowAction> forStatus(VoucherStatus status) {
            return switch (status) {
                case DA_HOAN_THANH -> List.of(VIEW, PRINT, EDIT);
                case CHO_XU_LY -> List.of(VIEW, PROCESS);
                case DA_HUY -> List.of(VIEW);
            };
        }
    }

    /** Ô "Thao tác": vẽ các liên kết theo trạng thái; {@link #hitTest} dùng cùng bố cục để bắt click. */
    static final class ActionCell extends JComponent implements TableCellRenderer {
        private static final long serialVersionUID = 1L;
        private static final int PADDING = 16;
        private static final int GAP = 26;
        private static final int PILL_PAD = 12;
        private final String processLabel;
        private final int statusColumn;
        private VoucherStatus status = VoucherStatus.DA_HUY;

        /**
         * @param processLabel chữ trên nút xử lý phiếu chờ ("Nhập kho", "Xuất kho")
         * @param statusColumn cột trong model chứa {@link VoucherStatus}
         */
        ActionCell(String processLabel, int statusColumn) {
            this.processLabel = processLabel;
            this.statusColumn = statusColumn;
            setFont(font(Font.PLAIN, 15));
        }

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            status = (VoucherStatus) tbl.getModel().getValueAt(row, statusColumn);
            return this;
        }

        private String label(RowAction action) {
            return action == RowAction.PROCESS ? processLabel : action.label;
        }

        RowAction hitTest(VoucherStatus rowStatus, int x, FontMetrics fm) {
            int cursor = PADDING;
            for (RowAction action : RowAction.forStatus(rowStatus)) {
                boolean pill = action == RowAction.PROCESS;
                int width = fm.stringWidth(label(action)) + (pill ? 2 * PILL_PAD : 0);
                int start = pill ? cursor - 6 : cursor;
                if (x >= start - 4 && x <= start + width + 4) {
                    return action;
                }
                cursor = start + width + (pill ? 0 : GAP);
            }
            return null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            antialias(g2);
            g2.setColor(SURFACE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int baseline = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            int cursor = PADDING;
            for (RowAction action : RowAction.forStatus(status)) {
                String text = label(action);
                int textWidth = fm.stringWidth(text);
                if (action == RowAction.PROCESS) {
                    int start = cursor - 6;
                    int width = textWidth + 2 * PILL_PAD;
                    g2.setColor(PRIMARY);
                    g2.fillRoundRect(start, (getHeight() - 36) / 2, width, 36, 8, 8);
                    g2.setColor(Color.WHITE);
                    g2.drawString(text, start + PILL_PAD, baseline);
                    cursor = start + width;
                } else {
                    // Phiếu đã hủy chỉ còn "Xem" và hiển thị mờ như thiết kế.
                    g2.setColor(status == VoucherStatus.DA_HUY ? TEXT_MUTED : LINK);
                    g2.drawString(text, cursor, baseline);
                    cursor += textWidth + GAP;
                }
            }
            g2.dispose();
        }
    }

    /**
     * Ô "Thao tác" dạng nút (Figma Hoàn trả kho): "Xem"/"Sửa" viền, nút xử lý nền đậm.
     * Đã hoàn thành → Xem · Sửa; chờ → Xem · {processLabel}; đã hủy → Xem.
     */
    static final class ButtonActionCell extends JComponent implements TableCellRenderer {
        private static final long serialVersionUID = 1L;
        private static final int PADDING = 16;
        private static final int GAP = 12;
        private static final int BUTTON_PAD = 18;
        private static final int BUTTON_HEIGHT = 36;
        private final String processLabel;
        private final int statusColumn;
        private VoucherStatus status = VoucherStatus.DA_HUY;

        ButtonActionCell(String processLabel, int statusColumn) {
            this.processLabel = processLabel;
            this.statusColumn = statusColumn;
            setFont(font(Font.PLAIN, 15));
        }

        static List<RowAction> actions(VoucherStatus status) {
            return switch (status) {
                case DA_HOAN_THANH -> List.of(RowAction.VIEW, RowAction.EDIT);
                case CHO_XU_LY -> List.of(RowAction.VIEW, RowAction.PROCESS);
                case DA_HUY -> List.of(RowAction.VIEW);
            };
        }

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            status = (VoucherStatus) tbl.getModel().getValueAt(row, statusColumn);
            return this;
        }

        private String label(RowAction action) {
            return action == RowAction.PROCESS ? processLabel : action.label;
        }

        RowAction hitTest(VoucherStatus rowStatus, int x, FontMetrics fm) {
            int cursor = PADDING;
            for (RowAction action : actions(rowStatus)) {
                int width = fm.stringWidth(label(action)) + 2 * BUTTON_PAD;
                if (x >= cursor && x <= cursor + width) {
                    return action;
                }
                cursor += width + GAP;
            }
            return null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            antialias(g2);
            g2.setColor(SURFACE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int top = (getHeight() - BUTTON_HEIGHT) / 2;
            int baseline = top + (BUTTON_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
            int cursor = PADDING;
            for (RowAction action : actions(status)) {
                String text = label(action);
                int width = fm.stringWidth(text) + 2 * BUTTON_PAD;
                if (action == RowAction.PROCESS) {
                    g2.setColor(PRIMARY);
                    g2.fillRoundRect(cursor, top, width, BUTTON_HEIGHT, 8, 8);
                    g2.setColor(Color.WHITE);
                } else {
                    g2.setColor(SURFACE);
                    g2.fillRoundRect(cursor, top, width, BUTTON_HEIGHT, 8, 8);
                    g2.setColor(BORDER_STRONG);
                    g2.drawRoundRect(cursor, top, width - 1, BUTTON_HEIGHT - 1, 8, 8);
                    g2.setColor(TEXT);
                }
                g2.drawString(text, cursor + BUTTON_PAD, baseline);
                cursor += width + GAP;
            }
            g2.dispose();
        }
    }

    /** Trạng thái dạng chấm màu + chữ, không nền (Figma Hoàn trả kho); cho phép đổi nhãn trạng thái chờ. */
    static final class DotStatusRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final String pendingLabel;

        DotStatusRenderer(String pendingLabel) {
            this.pendingLabel = pendingLabel;
        }

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            JLabel lblCell = (JLabel) super.getTableCellRendererComponent(tbl, "", false, false, row, column);
            VoucherStatus status = (VoucherStatus) value;
            lblCell.setText(status == VoucherStatus.CHO_XU_LY ? pendingLabel : status.label());
            lblCell.setIcon(new DotIcon(InventoryFormat.statusDot(status), 10));
            lblCell.setIconTextGap(10);
            lblCell.setForeground(TEXT);
            lblCell.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 8));
            return lblCell;
        }
    }

    /** Nhãn trạng thái dạng viên thuốc màu. */
    static final class StatusRenderer implements TableCellRenderer {
        private final StatusPill lblStatus = new StatusPill();
        private final JPanel pnlCell = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 13));

        StatusRenderer() {
            pnlCell.setBackground(SURFACE);
            pnlCell.add(lblStatus);
        }

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            VoucherStatus status = (VoucherStatus) value;
            lblStatus.apply(status.label(), InventoryFormat.statusDot(status), InventoryFormat.statusFill(status),
                    InventoryFormat.statusText(status));
            return pnlCell;
        }
    }

    static final class CellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            JLabel lblCell = (JLabel) super.getTableCellRendererComponent(tbl, value, false, false, row, column);
            lblCell.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 8));
            lblCell.setForeground(TEXT);
            return lblCell;
        }
    }

    static final class HeaderRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            JLabel lblHeaderCell = (JLabel) super.getTableCellRendererComponent(tbl, value, false, false, row, column);
            lblHeaderCell.setFont(font(Font.BOLD, 15));
            lblHeaderCell.setForeground(TEXT);
            lblHeaderCell.setBackground(HEADER_ROW);
            lblHeaderCell.setOpaque(true);
            lblHeaderCell.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                    BorderFactory.createEmptyBorder(0, 16, 0, 8)));
            return lblHeaderCell;
        }
    }

    /** Trang co theo chiều ngang khung nhìn, chỉ cuộn dọc. */
    static final class ScrollablePage extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(visible.height - 40, 18);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
