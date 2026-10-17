package com.quanlynhakhoa.features.inventory.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.Set;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.text.JTextComponent;

/** Màu, font và component vẽ tay dùng chung cho các màn hình kho (không thêm dependency). */
final class InventoryUiKit {

    static final Color BACKGROUND = new Color(0xEEF0F5);
    static final Color SURFACE = Color.WHITE;
    static final Color BORDER = new Color(0xDCE0E8);
    static final Color HEADER_ROW = new Color(0xF3F5F9);
    static final Color TEXT = new Color(0x1B2233);
    static final Color TEXT_MUTED = new Color(0x5E6678);
    static final Color PRIMARY = new Color(0x26366B);
    static final Color PRIMARY_HOVER = new Color(0x31458A);
    static final Color SELECTED_FILL = new Color(0xE3E8F6);
    static final Color NUMBER_BLUE = new Color(0x1E3E9C);
    static final Color NUMBER_BLUE_DARK = new Color(0x1B2A5A);
    static final Color ORANGE = new Color(0xE07B12);
    static final Color RED = new Color(0xD32F2F);
    static final Color GREEN = new Color(0x2E7D32);
    static final Color AMBER = new Color(0xC99A06);
    static final Color BORDER_STRONG = new Color(0xB9C0CE);
    static final Color SIDEBAR = new Color(0xF7F8FB);
    static final Color MENU_SELECTED = new Color(0xE4E9F7);
    static final Color ROLE_CHIP = new Color(0xDDF3F1);
    static final Color ROLE_CHIP_TEXT = new Color(0x1F6E68);

    private static final String FAMILY = pickFamily();

    private InventoryUiKit() {
    }

    private static String pickFamily() {
        Set<String> available = Set.copyOf(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String candidate : new String[] {"Segoe UI", "Inter", "Roboto", "Noto Sans", "DejaVu Sans"}) {
            if (available.contains(candidate)) {
                return candidate;
            }
        }
        return Font.SANS_SERIF;
    }

    static Font font(int style, float size) {
        return new Font(FAMILY, style, Math.round(size));
    }

