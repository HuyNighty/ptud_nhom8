package vn.n08.nhakhoa.view.component;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;

/**
 * Lưới form 2 cột (nhãn trên, ô dưới) có dòng báo lỗi ngay dưới từng ô.
 * Tên ô (field) khớp BusinessException.field() để đánh dấu lỗi đúng chỗ.
 */
public class FormGrid extends JPanel {
    private final GridBagConstraints g = new GridBagConstraints();
    private final Map<String, JLabel> errors = new HashMap<>();
    private final Map<String, JComponent> inputs = new HashMap<>();
    private int col;
    private final int cols;

    public FormGrid(int cols) {
        super(new GridBagLayout());
        this.cols = cols;
        setOpaque(false);
        g.gridy = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.weightx = 1;
    }

    public FormGrid add(String field, String label, JComponent input) {
        return add(field, label, input, 1);
    }

    /** span = số cột chiếm (1..cols). */
    public FormGrid add(String field, String label, JComponent input, int span) {
        if (col + span > cols) newRow();
        JPanel cell = new JPanel(new java.awt.BorderLayout(0, 3));
        cell.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(Theme.SMALL_B);
        l.setForeground(Theme.INK2);
        cell.add(l, java.awt.BorderLayout.NORTH);
        cell.add(input, java.awt.BorderLayout.CENTER);
        JLabel err = new JLabel(" ");
        err.setFont(Theme.SMALL);
        err.setForeground(Theme.RED);
        cell.add(err, java.awt.BorderLayout.SOUTH);
        if (field != null) {
            errors.put(field, err);
            inputs.put(field, input);
        }
        g.gridx = col;
        g.gridwidth = span;
        g.insets = new Insets(0, col == 0 ? 0 : 12, 4, 0);
        super.add(cell, g);
        col += span;
        if (col >= cols) newRow();
        return this;
    }

    /** Thêm thành phần chiếm trọn hàng (ghi chú, cảnh báo). */
    public FormGrid full(JComponent c) {
        if (col > 0) newRow();
        g.gridx = 0;
        g.gridwidth = cols;
        g.insets = new Insets(0, 0, 8, 0);
        super.add(c, g);
        newRow();
        return this;
    }

    private void newRow() {
        col = 0;
        g.gridy++;
    }

    public void clearErrors() {
        errors.values().forEach(l -> l.setText(" "));
    }

    /** Hiển thị lỗi cho ô; trả false nếu không có ô tên đó. */
    public boolean showError(String field, String msg) {
        JLabel l = field == null ? null : errors.get(field);
        if (l == null) return false;
        l.setText(msg);
        JComponent in = inputs.get(field);
        if (in != null) in.requestFocusInWindow();
        return true;
    }
}
