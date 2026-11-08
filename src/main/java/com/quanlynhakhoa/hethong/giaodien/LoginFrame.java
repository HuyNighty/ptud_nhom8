package com.quanlynhakhoa.hethong.giaodien;

import com.quanlynhakhoa.hethong.xacthuc.AuthDecision;
import com.quanlynhakhoa.hethong.xacthuc.DesktopLoginGate;
import com.quanlynhakhoa.hethong.xacthuc.JdbcAuthService;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.GeneralPath;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

/** Dental visual refresh; existing JDBC authentication and session gate are unchanged. */
public final class LoginFrame extends JFrame {
    private static final Color NAVY = new Color(21, 55, 92);
    private static final Color BLUE = new Color(12, 112, 181);
    private static final Color MUTED = new Color(105, 125, 145);
    private static final Color BORDER = new Color(205, 220, 232);

    private final JdbcAuthService auth;
    private final JTextField txtUsername = new JTextField(22);
    private final JPasswordField txtPassword = new JPasswordField(22);
    private final JButton btnLogin = new JButton("ĐĂNG NHẬP");
    private final JLabel lblStatus = new JLabel("Đăng nhập bằng tài khoản được phòng khám cấp.");
    private final JCheckBox chkShowPassword = new JCheckBox("Hiện mật khẩu");
    private boolean working;

    public LoginFrame(JdbcAuthService auth) {
        super("Nhóm 8 - Đăng nhập quản lý nha khoa");
        if (auth == null) throw new IllegalArgumentException("auth required");
        this.auth = auth;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(850, 590));
        setSize(1060, 680);
        setLocationRelativeTo(null);

