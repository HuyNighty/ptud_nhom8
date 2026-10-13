package vn.n08.nhakhoa.view.component;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/** FlowLayout tự xuống dòng và báo đúng chiều cao khi hàng nút / bộ lọc không đủ chỗ. */
public class WrapLayout extends FlowLayout {
    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override public Dimension preferredLayoutSize(Container target) { return layoutSize(target, true); }

    @Override public Dimension minimumLayoutSize(Container target) {
        Dimension d = layoutSize(target, false);
        d.width -= getHgap() + 1;
        return d;
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getSize().width;
            Container c = target;
            while (c.getSize().width == 0 && c.getParent() != null) c = c.getParent();
            if (targetWidth == 0) targetWidth = c.getSize().width;
            if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
            Insets in = target.getInsets();
            int maxWidth = targetWidth - (in.left + in.right + getHgap() * 2);
            Dimension dim = new Dimension(0, 0);
            int rowW = 0, rowH = 0;
            for (Component m : target.getComponents()) {
                if (!m.isVisible()) continue;
                Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                if (rowW + d.width > maxWidth) {
                    add(dim, rowW, rowH);
                    rowW = 0;
                    rowH = 0;
                }
                if (rowW != 0) rowW += getHgap();
                rowW += d.width;
                rowH = Math.max(rowH, d.height);
            }
            add(dim, rowW, rowH);
            dim.width += in.left + in.right + getHgap() * 2;
            dim.height += in.top + in.bottom + getVgap() * 2;
            Container sp = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
            if (sp != null && target.isValid()) dim.width -= getHgap() + 1;
            return dim;
        }
    }

    private void add(Dimension dim, int rowW, int rowH) {
        dim.width = Math.max(dim.width, rowW);
        if (dim.height > 0) dim.height += getVgap();
        dim.height += rowH;
    }
}
