package vn.n08.nhakhoa.view.component;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

/** Bố cục dùng chung. */
public final class Layouts {
    private Layouts() {}

    /** Danh sách bên trái co giãn + khung chi tiết bên phải rộng cố định. */
    public static JPanel masterDetail(JComponent master, JComponent detail, int detailWidth) {
        JPanel p = new JPanel(new BorderLayout(14, 0));
        p.setOpaque(false);
        p.add(master, BorderLayout.CENTER);
        JPanel track = new TrackWidth();
        track.add(detail);
        JScrollPane sp = new JScrollPane(track) {
            /** Khung chi tiết rộng tối đa detailWidth nhưng không quá 46% bề ngang để danh sách còn đủ chỗ. */
            @Override public Dimension getPreferredSize() {
                int avail = p.getWidth() > 0 ? p.getWidth() : detailWidth * 2;
                return new Dimension(Math.min(detailWidth, (int) (avail * 0.46)), super.getPreferredSize().height);
            }
        };
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        p.add(sp, BorderLayout.EAST);
        return p;
    }

    /** Hàng các thành phần chia đều (thẻ số liệu). */
    public static JPanel row(int gap, JComponent... cs) {
        JPanel p = new JPanel(new GridLayout(1, cs.length, gap, 0));
        p.setOpaque(false);
        for (JComponent c : cs) p.add(c);
        return p;
    }

    /** Khối dọc: phần trên (toolbar) + phần dưới (bảng). */
    public static JPanel stack(JComponent top, JComponent center) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        if (top != null) p.add(top, BorderLayout.NORTH);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    public static JPanel pad(JComponent c, int t, int l, int b, int r) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(t, l, b, r));
        p.add(c);
        return p;
    }

    /** Ghi chú có nền màu (thông tin / cảnh báo). */
    public static JComponent note(String html, boolean warn) {
        javax.swing.JLabel l = new javax.swing.JLabel("<html><div style='width:100%'>" + html + "</div></html>");
        l.setOpaque(true);
        l.setBackground(warn ? Theme.AMBER_S : Theme.PRI_SOFT);
        l.setForeground(warn ? Theme.AMBER : Theme.PRI_INK);
        l.setFont(Theme.SMALL);
        l.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        return l;
    }

    /** Panel trong thanh cuộn luôn vừa bề ngang khung (chỉ cuộn dọc). */
    public static class TrackWidth extends JPanel implements javax.swing.Scrollable {
        public TrackWidth() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(java.awt.Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(java.awt.Rectangle r, int o, int d) { return r.height; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
