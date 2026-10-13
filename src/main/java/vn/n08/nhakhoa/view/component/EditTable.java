package vn.n08.nhakhoa.view.component;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Bảng có cột nhập liệu (VD: số lượng thực tế khi kiểm kê / kiểm tra / hoàn trả).
 * Cột chỉ đọc khai báo bằng hàm; cột nhập là chuỗi, được đọc lại qua {@link #input(int, int)}.
 */
public class EditTable<T> extends JPanel {
    private record Col<T>(String title, int width, Function<T, Object> value, boolean edit) {}

    private final List<Col<T>> cols = new ArrayList<>();
    private List<T> rows = new ArrayList<>();
    private final List<String[]> inputs = new ArrayList<>();
    private final Model model = new Model();
    private final JTable table = new JTable(model);

    public EditTable() {
        super(new BorderLayout());
        setOpaque(false);
        table.setRowHeight(30);
        table.setSurrendersFocusOnKeystroke(true);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.getTableHeader().setFont(Theme.SMALL_B);
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, DataTable.format(v), s, f, r, c);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                setBackground(cols.get(c).edit() ? Theme.AMBER_S : s ? Theme.PRI_SOFT : Theme.SURFACE);
                setForeground(Theme.INK);
                return this;
            }
        });
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        sp.getViewport().setBackground(Theme.SURFACE);
        add(sp);
    }

    public EditTable<T> col(String title, int width, Function<T, Object> f) {
        cols.add(new Col<>(title, width, f, false));
        return this;
    }

    /** Cột nhập; giá trị ban đầu lấy từ hàm (có thể null). */
    public EditTable<T> input(String title, int width, Function<T, Object> initial) {
        cols.add(new Col<>(title, width, initial, true));
        return this;
    }

    public void setRows(List<T> data) {
        rows = new ArrayList<>(data);
        inputs.clear();
        for (T r : rows) {
            String[] in = new String[cols.size()];
            for (int c = 0; c < cols.size(); c++) {
                if (cols.get(c).edit()) {
                    Object v = cols.get(c).value().apply(r);
                    in[c] = v == null ? "" : DataTable.format(v).equals("—") ? "" : DataTable.format(v);
                }
            }
            inputs.add(in);
        }
        model.fireTableStructureChanged();
        for (int i = 0; i < cols.size(); i++) table.getColumnModel().getColumn(i).setPreferredWidth(cols.get(i).width());
    }

    public List<T> rows() { return rows; }

    /** Giá trị đã nhập ở dòng r, cột c (thứ tự khai báo). */
    public String input(int r, int c) {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        String s = inputs.get(r)[c];
        return s == null ? "" : s.trim();
    }

    public JTable table() { return table; }

    private class Model extends AbstractTableModel {
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.size(); }
        @Override public String getColumnName(int c) { return cols.get(c).title(); }
        @Override public boolean isCellEditable(int r, int c) { return cols.get(c).edit(); }
        @Override public Object getValueAt(int r, int c) { return cols.get(c).edit() ? inputs.get(r)[c] : cols.get(c).value().apply(rows.get(r)); }
        @Override public void setValueAt(Object v, int r, int c) { inputs.get(r)[c] = v == null ? "" : v.toString(); }
    }
}
