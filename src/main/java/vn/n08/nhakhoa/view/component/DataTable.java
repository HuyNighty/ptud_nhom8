package vn.n08.nhakhoa.view.component;

import vn.n08.nhakhoa.model.Labeled;
import vn.n08.nhakhoa.util.Fmt;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Bảng dữ liệu dùng chung: cột khai báo bằng hàm, tự hiển thị badge (Labeled), tiền, ngày;
 * có trạng thái đang tải / trống / lỗi; sắp xếp khi bấm tiêu đề; double-click hoặc Enter để mở.
 */
public class DataTable<T> extends JPanel {
    private record Col<T>(String title, int width, Function<T, Object> value) {}

    private final List<Col<T>> cols = new ArrayList<>();
    private List<T> rows = new ArrayList<>();
    private final Model model = new Model();
    private final JTable table = new JTable(model) {
        /** Giãn kín khung khi đủ chỗ; khi hẹp thì giữ độ rộng cột và cho cuộn ngang. */
        @Override public boolean getScrollableTracksViewportWidth() {
            return getParent() == null || getPreferredSize().width <= getParent().getWidth();
        }
    };
    private final CardLayout cards = new CardLayout();
    private final JLabel stateLabel = new JLabel("", SwingConstants.CENTER);
    private String emptyText = "Không có dữ liệu.";
    private Consumer<T> onOpen;
    private final List<Consumer<T>> onSelect = new ArrayList<>();
    private Function<T, Color> rowTint;
    private boolean autoSelect;

    public DataTable() {
        super();
        setLayout(cards);
        setOpaque(false);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.setRowHeight(32);
        table.setDefaultRenderer(Object.class, new Cell());
        JTableHeader h = table.getTableHeader();
        h.setFont(Theme.SMALL_B);
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(10, 32));
        ((DefaultTableCellRenderer) h.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);
        JScrollPane sp = new JScrollPane(table);
        sp.getViewport().setBackground(Theme.SURFACE);
        sp.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        stateLabel.setFont(Theme.BODY);
        stateLabel.setForeground(Theme.MUTED);
        JPanel state = new JPanel(new BorderLayout());
        state.setBackground(Theme.SURFACE);
        state.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        state.add(stateLabel);
        add(sp, "table");
        add(state, "state");
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && onOpen != null && selected() != null) onOpen.accept(selected());
            }
        });
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open");
        table.getActionMap().put("open", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { if (onOpen != null && selected() != null) onOpen.accept(selected()); }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onSelect.forEach(c -> c.accept(selected()));
        });
        showState(emptyText);
    }

    public DataTable<T> col(String title, int width, Function<T, Object> value) {
        cols.add(new Col<>(title, width, value));
        model.fireTableStructureChanged();
        applyWidths();
        return this;
    }

    public DataTable<T> emptyText(String s) {
        emptyText = s;
        if (rows.isEmpty()) showState(s);
        return this;
    }
    /** Tự chọn dòng đầu khi tải (dùng cho danh sách có khung chi tiết). */
    public DataTable<T> autoSelect() { autoSelect = true; return this; }
    public DataTable<T> onOpen(Consumer<T> c) { onOpen = c; return this; }
    public DataTable<T> onSelect(Consumer<T> c) { onSelect.add(c); return this; }
    /** Tô nền dòng (VD: dòng cảnh báo). */
    public DataTable<T> rowTint(Function<T, Color> f) { rowTint = f; return this; }

    private void applyWidths() {
        TableRowSorter<Model> sorter = new TableRowSorter<>(model);
        for (int i = 0; i < cols.size(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(cols.get(i).width());
            sorter.setComparator(i, CMP);
        }
        table.setRowSorter(sorter);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final Comparator<Object> CMP = (a, b) -> {
        if (a == null) return b == null ? 0 : -1;
        if (b == null) return 1;
        if (a instanceof Comparable ca && a.getClass() == b.getClass()) return ca.compareTo(b);
        return String.valueOf(a).compareTo(String.valueOf(b));
    };

    public void setRows(List<T> data) {
        T keep = selected();
        rows = new ArrayList<>(data);
        model.fireTableDataChanged();
        if (rows.isEmpty()) showState(emptyText);
        else {
            cards.show(this, "table");
            if (keep != null) select(keep::equals);
            if (selected() == null && autoSelect) table.setRowSelectionInterval(0, 0);
        }
    }

    public List<T> rows() { return rows; }

    public void loading() { showState("Đang tải dữ liệu…"); }

    public void error(String msg) { showState("<html><div style='text-align:center;color:#DC2626'>Không tải được dữ liệu.<br>" + esc(msg) + "</div></html>"); }

    private void showState(String s) {
        stateLabel.setText(s);
        cards.show(this, "state");
    }

    public T selected() {
        int v = table.getSelectedRow();
        if (v < 0) return null;
        int m = table.convertRowIndexToModel(v);
        return m < rows.size() ? rows.get(m) : null;
    }

    public void select(java.util.function.Predicate<T> p) {
        for (int i = 0; i < rows.size(); i++) {
            if (p.test(rows.get(i))) {
                int v = table.convertRowIndexToView(i);
                if (v >= 0) {
                    table.setRowSelectionInterval(v, v);
                    table.scrollRectToVisible(table.getCellRect(v, 0, true));
                }
                return;
            }
        }
    }

    public JTable table() { return table; }

    static String esc(String s) { return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }

    private class Model extends AbstractTableModel {
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.size(); }
        @Override public String getColumnName(int c) { return cols.get(c).title(); }
        @Override public Object getValueAt(int r, int c) { return cols.get(c).value().apply(rows.get(r)); }
    }

    private class Cell implements TableCellRenderer {
        private final DefaultTableCellRenderer text = new DefaultTableCellRenderer();
        private final Badge badge = new Badge();
        private final JPanel badgeWrap = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 5));

        Cell() {
            badgeWrap.add(badge);
            text.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
            Color bg = sel ? Theme.PRI_SOFT : Theme.SURFACE;
            if (!sel && rowTint != null) {
                Color tint = rowTint.apply(rows.get(t.convertRowIndexToModel(r)));
                if (tint != null) bg = tint;
            }
            if (v instanceof Labeled l) {
                badge.set(l);
                badgeWrap.setBackground(bg);
                return badgeWrap;
            }
            text.getTableCellRendererComponent(t, format(v), sel, false, r, c);
            text.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            text.setBackground(bg);
            text.setForeground(Theme.INK);
            text.setFont(Theme.BODY);
            text.setHorizontalAlignment(v instanceof Number ? SwingConstants.RIGHT : SwingConstants.LEFT);
            text.setToolTipText(v instanceof String s && s.length() > 40 ? "<html><div style='width:320px'>" + esc(s) + "</div></html>" : null);
            return text;
        }
    }

    static String format(Object v) {
        if (v == null) return "—";
        if (v instanceof BigDecimal b) return Fmt.money(b);
        if (v instanceof LocalDateTime d) return Fmt.dateTime(d);
        if (v instanceof LocalDate d) return Fmt.date(d);
        if (v instanceof LocalTime d) return Fmt.time(d);
        if (v instanceof Boolean b) return b ? "Có" : "";
        String s = v.toString();
        return s.isBlank() ? "—" : s;
    }
}
