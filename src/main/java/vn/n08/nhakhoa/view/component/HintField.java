package vn.n08.nhakhoa.view.component;

import javax.swing.JTextField;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Ô nhập có chữ gợi ý mờ khi trống. */
public class HintField extends JTextField {
    private String hint;

    public HintField(String hint, int cols) {
        super(cols);
        this.hint = hint;
    }

    public void setHint(String h) { hint = h; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (hint != null && getText().isEmpty() && !isFocusOwner()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Theme.FAINT);
            g2.setFont(getFont());
            var in = getInsets();
            g2.drawString(hint, in.left, getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
            g2.dispose();
        }
    }
}
