package vn.n08.nhakhoa.view.component;

import vn.n08.nhakhoa.util.Fmt;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/** Tạo nhanh các ô nhập dùng chung. */
public final class Fields {
    private Fields() {}

    public static HintField text(String hint) {
        return new HintField(hint, 18);
    }

    public static HintField date(LocalDate v) {
        HintField f = new HintField("dd/mm/yyyy", 10);
        if (v != null) f.setText(Fmt.date(v));
        return f;
    }

    /** Ô tiền: tự định dạng 1.200.000 khi rời ô. */
    public static HintField money(BigDecimal v) {
        HintField f = new HintField("0", 12);
        f.setHorizontalAlignment(JLabel.RIGHT);
        if (v != null) f.setText(Fmt.money(v).replace(" ₫", ""));
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) {
                String d = f.getText().replaceAll("[^0-9]", "");
                if (!d.isEmpty() && d.length() < 16) f.setText(Fmt.money(new BigDecimal(d)).replace(" ₫", ""));
            }
        });
        return f;
    }

    public static JTextArea area(int rows) {
        JTextArea a = new JTextArea(rows, 30);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(Theme.BODY);
        return a;
    }

    public static JScrollPane scroll(JTextArea a) {
        JScrollPane s = new JScrollPane(a);
        s.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        return s;
    }

    /** Combo với nhãn tùy biến; phần tử null hiển thị bằng nullLabel. */
    public static <T> JComboBox<T> combo(List<T> items, Function<T, String> label, String nullLabel) {
        JComboBox<T> c = new JComboBox<>();
        if (nullLabel != null) c.addItem(null);
        for (T t : items) c.addItem(t);
        c.setRenderer(new DefaultListCellRenderer() {
            @Override
            @SuppressWarnings("unchecked")
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v == null ? nullLabel : label.apply((T) v), i, s, f);
                setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
                return this;
            }
        });
        c.setFont(Theme.BODY);
        return c;
    }

    public static <T> void setItems(JComboBox<T> c, List<T> items, boolean keepNull) {
        Object sel = c.getSelectedItem();
        c.removeAllItems();
        if (keepNull) c.addItem(null);
        for (T t : items) c.addItem(t);
        if (sel != null) c.setSelectedItem(sel);
    }

    @SuppressWarnings("unchecked")
    public static <T> T selected(JComboBox<T> c) {
        return (T) c.getSelectedItem();
    }
}
