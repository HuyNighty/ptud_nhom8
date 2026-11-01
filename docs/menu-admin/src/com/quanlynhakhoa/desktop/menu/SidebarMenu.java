package com.quanlynhakhoa.desktop.menu;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.net.URL;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Thanh menu dọc bên trái: logo, ô tìm chức năng, mục "Tổng quan" và các nhóm nghiệp vụ
 * xổ xuống / thu gọn (có hiệu ứng), icon bên trái từng mục, badge số việc chờ xử lý.
 *
 * <pre>
 * SidebarMenu menu = new SidebarMenu("N08 Nha Khoa", "Quản lý phòng khám", null,
 *         AdminMenu.home(), AdminMenu.groups());
 * menu.addSelectionListener(key -> cardLayout.show(content, key));
 * menu.setBadge(AdminMenu.TAI_KHAM, 6);
 * menu.select(AdminMenu.TONG_QUAN);
 * </pre>
 *
 * Icon từng mục: nếu có ảnh {@code /images/menu/<key>.png} trong resources thì dùng ảnh đó,
 * không thì dùng icon vẽ sẵn {@link MenuIcon}.
 */
public class SidebarMenu extends JPanel {

    // ===== Bảng màu (đồng bộ với prototype Admin v0.3) =====
    public static final Color BG = Color.WHITE;
    public static final Color BORDER = new Color(0xE5E7EB);
    public static final Color TEXT = new Color(0x1F2937);
    public static final Color TEXT_STRONG = new Color(0x111827);
    public static final Color MUTED = new Color(0x6B7280);
    public static final Color ICON = new Color(0x4B5563);
    public static final Color ACCENT = new Color(0x2563EB);
    public static final Color ACCENT_DARK = new Color(0x1E3A8A);
    public static final Color SELECTED_BG = new Color(0xE8EFFD);
    public static final Color HOVER_BG = new Color(0xF3F4F6);
    public static final Color BADGE = new Color(0xDC2626);
    public static final Color FIELD_BG = new Color(0xF3F4F6);

    private static final int WIDTH = 272;
    private static final int ITEM_HEIGHT = 38;
    private static final int HEADER_HEIGHT = 38;
    private static final int ANIMATION_MS = 180;

    private static final String FONT_FAMILY = pickFontFamily();
    private static final Font F_ITEM = new Font(FONT_FAMILY, Font.PLAIN, 14);
    private static final Font F_ITEM_BOLD = new Font(FONT_FAMILY, Font.BOLD, 14);
    private static final Font F_GROUP = new Font(FONT_FAMILY, Font.BOLD, 13);
    private static final Font F_BADGE = new Font(FONT_FAMILY, Font.BOLD, 11);
    private static final Font F_TITLE = new Font(FONT_FAMILY, Font.BOLD, 18);
    private static final Font F_SUBTITLE = new Font(FONT_FAMILY, Font.PLAIN, 12);

    private final Map<String, ItemButton> items = new LinkedHashMap<>();
    private final List<GroupSection> sections = new ArrayList<>();
    private final List<Consumer<String>> listeners = new ArrayList<>();
    private final BodyPanel body = new BodyPanel();
    private final JLabel emptyLabel = new JLabel("Không tìm thấy chức năng phù hợp", SwingConstants.CENTER);
    private final JScrollPane scroll;
    private ItemButton homeItem;
    private ItemButton selected;
    private boolean accordion;
    private boolean searching;