    static void antialias(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /** Thẻ nền trắng bo góc có viền mảnh. */
    static final class Card extends JPanel {
        private final int arc;
        private Color fill = SURFACE;
        private Color stroke = BORDER;

        Card(LayoutManager layout, int padding) {
            this(layout, padding, 14);
        }

        Card(LayoutManager layout, int padding, int arc) {
            super(layout);
            this.arc = arc;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        }

        /** Thẻ có nền màu nhạt (vd. thẻ thống kê "Chờ kiểm duyệt" cam, "Đã hoàn thành" xanh). */
        Card tint(Color background, Color border) {
            this.fill = background;
            this.stroke = border;
            repaint();
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            antialias(g2);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.setColor(stroke);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    enum ButtonKind { PRIMARY, OUTLINE, CHIP, MENU, PAGE, PAGE_SELECTED, TAB }

    static JButton createButton(String text, ButtonKind kind) {
        JButton btn = new JButton(text);
        styleButton(btn, kind);
        return btn;
    }

    static JToggleButton createToggle(String text) {
        JToggleButton tgl = new JToggleButton(text);
        styleButton(tgl, ButtonKind.CHIP);
        return tgl;
    }

    private static void styleButton(AbstractButton btn, ButtonKind kind) {
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setRolloverEnabled(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFont(font(kind == ButtonKind.PRIMARY ? Font.BOLD : Font.PLAIN, kind == ButtonKind.CHIP ? 13 : 14));
        btn.setForeground(kind == ButtonKind.PRIMARY ? Color.WHITE : TEXT);
        int vertical = kind == ButtonKind.CHIP ? 6 : 10;
        btn.setBorder(BorderFactory.createEmptyBorder(vertical, 14, vertical, 14));
        btn.putClientProperty("n08.kind", kind);
        btn.setUI(new RoundedButtonUI());
    }

    private static final class RoundedButtonUI extends javax.swing.plaf.basic.BasicButtonUI {
        @Override
        public void paint(Graphics g, javax.swing.JComponent c) {
            AbstractButton b = (AbstractButton) c;
            ButtonKind kind = (ButtonKind) b.getClientProperty("n08.kind");
            boolean hover = b.getModel().isRollover();
            boolean selected = b.isSelected();
            Graphics2D g2 = (Graphics2D) g.create();
            antialias(g2);
            int w = c.getWidth() - 1;
            int h = c.getHeight() - 1;
            switch (kind) {
                case PRIMARY -> {
                    g2.setColor(hover ? PRIMARY_HOVER : PRIMARY);
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                }
                case OUTLINE -> {
                    g2.setColor(hover ? HEADER_ROW : SURFACE);
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                    g2.setColor(BORDER);
                    g2.drawRoundRect(0, 0, w, h, 10, 10);
                }
                case TAB -> {
                    if (selected) {
                        g2.setColor(PRIMARY);
                        g2.fillRect(0, h - 2, w + 1, 3);
                    } else if (hover) {
                        g2.setColor(BORDER_STRONG);
                        g2.fillRect(0, h - 1, w + 1, 2);
                    }
                }
                case PAGE, PAGE_SELECTED -> {
                    boolean on = kind == ButtonKind.PAGE_SELECTED;
                    g2.setColor(on ? SELECTED_FILL : (hover && b.isEnabled() ? HEADER_ROW : SURFACE));
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                    g2.setColor(on ? PRIMARY : BORDER);
                    g2.setStroke(new BasicStroke(on ? 1.4f : 1f));
                    g2.drawRoundRect(0, 0, w, h, 10, 10);
                }
                case MENU -> {
                    if (selected || hover) {
                        g2.setColor(selected ? MENU_SELECTED : HEADER_ROW);
                        g2.fillRoundRect(0, 0, w, h, 12, 12);
                    }
                }
                case CHIP -> {
                    g2.setColor(selected ? SELECTED_FILL : (hover ? HEADER_ROW : SURFACE));
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                    g2.setColor(selected ? PRIMARY : BORDER);
                    g2.setStroke(new BasicStroke(selected ? 1.4f : 1f));
                    g2.drawRoundRect(0, 0, w, h, 10, 10);
                }
            }
            g2.dispose();
            super.paint(g, c);
        }
    }

    /** Chấm tròn màu trạng thái (dùng trong bảng và danh sách cần chú ý). */
    record DotIcon(Color color, int size) implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            antialias(g2);
            g2.setColor(color);
            g2.fillOval(x, y, size, size);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    /** Ô nhập liệu viền mảnh, đệm trong — dùng cho JTextField/JTextArea. */
    static <T extends JTextComponent> T styleInput(T input) {
        input.setFont(font(Font.PLAIN, 14));
        input.setForeground(TEXT);
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        return input;
    }

    static JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font(Font.PLAIN, 13));
        lbl.setForeground(TEXT_MUTED);
        return lbl;
    }

    /** Mục menu sidebar: JToggleButton căn trái, có icon, nền xanh nhạt khi đang chọn. */
    static JToggleButton createMenuItem(String text, MenuGlyph glyph) {
        JToggleButton tgl = new JToggleButton(text);
        styleButton(tgl, ButtonKind.MENU);
        tgl.setFont(font(Font.PLAIN, 15));
        tgl.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        if (glyph != null) {
            tgl.setIcon(new GlyphIcon(glyph, 20, TEXT_MUTED));
            tgl.setSelectedIcon(new GlyphIcon(glyph, 20, PRIMARY));
        }
        tgl.setIconTextGap(14);
        tgl.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        return tgl;
    }

    /** Icon vẽ bằng Java2D, không cần file ảnh. */
    enum MenuGlyph { HOME, BOX, CALENDAR, CLINIC, CHEVRON_DOWN, CHEVRON_LEFT, CHEVRON_RIGHT, SEARCH, UPLOAD, DOWNLOAD, PLUS }

    record GlyphIcon(MenuGlyph glyph, int size, Color color) implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            antialias(g2);
            g2.translate(x, y);
            float s = size / 20f;
            g2.scale(s, s);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (glyph) {
                case HOME -> {
                    g2.drawPolyline(new int[] {2, 10, 18}, new int[] {9, 2, 9}, 3);
                    g2.drawPolyline(new int[] {4, 4, 16, 16}, new int[] {8, 18, 18, 8}, 4);
                    g2.drawPolyline(new int[] {8, 8, 12, 12}, new int[] {18, 12, 12, 18}, 4);
                }
                case BOX -> {
                    g2.drawPolygon(new int[] {10, 18, 18, 10, 2, 2}, new int[] {2, 6, 14, 18, 14, 6}, 6);
                    g2.drawPolyline(new int[] {2, 10, 18}, new int[] {6, 10, 6}, 3);
                    g2.drawLine(10, 10, 10, 18);
                }
                case CALENDAR -> {
                    g2.drawRoundRect(2, 4, 16, 14, 3, 3);
                    g2.drawLine(2, 8, 18, 8);
                    g2.drawLine(6, 2, 6, 5);
                    g2.drawLine(14, 2, 14, 5);
                    g2.fillRect(5, 11, 2, 2);
                    g2.fillRect(9, 11, 2, 2);
                    g2.fillRect(13, 11, 2, 2);
                }
                case CLINIC -> {
                    g2.drawRoundRect(3, 3, 14, 14, 4, 4);
                    g2.drawLine(10, 6, 10, 14);
                    g2.drawLine(6, 10, 14, 10);
                }
                case CHEVRON_DOWN -> g2.drawPolyline(new int[] {5, 10, 15}, new int[] {8, 13, 8}, 3);
                case CHEVRON_LEFT -> g2.drawPolyline(new int[] {12, 7, 12}, new int[] {5, 10, 15}, 3);
                case CHEVRON_RIGHT -> g2.drawPolyline(new int[] {8, 13, 8}, new int[] {5, 10, 15}, 3);
                case SEARCH -> {
                    g2.drawOval(3, 3, 11, 11);
                    g2.drawLine(12, 12, 17, 17);
                }
                case UPLOAD -> {
                    g2.drawLine(10, 3, 10, 13);
                    g2.drawPolyline(new int[] {6, 10, 14}, new int[] {7, 3, 7}, 3);
                    g2.drawPolyline(new int[] {3, 3, 17, 17}, new int[] {12, 17, 17, 12}, 4);
                }
                case DOWNLOAD -> {
                    g2.drawLine(10, 3, 10, 13);
                    g2.drawPolyline(new int[] {6, 10, 14}, new int[] {9, 13, 9}, 3);
                    g2.drawPolyline(new int[] {3, 3, 17, 17}, new int[] {12, 17, 17, 12}, 4);
                }
                case PLUS -> {
                    g2.drawLine(10, 4, 10, 16);
                    g2.drawLine(4, 10, 16, 10);
                }
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    /** JTextField có dòng gợi ý mờ khi rỗng. */
    static final class HintTextField extends javax.swing.JTextField {
        private static final long serialVersionUID = 1L;
        private final String hint;

        HintTextField(String hint, int columns) {
            super(columns);
            this.hint = hint;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !hint.isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                antialias(g2);
                g2.setColor(TEXT_MUTED);
                g2.setFont(getFont());
                java.awt.Insets in = getInsets();
                java.awt.FontMetrics fm = g2.getFontMetrics();
                g2.drawString(hint, in.left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        }
    }

    /** JComboBox phẳng: nền trắng, viền mảnh bo góc, mũi tên dạng chevron. */
    static <T> javax.swing.JComboBox<T> styleCombo(javax.swing.JComboBox<T> cmb) {
        cmb.setFont(font(Font.PLAIN, 15));
        cmb.setForeground(TEXT);
        cmb.setBackground(SURFACE);
        cmb.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        cmb.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton btnArrow = new JButton(new GlyphIcon(MenuGlyph.CHEVRON_DOWN, 16, TEXT_MUTED));
                btnArrow.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 10));
                btnArrow.setContentAreaFilled(false);
                btnArrow.setFocusPainted(false);
                return btnArrow;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, java.awt.Rectangle bounds, boolean hasFocus) {
                g.setColor(SURFACE);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });
        cmb.setRenderer(new javax.swing.DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                          boolean selected, boolean focused) {
                JLabel lblItem = (JLabel) super.getListCellRendererComponent(list, value, index, selected, false);
                lblItem.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
                lblItem.setFont(font(Font.PLAIN, 15));
                lblItem.setBackground(selected && index >= 0 ? SELECTED_FILL : SURFACE);
                lblItem.setForeground(cmb.isEnabled() ? TEXT : TEXT_MUTED);
                return lblItem;
            }
        });
        return cmb;
    }

    /** Một nút trong thanh phân đoạn (Tất cả / Thuốc / ...). Dùng chung cho mọi màn kho. */
    static JToggleButton createSegment(String text) {
        JToggleButton tglSegment = createMenuItem(text, null);
        tglSegment.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tglSegment.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        return tglSegment;
    }

    /** Khung bo góc chứa các segment, gom vào một ButtonGroup và chọn sẵn segment đầu tiên được chỉ định. */
    static Card createSegmentBar(JToggleButton selected, JToggleButton... segments) {
        Card pnlSegments = new Card(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 2, 0), 3, 10);
        javax.swing.ButtonGroup grpSegments = new javax.swing.ButtonGroup();
        for (JToggleButton tglSegment : segments) {
            grpSegments.add(tglSegment);
            pnlSegments.add(tglSegment);
        }
        selected.setSelected(true);
        return pnlSegments;
    }

    /** Nhãn trạng thái dạng viên thuốc: chấm màu + chữ trên nền nhạt. */
    static final class StatusPill extends JLabel {
        private static final long serialVersionUID = 1L;
        private Color fill = HEADER_ROW;

        StatusPill() {
            setFont(font(Font.PLAIN, 14));
            setIconTextGap(8);
            setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 14));
        }

        void apply(String text, Color dot, Color background, Color foreground) {
            setText(text);
            setIcon(new DotIcon(dot, 10));
            fill = background;
            setForeground(foreground);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            antialias(g2);
            g2.setColor(fill);
            java.awt.Dimension d = getPreferredSize();
            int h = Math.min(getHeight(), d.height);
            g2.fillRoundRect(0, (getHeight() - h) / 2, Math.min(getWidth(), d.width), h, h, h);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Bộ phân trang: ‹ 1 2 3 … 11 ›. Gọi {@link #update} sau mỗi lần đổi dữ liệu/trang. */
    static final class Pager extends JPanel {
        private static final long serialVersionUID = 1L;
        private transient java.util.function.IntConsumer onPage = page -> { };

        Pager() {
            super(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 0));
            setOpaque(false);
        }

        void onPageSelected(java.util.function.IntConsumer handler) {
            onPage = java.util.Objects.requireNonNull(handler);
        }

        /** @param pageCount 0 = ẩn bộ phân trang */
        void update(int current, int pageCount) {
            removeAll();
            if (pageCount > 0) {
                JButton btnPrevPage = pagerButton(null, MenuGlyph.CHEVRON_LEFT, false);
                btnPrevPage.setEnabled(current > 0);
                btnPrevPage.addActionListener(e -> onPage.accept(current - 1));
                add(btnPrevPage);
                for (Integer page : visiblePages(current, pageCount)) {
                    if (page == null) {
                        JLabel lblEllipsis = new JLabel("…");
                        lblEllipsis.setFont(font(Font.PLAIN, 15));
                        lblEllipsis.setForeground(TEXT_MUTED);
                        add(lblEllipsis);
                    } else {
                        JButton btnPage = pagerButton(String.valueOf(page + 1), null, page == current);
                        btnPage.addActionListener(e -> onPage.accept(page));
                        add(btnPage);
                    }
                }
                JButton btnNextPage = pagerButton(null, MenuGlyph.CHEVRON_RIGHT, false);
                btnNextPage.setEnabled(current < pageCount - 1);
                btnNextPage.addActionListener(e -> onPage.accept(current + 1));
                add(btnNextPage);
            }
            revalidate();
            repaint();
        }

        /** Trang hiển thị: đầu, cuối, quanh trang hiện tại; null = "…". VD 0,1,2,3,null,10. */
        static java.util.List<Integer> visiblePages(int current, int pageCount) {
            java.util.TreeSet<Integer> pages = new java.util.TreeSet<>();
            pages.add(0);
            pages.add(pageCount - 1);
            for (int p = Math.max(0, current - 1); p <= Math.min(pageCount - 1, current + 2); p++) {
                pages.add(p);
            }
            if (current < 3) {
                for (int p = 0; p < Math.min(4, pageCount); p++) {
                    pages.add(p);
                }
            }
            java.util.List<Integer> result = new java.util.ArrayList<>();
            Integer previous = null;
            for (Integer p : pages) {
                if (previous != null && p - previous > 1) {
                    result.add(null);
                }
                result.add(p);
                previous = p;
            }
            return result;
        }

        private static JButton pagerButton(String text, MenuGlyph glyph, boolean selected) {
            JButton btnPager = createButton(text == null ? "" : text,
                    selected ? ButtonKind.PAGE_SELECTED : ButtonKind.PAGE);
            if (glyph != null) {
                btnPager.setIcon(new GlyphIcon(glyph, 16, TEXT));
                btnPager.setDisabledIcon(new GlyphIcon(glyph, 16, BORDER_STRONG));
            }
            btnPager.setFont(font(Font.PLAIN, 15));
            btnPager.setPreferredSize(new java.awt.Dimension(44, 40));
            btnPager.setBorder(BorderFactory.createEmptyBorder());
            return btnPager;
        }
    }

    /**
     * Kiểu bảng dùng chung cho mọi bảng kho (màn chính và hộp thoại): tiêu đề nền nhạt chữ đậm căn trái,
     * chỉ kẻ ngang, ô có đệm, cột số/tiền căn phải.
     *
     * @param rightAligned chỉ số các cột căn phải (số lượng, tiền)
     */
    static void styleTable(javax.swing.JTable tbl, int rowHeight, int[] widths, java.util.Set<Integer> rightAligned) {
        tbl.setFont(font(Font.PLAIN, 14));
        tbl.setForeground(TEXT);
        tbl.setBackground(SURFACE);
        tbl.setRowHeight(rowHeight);
        tbl.setShowVerticalLines(false);
        tbl.setShowHorizontalLines(true);
        tbl.setGridColor(BORDER);
        tbl.setIntercellSpacing(new java.awt.Dimension(0, 1));
        tbl.setSelectionBackground(SELECTED_FILL);
        tbl.setSelectionForeground(TEXT);
        tbl.setFillsViewportHeight(true);
        javax.swing.table.DefaultTableCellRenderer cells = new javax.swing.table.DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean selected,
                                                           boolean focused, int row, int column) {
                JLabel lblCell = (JLabel) super.getTableCellRendererComponent(table, value, selected, false, row, column);
                lblCell.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
                lblCell.setHorizontalAlignment(rightAligned.contains(column)
                        ? javax.swing.SwingConstants.RIGHT : javax.swing.SwingConstants.LEFT);
                lblCell.setForeground("—".equals(value) ? TEXT_MUTED : TEXT);
                return lblCell;
            }
        };
        tbl.setDefaultRenderer(Object.class, cells);
        tbl.setDefaultRenderer(Integer.class, cells);
        javax.swing.table.JTableHeader tbhTable = tbl.getTableHeader();
        tbhTable.setReorderingAllowed(false);
        tbhTable.setPreferredSize(new java.awt.Dimension(10, 42));
        tbhTable.setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean selected,
                                                           boolean focused, int row, int column) {
                JLabel lblHeaderCell = (JLabel) super.getTableCellRendererComponent(table, value, false, false, row, column);
                lblHeaderCell.setFont(font(Font.BOLD, 14));
                lblHeaderCell.setForeground(TEXT);
                lblHeaderCell.setBackground(HEADER_ROW);
                lblHeaderCell.setOpaque(true);
                lblHeaderCell.setHorizontalAlignment(rightAligned.contains(column)
                        ? javax.swing.SwingConstants.RIGHT : javax.swing.SwingConstants.LEFT);
                lblHeaderCell.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                        BorderFactory.createEmptyBorder(0, 14, 0, 14)));
                return lblHeaderCell;
            }
        });
        for (int i = 0; i < widths.length && i < tbl.getColumnCount(); i++) {
            tbl.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    /** Khung cuộn viền bo góc nhạt cho bảng trong hộp thoại, nền trắng tới hết vùng trống. */
    static javax.swing.JScrollPane tableScroll(javax.swing.JTable tbl) {
        javax.swing.JScrollPane scrTable = new javax.swing.JScrollPane(tbl);
        scrTable.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        scrTable.getViewport().setBackground(SURFACE);
        scrTable.setBackground(SURFACE);
        javax.swing.JPanel pnlCorner = new javax.swing.JPanel();
        pnlCorner.setBackground(HEADER_ROW);
        scrTable.setCorner(javax.swing.ScrollPaneConstants.UPPER_RIGHT_CORNER, pnlCorner);
        return scrTable;
    }

    /**
     * Chốt giá trị người dùng vừa gõ trong JSpinner (khi chưa rời ô thì giá trị chưa được ghi nhận).
     *
     * @return false nếu có ô gõ sai định dạng — khi đó trả về giá trị cũ và báo lỗi
     */
    static boolean commitSpinners(JLabel lblError, javax.swing.JSpinner... spinners) {
        for (javax.swing.JSpinner spinner : spinners) {
            try {
                spinner.commitEdit();
            } catch (java.text.ParseException e) {
                if (spinner.getEditor() instanceof javax.swing.JSpinner.DefaultEditor editor) {
                    editor.getTextField().setValue(spinner.getValue());
                }
                lblError.setText("Giá trị \"" + (spinner.getEditor() instanceof javax.swing.JSpinner.DefaultEditor ed
                        ? ed.getTextField().getText() : "") + "\" không hợp lệ — đã trả lại giá trị cũ, kiểm tra lại ô số/ngày.");
                return false;
            }
        }
        return true;
    }

    /** Tab gạch chân (Tất cả / Chờ xử lý / Đã hoàn thành). */
    static JToggleButton createTab(String text) {
        JToggleButton tglTab = new JToggleButton(text);
        styleButton(tglTab, ButtonKind.TAB);
        tglTab.setFont(font(Font.PLAIN, 16));
        tglTab.setBorder(BorderFactory.createEmptyBorder(8, 16, 12, 16));
        tglTab.addChangeListener(e -> tglTab.setForeground(tglTab.isSelected() ? PRIMARY : TEXT));
        return tglTab;
    }

    /** Hàng tab có đường kẻ mảnh phía dưới; chọn sẵn tab {@code selected}. */
    static JPanel createTabBar(JToggleButton selected, JToggleButton... tabs) {
        JPanel pnlTabs = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        pnlTabs.setOpaque(false);
        pnlTabs.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        javax.swing.ButtonGroup grpTabs = new javax.swing.ButtonGroup();
        for (JToggleButton tglTab : tabs) {
            grpTabs.add(tglTab);
            pnlTabs.add(tglTab);
        }
        selected.setSelected(true);
        return pnlTabs;
    }
}
