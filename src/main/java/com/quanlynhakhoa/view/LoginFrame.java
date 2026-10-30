package com.quanlynhakhoa.view;

import com.quanlynhakhoa.auth.AuthDecision;
import com.quanlynhakhoa.auth.DesktopLoginGate;
import com.quanlynhakhoa.auth.JdbcAuthService;
import com.quanlynhakhoa.security.DesktopSession;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

/** R3-A3 Swing login. SQL work is off the EDT; no main window without auth. */
public final class LoginFrame extends JFrame {
    private final JdbcAuthService auth;
    private final JTextField txtUsername = new JTextField(22);
    private final JPasswordField txtPassword = new JPasswordField(22);
    private final JButton btnLogin = new JButton("Đăng nhập");
    private final JLabel lblStatus = new JLabel("Chỉ thử nghiệm trên database N08AuthIT_ riêng biệt.");
    private boolean working;

    public LoginFrame(JdbcAuthService auth) {
        super("Nhóm 8 - Đăng nhập quản lý nha khoa");
        if (auth == null) throw new IllegalArgumentException("auth required");
        this.auth = auth;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(480, 300));
        setSize(560, 340);
        setLocationRelativeTo(null);

        JPanel pnlContent = new JPanel(new BorderLayout(12, 16));
        pnlContent.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        JLabel lblTitle = new JLabel("ĐĂNG NHẬP - QUẢN LÝ NHA KHOA");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        pnlContent.add(lblTitle, BorderLayout.NORTH);

        JPanel pnlLabels = new JPanel(new GridLayout(2, 1, 0, 14));
        pnlLabels.add(new JLabel("Tên đăng nhập"));
        pnlLabels.add(new JLabel("Mật khẩu"));
        JPanel pnlFields = new JPanel(new GridLayout(2, 1, 0, 12));
        pnlFields.add(txtUsername);
        pnlFields.add(txtPassword);
        JPanel pnlInput = new JPanel(new BorderLayout(12, 0));
        pnlInput.add(pnlLabels, BorderLayout.WEST);
        pnlInput.add(pnlFields, BorderLayout.CENTER);
        pnlContent.add(pnlInput, BorderLayout.CENTER);

        JPanel pnlBottom = new JPanel(new BorderLayout(0, 10));
        JPanel pnlActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pnlActions.add(btnLogin);
        pnlBottom.add(pnlActions, BorderLayout.NORTH);
        pnlBottom.add(lblStatus, BorderLayout.SOUTH);
        pnlContent.add(pnlBottom, BorderLayout.SOUTH);
        setContentPane(pnlContent);
        getRootPane().setDefaultButton(btnLogin);
        btnLogin.addActionListener(this::submit);
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
                try {
                    AuthDecision decision = get();
                    if (DesktopLoginGate.mayOpenMain(decision)) {
                        DesktopSession session = DesktopSession.fromSuccessfulLogin(decision);
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
                    lblStatus.setText("Không thể kết nối hoặc xác thực. Kiểm tra môi trường thử nghiệm.");
                }
                SwingUtilities.invokeLater(() -> txtPassword.requestFocusInWindow());
            }
        }.execute();
    }
}