    public SidebarMenu(String title, String subtitle, Image logo,
                       AdminMenu.Entry home, List<AdminMenu.Group> groups) {
        super(new BorderLayout());
        setBackground(BG);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));

        add(buildHeader(title, subtitle, logo), BorderLayout.NORTH);

        if (home != null) {
            homeItem = new ItemButton(home, null, 14);
            items.put(home.key(), homeItem);
            body.add(homeItem);
            body.add(Box.createVerticalStrut(4));
        }
        for (AdminMenu.Group g : groups) {
            GroupSection section = new GroupSection(g);
            sections.add(section);
            body.add(section);
        }
        emptyLabel.setFont(F_SUBTITLE);
        emptyLabel.setForeground(MUTED);
        emptyLabel.setAlignmentX(LEFT_ALIGNMENT);
        emptyLabel.setBorder(BorderFactory.createEmptyBorder(16, 14, 0, 14));
        emptyLabel.setVisible(false);
        body.add(emptyLabel);
        body.add(Box.createVerticalGlue());

        scroll = new JScrollPane(body, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scroll.getVerticalScrollBar().setUI(new ThinScrollBarUI());
        scroll.getVerticalScrollBar().setOpaque(false);
        add(scroll, BorderLayout.CENTER);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(WIDTH, super.getPreferredSize().height);
    }

    // ================= API công khai =================

    /** Nhận khóa màn hình mỗi khi người dùng chọn một mục. */
    public void addSelectionListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    /** Chọn mục theo khóa, tự mở nhóm chứa nó và cuộn tới; có phát sự kiện. */
    public void select(String key) {
        select(key, true);
    }

    public void select(String key, boolean notify) {
        ItemButton item = items.get(key);
        if (item == null || !item.allowed) {
            return;
        }
        if (selected != null) {
            selected.setSelected(false);
        }
        selected = item;
        item.setSelected(true);
        if (item.section != null && !item.section.expanded) {
            item.section.setExpanded(true, false);
        }
        sections.forEach(s -> s.header.repaint());
        SwingUtilities.invokeLater(() -> item.scrollRectToVisible(new Rectangle(0, 0, item.getWidth(), item.getHeight())));
        if (notify) {
            listeners.forEach(l -> l.accept(key));
        }
    }

    public String getSelectedKey() {
        return selected == null ? null : selected.entry.key();
    }

    /** Đặt số việc chờ xử lý cạnh mục (0 = ẩn). Nhóm đang thu gọn hiện tổng badge. */
    public void setBadge(String key, int count) {
        ItemButton item = items.get(key);
        if (item != null) {
            item.badge = Math.max(0, count);
            item.repaint();
            if (item.section != null) {
                item.section.header.repaint();
            }
        }
    }

    /** Ẩn/hiện một mục (ví dụ lọc theo quyền của vai trò). Nhóm không còn mục nào sẽ bị ẩn. */
    public void setEntryVisible(String key, boolean visible) {
        ItemButton item = items.get(key);
        if (item != null) {
            item.allowed = visible;
            applyFilter(currentQuery);
        }
    }

    /** Chế độ accordion: mở một nhóm thì các nhóm khác tự thu gọn. */
    public void setAccordion(boolean accordion) {
        this.accordion = accordion;
    }

    public void setGroupExpanded(String groupKey, boolean expanded) {
        sections.stream().filter(s -> s.group.key().equals(groupKey))
                .forEach(s -> s.setExpanded(expanded, true));
    }

    public void expandAll() {
        sections.forEach(s -> s.setExpanded(true, true));
    }

    public void collapseAll() {
        sections.forEach(s -> s.setExpanded(false, true));
    }

    // ================= Header: logo + tiêu đề + tìm kiếm =================

    private String currentQuery = "";

    private JComponent buildHeader(String title, String subtitle, Image logo) {
        JPanel header = new JPanel(new BorderLayout(0, 14));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(18, 14, 12, 14));

        JPanel brand = new JPanel(new BorderLayout(12, 0));
        brand.setOpaque(false);
        brand.add(new JLabel(new LogoIcon(logo, 42)), BorderLayout.WEST);

        JPanel texts = new JPanel(new GridLayout(2, 1, 0, 0));
        texts.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(F_TITLE);
        t.setForeground(ACCENT_DARK);
        JLabel s = new JLabel(subtitle);
        s.setFont(F_SUBTITLE);
        s.setForeground(MUTED);
        texts.add(t);
        texts.add(s);
        brand.add(texts, BorderLayout.CENTER);
        header.add(brand, BorderLayout.NORTH);

        SearchField search = new SearchField();
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(search.getText()); }
            public void removeUpdate(DocumentEvent e) { applyFilter(search.getText()); }
            public void changedUpdate(DocumentEvent e) { applyFilter(search.getText()); }
        });
        header.add(search, BorderLayout.SOUTH);
        return header;
    }

    private void applyFilter(String raw) {
        currentQuery = raw == null ? "" : raw;
        String q = normalize(currentQuery.trim());
        boolean nowSearching = !q.isEmpty();
        boolean any = false;

        if (homeItem != null) {
            homeItem.setVisible(homeItem.allowed && (!nowSearching || homeItem.matches(q)));
            any |= homeItem.isVisible();
        }
        for (GroupSection s : sections) {
            boolean groupMatch = nowSearching && normalize(s.group.title()).contains(q);
            boolean anyInGroup = false;
            for (ItemButton it : s.items) {
                boolean show = it.allowed && (!nowSearching || groupMatch || it.matches(q));
                it.setVisible(show);
                anyInGroup |= show;
            }
            s.setVisible(anyInGroup);
            any |= anyInGroup;
            if (nowSearching && !searching) {
                s.expandedBeforeSearch = s.expanded;
            }
            if (nowSearching) {
                s.setExpanded(true, false);
            } else if (searching) {
                s.setExpanded(s.expandedBeforeSearch || s.containsSelected(), false);
            }
        }
        searching = nowSearching;
        emptyLabel.setVisible(!any);
        body.revalidate();
        body.repaint();
    }

    // ================= Thành phần con =================

    /** Panel nội dung cuộn, luôn rộng bằng viewport. */
    private static final class BodyPanel extends JPanel implements Scrollable {
        BodyPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(BG);
            setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        }

        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - 32; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /** Một nhóm nghiệp vụ: tiêu đề bấm để xổ xuống + danh sách mục. */
    private final class GroupSection extends JPanel {
        final AdminMenu.Group group;
        final GroupHeader header;
        final CollapsePanel content = new CollapsePanel();
        final List<ItemButton> items = new ArrayList<>();
        boolean expanded = true;
        boolean expandedBeforeSearch = true;
        double progress = 1.0;
        private Timer timer;

        GroupSection(AdminMenu.Group group) {
            this.group = group;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            header = new GroupHeader(this);
            add(header);
            add(content);
            for (AdminMenu.Entry e : group.entries()) {
                ItemButton it = new ItemButton(e, this, 30);
                items.add(it);
                SidebarMenu.this.items.put(e.key(), it);
                content.add(it);
            }
        }

        boolean containsSelected() {
            return selected != null && selected.section == this;
        }

        int badgeSum() {
            int sum = 0;
            for (ItemButton it : items) {
                if (it.allowed) {
                    sum += it.badge;
                }
            }
            return sum;
        }

        void toggle() {
            setExpanded(!expanded, true);
        }

        void setExpanded(boolean value, boolean animate) {
            if (value && accordion && !searching) {
                for (GroupSection other : sections) {
                    if (other != this && other.expanded) {
                        other.setExpanded(false, animate);
                    }
                }
            }
            expanded = value;
            double target = value ? 1.0 : 0.0;
            if (timer != null) {
                timer.stop();
            }
            if (!animate || !isShowing()) {
                progress = target;
                refresh();
                return;
            }
            double from = progress;
            long start = System.currentTimeMillis();
            timer = new Timer(15, (ActionEvent ev) -> {
                double t = Math.min(1.0, (System.currentTimeMillis() - start) / (double) ANIMATION_MS);
                double eased = 1 - Math.pow(1 - t, 3);
                progress = from + (target - from) * eased;
                refresh();
                if (t >= 1.0) {
                    ((Timer) ev.getSource()).stop();
                }
            });
            timer.start();
        }

        private void refresh() {
            content.setVisible(progress > 0.001);
            content.revalidate();
            header.repaint();
            body.revalidate();
            body.repaint();
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        /** Vùng chứa mục, chiều cao co giãn theo {@code progress} để tạo hiệu ứng xổ xuống. */
        final class CollapsePanel extends JPanel {
            CollapsePanel() {
                super(null);
                setOpaque(false);
                setAlignmentX(LEFT_ALIGNMENT);
            }

            private int fullHeight() {
                int h = 0;
                for (Component c : getComponents()) {
                    if (c.isVisible()) {
                        h += c.getPreferredSize().height;
                    }
                }
                return h > 0 ? h + 4 : 0;
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(WIDTH, (int) Math.round(fullHeight() * progress));
            }

            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }

            @Override
            public void doLayout() {
                int y = 0;
                for (Component c : getComponents()) {
                    if (c.isVisible()) {
                        int h = c.getPreferredSize().height;
                        c.setBounds(0, y, getWidth(), h);
                        y += h;
                    }
                }
            }
        }
    }

    /** Tiêu đề nhóm: icon nhóm, tên nhóm, tổng badge khi thu gọn, mũi tên xoay. */
    private final class GroupHeader extends JComponent {
        private final GroupSection section;
        private final MenuIcon icon;
        private final MenuIcon chevron = new MenuIcon(MenuIcon.Shape.CHEVRON_DOWN, 16, MUTED).withStroke(2f);
        private boolean hover;

        GroupHeader(GroupSection section) {
            this.section = section;
            this.icon = new MenuIcon(section.group.icon(), 18, ICON);
            setAlignmentX(LEFT_ALIGNMENT);
            setFocusable(true);
            setRequestFocusEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Bấm để xổ xuống / thu gọn nhóm " + section.group.title());
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        section.toggle();
                    }
                }
            });
            bindKeys(this, section::toggle);
        }

        @Override public Dimension getPreferredSize() { return new Dimension(WIDTH, HEADER_HEIGHT); }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, HEADER_HEIGHT); }
        @Override public Dimension getMinimumSize() { return getPreferredSize(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = prepare(g);
            int w = getWidth();
            int h = getHeight();
            if (hover) {
                g2.setColor(HOVER_BG);
                g2.fill(new RoundRectangle2D.Double(8, 2, w - 16, h - 4, 10, 10));
            }
            boolean active = section.containsSelected();
            Color fg = active ? ACCENT : TEXT_STRONG;

            icon.setColor(active ? ACCENT : ICON);
            icon.paintIcon(this, g2, 16, (h - 18) / 2);

            g2.setFont(F_GROUP);
            g2.setColor(fg);
            FontMetrics fm = g2.getFontMetrics();
            int sum = section.badgeSum();
            boolean showSum = sum > 0 && section.progress < 0.5;
            int titleMax = w - 44 - 36 - (showSum ? 32 : 0);
            g2.drawString(ellipsize(section.group.title(), fm, titleMax), 44, (h - fm.getHeight()) / 2 + fm.getAscent());

            // mũi tên: -90° khi thu gọn, 0° khi mở
            int cx = w - 30;
            int cy = (h - 16) / 2;
            Graphics2D gc = (Graphics2D) g2.create();
            gc.rotate(Math.toRadians(-90 * (1 - section.progress)), cx + 8, cy + 8);
            chevron.paintIcon(this, gc, cx, cy);
            gc.dispose();

            if (showSum) {
                paintBadge(g2, sum, cx - 6, h, true);
            }
            if (isFocusOwner()) {
                paintFocus(g2, w, h);
            }
            g2.dispose();
        }
    }

    /** Một mục chức năng. */
    private final class ItemButton extends JComponent {
        final AdminMenu.Entry entry;
        final GroupSection section;
        private final int indent;
        private final Icon imageIcon;
        private final MenuIcon vectorIcon;
        private final String searchText;
        boolean allowed = true;
        int badge;
        private boolean hover;
        private boolean isSelected;

        ItemButton(AdminMenu.Entry entry, GroupSection section, int indent) {
            this.entry = entry;
            this.section = section;
            this.indent = indent;
            this.vectorIcon = new MenuIcon(entry.icon(), 18, ICON);
            this.imageIcon = loadImageIcon(entry.key());
            this.searchText = normalize(entry.title() + " " + entry.hint());
            setAlignmentX(LEFT_ALIGNMENT);
            setFocusable(true);
            setRequestFocusEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(entry.hint());
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        select(entry.key());
                    }
                }
            });
            bindKeys(this, () -> select(entry.key()));
        }

        boolean matches(String q) {
            return searchText.contains(q);
        }

        void setSelected(boolean value) {
            isSelected = value;
            repaint();
        }

        @Override public Dimension getPreferredSize() { return new Dimension(WIDTH, ITEM_HEIGHT); }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, ITEM_HEIGHT); }
        @Override public Dimension getMinimumSize() { return getPreferredSize(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = prepare(g);
            int w = getWidth();
            int h = getHeight();
            int left = 8;
            if (isSelected) {
                g2.setColor(SELECTED_BG);
                g2.fill(new RoundRectangle2D.Double(left, 2, w - 2 * left, h - 4, 10, 10));
                g2.setColor(ACCENT);
                g2.fill(new RoundRectangle2D.Double(left, 9, 3.5, h - 18, 3, 3));
            } else if (hover) {
                g2.setColor(HOVER_BG);
                g2.fill(new RoundRectangle2D.Double(left, 2, w - 2 * left, h - 4, 10, 10));
            }

            int iconY = (h - 18) / 2;
            if (imageIcon != null) {
                imageIcon.paintIcon(this, g2, indent, iconY);
            } else {
                vectorIcon.setColor(isSelected ? ACCENT : ICON);
                vectorIcon.paintIcon(this, g2, indent, iconY);
            }

            g2.setFont(isSelected ? F_ITEM_BOLD : F_ITEM);
            g2.setColor(isSelected ? ACCENT_DARK : TEXT);
            FontMetrics fm = g2.getFontMetrics();
            int textX = indent + 18 + 12;
            int maxText = w - textX - (badge > 0 ? 48 : 16);
            g2.drawString(ellipsize(entry.title(), fm, maxText), textX, (h - fm.getHeight()) / 2 + fm.getAscent());

            if (badge > 0) {
                paintBadge(g2, badge, w - 16, h, false);
            }
            if (isFocusOwner()) {
                paintFocus(g2, w, h);
            }
            g2.dispose();
        }
    }

    /** Ô tìm chức năng (bỏ dấu, tìm cả mã UC — ví dụ "uc14", "cong no"). Esc để xóa. */
    private static final class SearchField extends JTextField {
        private final MenuIcon icon = new MenuIcon(MenuIcon.Shape.SEARCH, 16, MUTED);

        SearchField() {
            setOpaque(false);
            setFont(F_ITEM.deriveFont(13f));
            setForeground(TEXT);
            setCaretColor(TEXT);
            setBorder(BorderFactory.createEmptyBorder(0, 34, 0, 10));
            setPreferredSize(new Dimension(WIDTH, 34));
            setToolTipText("Tìm theo tên chức năng hoặc mã use case");
            getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clear");
            getActionMap().put("clear", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { setText(""); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = prepare(g);
            g2.setColor(FIELD_BG);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
            if (isFocusOwner()) {
                g2.setColor(ACCENT);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Double(0.6, 0.6, getWidth() - 1.2, getHeight() - 1.2, 10, 10));
            }
            icon.paintIcon(this, g2, 11, (getHeight() - 16) / 2);
            if (getText().isEmpty()) {
                g2.setFont(getFont());
                g2.setColor(MUTED);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("Tìm chức năng…", 34, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Logo: ảnh truyền vào (bo góc) hoặc ô màu có biểu tượng răng. */
    private static final class LogoIcon implements Icon {
        private final Image image;
        private final int size;
        private final MenuIcon tooth;

        LogoIcon(Image image, int size) {
            this.image = image;
            this.size = size;
            this.tooth = new MenuIcon(MenuIcon.Shape.TOOTH, (int) (size * 0.6), Color.WHITE).withStroke(2f);
        }

        public int getIconWidth() { return size; }
        public int getIconHeight() { return size; }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = prepare(g);
            RoundRectangle2D shape = new RoundRectangle2D.Double(x, y, size, size, 14, 14);
            if (image != null) {
                g2.setClip(shape);
                g2.drawImage(image, x, y, size, size, null);
            } else {
                g2.setPaint(new GradientPaint(x, y, new Color(0x3B82F6), x + size, y + size, ACCENT_DARK));
                g2.fill(shape);
                int t = tooth.getIconWidth();
                tooth.paintIcon(c, g2, x + (size - t) / 2, y + (size - t) / 2);
            }
            g2.dispose();
        }
    }

    /** Thanh cuộn mảnh, không có nút mũi tên. */
    private static final class ThinScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override protected void configureScrollBarColors() {
            thumbColor = new Color(0xD1D5DB);
            trackColor = BG;
        }
        @Override protected javax.swing.JButton createDecreaseButton(int o) { return zeroButton(); }
        @Override protected javax.swing.JButton createIncreaseButton(int o) { return zeroButton(); }
        private static javax.swing.JButton zeroButton() {
            javax.swing.JButton b = new javax.swing.JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(BG);
            g.fillRect(r.x, r.y, r.width, r.height);
        }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty()) {
                return;
            }
            Graphics2D g2 = prepare(g);
            g2.setColor(isDragging ? new Color(0x9CA3AF) : thumbColor);
            g2.fill(new RoundRectangle2D.Double(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 4, 4));
            g2.dispose();
        }
    }

    // ================= Tiện ích =================

    private static Graphics2D prepare(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        return g2;
    }

    /** Vẽ badge dạng viên thuốc, căn phải tại {@code right}. */
    private static void paintBadge(Graphics2D g2, int count, int right, int h, boolean muted) {
        String s = count > 99 ? "99+" : String.valueOf(count);
        g2.setFont(F_BADGE);
        FontMetrics fm = g2.getFontMetrics();
        int bw = Math.max(20, fm.stringWidth(s) + 12);
        int bh = 18;
        int bx = right - bw;
        int by = (h - bh) / 2;
        g2.setColor(muted ? new Color(0xFEE2E2) : BADGE);
        g2.fill(new RoundRectangle2D.Double(bx, by, bw, bh, bh, bh));
        g2.setColor(muted ? BADGE : Color.WHITE);
        g2.drawString(s, bx + (bw - fm.stringWidth(s)) / 2, by + (bh - fm.getHeight()) / 2 + fm.getAscent());
    }

    private static void paintFocus(Graphics2D g2, int w, int h) {
        g2.setColor(ACCENT);
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Double(8.5, 2.5, w - 17, h - 5, 10, 10));
    }

    /** Enter / Space để kích hoạt, mũi tên lên / xuống để di chuyển giữa các mục. */
    private static void bindKeys(JComponent c, Runnable action) {
        c.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "activate");
        c.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "activate");
        c.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "next");
        c.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "prev");
        c.getActionMap().put("activate", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
        c.getActionMap().put("next", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { c.transferFocus(); }
        });
        c.getActionMap().put("prev", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { c.transferFocusBackward(); }
        });
        c.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                c.repaint();
                c.scrollRectToVisible(new Rectangle(0, 0, c.getWidth(), c.getHeight()));
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) { c.repaint(); }
        });
    }

    private static Icon loadImageIcon(String key) {
        URL url = SidebarMenu.class.getResource("/images/menu/" + key + ".png");
        if (url == null) {
            return null;
        }
        Image img = new ImageIcon(url).getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    private static String ellipsize(String s, FontMetrics fm, int max) {
        if (fm.stringWidth(s) <= max) {
            return s;
        }
        String dots = "…";
        int end = s.length();
        while (end > 0 && fm.stringWidth(s.substring(0, end) + dots) > max) {
            end--;
        }
        return s.substring(0, end) + dots;
    }

    /** Bỏ dấu tiếng Việt + chữ thường để tìm kiếm ("Công nợ" → "cong no"). */
    static String normalize(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.replace('đ', 'd').replace('Đ', 'D').toLowerCase();
    }

    private static String pickFontFamily() {
        Set<String> available;
        try {
            available = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        } catch (Exception e) {
            return Font.DIALOG;
        }
        for (String f : new String[]{"Segoe UI", "Inter", "Noto Sans", "Arial"}) {
            if (available.contains(f)) {
                return f;
            }
        }
        return Font.DIALOG;
    }
}
