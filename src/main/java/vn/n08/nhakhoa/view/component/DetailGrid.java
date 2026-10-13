package vn.n08.nhakhoa.view.component;

import vn.n08.nhakhoa.model.Labeled;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Lưới hiển thị cặp nhãn – giá trị (chỉ đọc). */
public class DetailGrid extends JPanel {
    private final GridBagConstraints g = new GridBagConstraints();
    private int n;
    private final int pairsPerRow;

    public DetailGrid(int pairsPerRow) {
        super(new GridBagLayout());
        this.pairsPerRow = pairsPerRow;
        setOpaque(false);
        g.anchor = GridBagConstraints.NORTHWEST;
        g.fill = GridBagConstraints.HORIZONTAL;
    }

    public DetailGrid row(String label, Object value) {
        JComponent v;
        if (value instanceof Labeled l) v = new Badge(l);
        else if (value instanceof JComponent c) v = c;
        else {
            JLabel x = new JLabel("<html><div style='width:220px'>" + DataTable.esc(DataTable.format(value)) + "</div></html>");
            x.setFont(Theme.BODY);
            x.setForeground(Theme.INK);
            v = x;
        }
        JLabel k = new JLabel(label);
        k.setFont(Theme.SMALL);
        k.setForeground(Theme.MUTED);
        int col = (n % pairsPerRow) * 2;
        g.gridy = n / pairsPerRow;
        g.gridx = col;
        g.weightx = 0;
        g.insets = new Insets(3, col == 0 ? 0 : 18, 3, 8);
        add(k, g);
        g.gridx = col + 1;
        g.weightx = 1;
        g.insets = new Insets(3, 0, 3, 0);
        add(v, g);
        n++;
        return this;
    }

    public void clear() {
        removeAll();
        n = 0;
    }
}
