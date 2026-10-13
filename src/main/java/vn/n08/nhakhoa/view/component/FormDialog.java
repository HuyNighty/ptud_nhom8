package vn.n08.nhakhoa.view.component;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.controller.ErrorHandler;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.CanhBaoException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.util.function.Supplier;

/**
 * Hộp thoại nhập liệu dùng chung: tiêu đề, form, nút Hủy / nút chính.
 * prepare() đọc ô nhập trên luồng giao diện và trả về công việc gọi Service; công việc chạy nền.
 * Lỗi nghiệp vụ hiện ngay dưới ô tương ứng (theo BusinessException.field) hoặc ở dải báo lỗi.
 * CanhBaoException → hỏi xác nhận rồi chạy lại với cờ {@link #daXacNhan}.
 */
public abstract class FormDialog<R> extends JDialog {
    protected final FormGrid form;
    protected boolean daXacNhan;
    private final JLabel banner = new JLabel();
    private final Btn ok;
    private R result;

    protected FormDialog(Component parent, String title, String sub, String okText, int cols) {
        super(parent == null ? null : (parent instanceof java.awt.Window w ? w : SwingUtilities.getWindowAncestor(parent)), title, ModalityType.APPLICATION_MODAL);
        form = new FormGrid(cols);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.SURFACE);
        JPanel head = new JPanel();
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setBackground(Theme.SURFACE);
        head.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE), BorderFactory.createEmptyBorder(16, 20, 12, 20)));
        JLabel t = new JLabel(title);
        t.setFont(Theme.H2);
        t.setForeground(Theme.INK);
        head.add(t);
        if (sub != null) {
            JLabel s = new JLabel("<html>" + DataTable.esc(sub) + "</html>");
            s.setFont(Theme.SMALL);
            s.setForeground(Theme.MUTED);
            head.add(s);
        }
        add(head, BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setBackground(Theme.SURFACE);
        body.setBorder(BorderFactory.createEmptyBorder(14, 20, 6, 20));
        banner.setOpaque(true);
        banner.setBackground(Theme.RED_S);
        banner.setForeground(Theme.RED);
        banner.setFont(Theme.BODY);
        banner.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        banner.setVisible(false);
        body.add(banner, BorderLayout.NORTH);
        body.add(form, BorderLayout.CENTER);
        JScrollPane sp = new JScrollPane(body);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        add(sp, BorderLayout.CENTER);
        ok = Btn.primary(okText, e -> submit());
        add(Dialogs.footer(Btn.secondary("Hủy bỏ", e -> dispose()), ok), BorderLayout.SOUTH);
        getRootPane().setDefaultButton(null);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** Đọc ô nhập, kiểm tra định dạng (ném BusinessException) và trả về lời gọi Service. */
    protected abstract Supplier<R> prepare();

    protected String okMessage(R r) { return null; }

    protected void submit() {
        form.clearErrors();
        banner.setVisible(false);
        Supplier<R> work;
        try {
            work = prepare();
        } catch (BusinessException e) {
            showError(e);
            return;
        }
        ok.setEnabled(false);
        Async.run(this, work, r -> {
            result = r;
            String m = okMessage(r);
            if (m != null) Toast.show(getOwner(), m);
            dispose();
        }, e -> {
            ok.setEnabled(true);
            if (e instanceof CanhBaoException cb) {
                if (Dialogs.warnings(this, cb, "Vẫn lưu")) {
                    daXacNhan = true;
                    submit();
                }
            } else if (e instanceof BusinessException be) {
                showError(be);
            } else {
                showBanner(ErrorHandler.message(e));
            }
        });
    }

    private void showError(BusinessException e) {
        if (!form.showError(e.field(), e.getMessage())) showBanner(e.getMessage());
        pack();
    }

    protected void showBanner(String msg) {
        banner.setText("<html><div style='width:460px'>" + DataTable.esc(msg) + "</div></html>");
        banner.setVisible(true);
        pack();
    }

    /** Hiển thị modal và trả kết quả (null nếu hủy). */
    public R open() {
        pack();
        Dimension s = getSize();
        setSize(Math.max(s.width, 520), Math.min(s.height, 760));
        setLocationRelativeTo(getOwner());
        setVisible(true);
        return result;
    }

    // ---- tiện ích đọc ô ----
    protected static String txt(javax.swing.text.JTextComponent c) {
        return c.getText() == null ? "" : c.getText().trim();
    }
}