        JPanel pnlRoot = new JPanel(new GridBagLayout());
        pnlRoot.setBackground(Color.WHITE);
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = 0;
        left.weightx = 0.45;
        left.weighty = 1;
        left.fill = GridBagConstraints.BOTH;
        pnlRoot.add(new DentalPhotoPanel(), left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = 0;
        right.weightx = 0.55;
        right.weighty = 1;
        right.fill = GridBagConstraints.BOTH;
        pnlRoot.add(createLoginPanel(), right);
        setContentPane(pnlRoot);

        getRootPane().setDefaultButton(btnLogin);
        btnLogin.addActionListener(this::submit);
        char originalEcho = txtPassword.getEchoChar();
        chkShowPassword.addActionListener(event ->
                txtPassword.setEchoChar(chkShowPassword.isSelected() ? (char) 0 : originalEcho));
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent event) {
                txtUsername.requestFocusInWindow();
            }
        });
    }

    private JPanel createLoginPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new EmptyBorder(30, 32, 30, 32));

        JPanel form = new JPanel();
        form.setBackground(Color.WHITE);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(392, 505));

        JLabel brand = new JLabel("✦   N08 DENTAL  /  QUẢN LÝ PHÒNG KHÁM");
        brand.setFont(new Font("SansSerif", Font.BOLD, 12));
        brand.setForeground(BLUE);
        brand.setAlignmentX(LEFT_ALIGNMENT);
        form.add(brand);
        form.add(Box.createVerticalStrut(30));

        JLabel heading = new JLabel("Chào mừng trở lại");
        heading.setFont(new Font("SansSerif", Font.BOLD, 28));
        heading.setForeground(NAVY);
        heading.setAlignmentX(LEFT_ALIGNMENT);
        form.add(heading);
        form.add(Box.createVerticalStrut(10));

        JLabel subtitle = new JLabel("Đăng nhập để tiếp tục công việc tại phòng khám.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(MUTED);
        subtitle.setAlignmentX(LEFT_ALIGNMENT);
        form.add(subtitle);
        form.add(Box.createVerticalStrut(26));

        form.add(fieldLabel("Tên đăng nhập", txtUsername));
        form.add(Box.createVerticalStrut(9));
        styleField(txtUsername);
        txtUsername.getAccessibleContext().setAccessibleName("Tên đăng nhập");
        form.add(txtUsername);
        form.add(Box.createVerticalStrut(18));

        form.add(fieldLabel("Mật khẩu", txtPassword));
        form.add(Box.createVerticalStrut(9));
        styleField(txtPassword);
        txtPassword.getAccessibleContext().setAccessibleName("Mật khẩu");
        form.add(txtPassword);
        form.add(Box.createVerticalStrut(6));

        chkShowPassword.setFont(new Font("SansSerif", Font.PLAIN, 12));
        chkShowPassword.setForeground(MUTED);
        chkShowPassword.setOpaque(false);
        chkShowPassword.setFocusPainted(false);
        chkShowPassword.setAlignmentX(LEFT_ALIGNMENT);
        form.add(chkShowPassword);
        form.add(Box.createVerticalStrut(18));

        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 49));
        btnLogin.setPreferredSize(new Dimension(392, 49));
        btnLogin.setAlignmentX(LEFT_ALIGNMENT);
        btnLogin.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnLogin.getAccessibleContext().setAccessibleName("Đăng nhập");
        btnLogin.putClientProperty("theme.primary", true);
        AppTheme.applyButtons(btnLogin);
        form.add(btnLogin);
        form.add(Box.createVerticalStrut(17));

        lblStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblStatus.setForeground(MUTED);
        lblStatus.setVerticalAlignment(SwingConstants.TOP);
        lblStatus.setAlignmentX(LEFT_ALIGNMENT);
        lblStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        lblStatus.setPreferredSize(new Dimension(392, 58));
        lblStatus.getAccessibleContext().setAccessibleName("Trạng thái đăng nhập");
        form.add(lblStatus);
        form.add(Box.createVerticalStrut(20));

        JLabel footer = new JLabel("HỆ THỐNG QUẢN LÝ NHA KHOA  ·  NHÓM 8");
        footer.setFont(new Font("SansSerif", Font.PLAIN, 11));
        footer.setForeground(new Color(146, 161, 174));
        footer.setAlignmentX(LEFT_ALIGNMENT);
        form.add(footer);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.CENTER;
        wrapper.add(form, constraints);
        return wrapper;
    }

    private static JLabel fieldLabel(String text, JTextField field) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(NAVY);
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setLabelFor(field);
        return label;
    }

    private static void styleField(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 15));
        field.setForeground(NAVY);
        field.setBackground(new Color(250, 252, 255));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(11, 14, 11, 14)));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        field.setPreferredSize(new Dimension(392, 48));
        field.setAlignmentX(LEFT_ALIGNMENT);
    }

    private static final class DentalPhotoPanel extends JPanel {
        private final Image image;

        private DentalPhotoPanel() {
            setPreferredSize(new Dimension(450, 620));
            Image loaded = null;
            try {
                URL url = LoginFrame.class.getResource("/images/dental-login.jpg");
                if (url != null) loaded = ImageIO.read(url);
            } catch (Exception ignored) {
                // Retain a clean blue fallback if an installation loses its resource.
            }
            image = loaded;
            getAccessibleContext().setAccessibleName("Không gian phòng khám nha khoa");
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                int w = getWidth(), h = getHeight();
                g.setColor(new Color(19, 98, 154));
                g.fillRect(0, 0, w, h);
                if (image != null) {
                    int iw = image.getWidth(this), ih = image.getHeight(this);
                    if (iw > 0 && ih > 0) {
                        double scale = Math.max((double) w / iw, (double) h / ih);
                        int width = (int) Math.ceil(iw * scale);
                        int height = (int) Math.ceil(ih * scale);
                        g.drawImage(image, (w - width) / 2, (h - height) / 2, width, height, this);
                    }
                }
                g.setPaint(new GradientPaint(0, h * 0.42f,
                        new Color(8, 45, 86, 5), 0, h,
                        new Color(7, 44, 82, 229)));
                g.fillRect(0, 0, w, h);
                int x = 38;
                int baseline = h - 154;
                g.setColor(new Color(214, 242, 251));
                g.fillRoundRect(x, baseline - 108, 67, 4, 4, 4);
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, 28));
                g.drawString("Tận tâm trong", x, baseline - 40);
                g.drawString("từng nụ cười.", x, baseline - 5);

            } finally {
                g.dispose();
            }
        }
    }

    private void submit(ActionEvent ignored) {
        if (working) return;
        String username = txtUsername.getText().trim();
        char[] chars = txtPassword.getPassword();
        txtPassword.setText("");
        if (username.isEmpty() || chars.length == 0) {
            Arrays.fill(chars, '\0');
            lblStatus.setText("Vui lòng nhập tên đăng nhập và mật khẩu.");
            return;
        }
        working = true;
        btnLogin.setEnabled(false);
        txtUsername.setEnabled(false);
        txtPassword.setEnabled(false);
        chkShowPassword.setEnabled(false);
        lblStatus.setText("Đang xác thực...");

        new SwingWorker<AuthDecision, Void>() {
            @Override protected AuthDecision doInBackground() throws Exception {
                try {
                    // API currently accepts String. A short-lived immutable copy is unavoidable.
                    return auth.authenticate(username, new String(chars));
                } finally {
                    Arrays.fill(chars, '\0');
                }
            }
            @Override protected void done() {
                // SwingWorker.done() runs on the Event Dispatch Thread.
                working = false;
                btnLogin.setEnabled(true);
                txtUsername.setEnabled(true);
                txtPassword.setEnabled(true);
                chkShowPassword.setEnabled(true);
                try {
                    AuthDecision decision = get();
                    if (DesktopLoginGate.mayOpenMain(decision)) {
                        DesktopSession session = DesktopSession.fromSuccessfulLogin(decision);
                        auth.bindSession(session);
                        MainFrame winMain = new MainFrame(session,
                                () -> new LoginFrame(auth).setVisible(true));
                        winMain.setVisible(true);
                        dispose();
                    } else if (decision.authenticated() && decision.forcePasswordChange()) {
                        lblStatus.setText("Tài khoản yêu cầu đổi mật khẩu.");
                        JOptionPane.showMessageDialog(LoginFrame.this,
                                "Chức năng đổi mật khẩu chưa được triển khai. Không thể mở ứng dụng.",
                                "Cần đổi mật khẩu", JOptionPane.WARNING_MESSAGE);
                    } else {
                        lblStatus.setText("Tên đăng nhập hoặc mật khẩu không hợp lệ, hoặc tài khoản bị hạn chế.");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    lblStatus.setText("Không thể hoàn tất đăng nhập.");
                } catch (ExecutionException | CancellationException ex) {
                    // Do not surface JDBC internals or credentials in UI / console.
                    ex.printStackTrace();
                    lblStatus.setText("Không thể kết nối hoặc xác thực. Kiểm tra cấu hình cơ sở dữ liệu.");
                }
                SwingUtilities.invokeLater(() -> txtPassword.requestFocusInWindow());
            }
        }.execute();
    }
}
