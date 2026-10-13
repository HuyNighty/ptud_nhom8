package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.view.component.PageHeader;
import vn.n08.nhakhoa.view.component.Theme;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** Một màn hình chức năng trong khung chính (CardLayout). reload() được gọi mỗi lần mở màn hình. */
public abstract class Page extends JPanel implements javax.swing.Scrollable {
    protected final PageHeader header;

    protected Page(String title, String sub) {
        super(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        header = new PageHeader(title, sub);
        add(header, BorderLayout.NORTH);
    }

    protected void body(JComponent c) {
        add(c, BorderLayout.CENTER);
    }

    // ---- Scrollable: luôn vừa chiều ngang khung; chiều dọc giãn theo cửa sổ, cuộn khi màn hình thấp ----
    @Override public java.awt.Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(java.awt.Rectangle r, int o, int d) { return 18; }
    @Override public int getScrollableBlockIncrement(java.awt.Rectangle r, int o, int d) { return r.height - 40; }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof javax.swing.JViewport v && v.getHeight() > Math.max(getMinimumHeight(), 560);
    }

    /** Chiều cao tối thiểu trước khi hiện thanh cuộn dọc. */
    protected int getMinimumHeight() { return 640; }

    /** Tải lại dữ liệu từ CSDL. */
    public abstract void reload();
}
