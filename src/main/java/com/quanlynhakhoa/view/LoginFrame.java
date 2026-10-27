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
    private final JTextField usernameField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);
    private final JButton loginButton = new JButton("Đăng nhập");
    private final JLabel statusLabel = new JLabel("Chỉ thử nghiệm trên database N08AuthIT_ riêng biệt.");
    private boolean working;

    public LoginFrame(JdbcAuthService auth) {
        super("Nhóm 8 - Đăng nhập quản lý nha khoa");
        if (auth == null) throw new IllegalArgumentException("auth required");
        this.auth = auth;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(480, 300));
        setSize(560, 340);
        setLocationRelativeTo(null);

        JPanel content = new JPanel(new BorderLayout(12, 16));
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        JLabel title = new JLabel("ĐĂNG NHẬP - QUẢN LÝ NHA KHOA");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        content.add(title, BorderLayout.NORTH);

        JPanel labels = new JPanel(new GridLayout(2, 1, 0, 14));
        labels.add(new JLabel("Tên đăng nhập"));
        labels.add(new JLabel("Mật khẩu"));
        JPanel fields = new JPanel(new GridLayout(2, 1, 0, 12));
        fields.add(usernameField);
        fields.add(passwordField);
        JPanel inputPanel = new JPanel(new BorderLayout(12, 0));
        inputPanel.add(labels, BorderLayout.WEST);
        inputPanel.add(fields, BorderLayout.CENTER);
        content.add(inputPanel, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 10));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.add(loginButton);
        bottom.add(actions, BorderLayout.NORTH);
        bottom.add(statusLabel, BorderLayout.SOUTH);
        content.add(bottom, BorderLayout.SOUTH);
        setContentPane(content);
        getRootPane().setDefaultButton(loginButton);
        loginButton.addActionListener(this::submit);
    }

    private void submit(ActionEvent ignored) {
        if (working) return;
        String username = usernameField.getText().trim();
        char[] chars = passwordField.getPassword();
        passwordField.setText("");
        if (username.isEmpty() || chars.length == 0) {
            Arrays.fill(chars, '\0');
            statusLabel.setText("Vui lòng nhập tên đăng nhập và mật khẩu.");
            return;
        }
        working = true;
        loginButton.setEnabled(false);
        usernameField.setEnabled(false);
        passwordField.setEnabled(false);
        statusLabel.setText("Đang xác thực...");

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
                loginButton.setEnabled(true);
                usernameField.setEnabled(true);
                passwordField.setEnabled(true);
                try {
                    AuthDecision decision = get();
                    if (DesktopLoginGate.mayOpenMain(decision)) {
                        DesktopSession session = DesktopSession.fromSuccessfulLogin(decision);
                        MainFrame main = new MainFrame(session,
                                () -> new LoginFrame(auth).setVisible(true));
                        main.setVisible(true);
                        dispose();
                    } else if (decision.authenticated() && decision.forcePasswordChange()) {
                        statusLabel.setText("Tài khoản yêu cầu đổi mật khẩu.");
                        JOptionPane.showMessageDialog(LoginFrame.this,
                                "Chức năng đổi mật khẩu chưa được triển khai. Không thể mở ứng dụng.",
                                "Cần đổi mật khẩu", JOptionPane.WARNING_MESSAGE);
                    } else {
                        statusLabel.setText("Tên đăng nhập hoặc mật khẩu không hợp lệ, hoặc tài khoản bị hạn chế.");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("Không thể hoàn tất đăng nhập.");
                } catch (ExecutionException | CancellationException ex) {
                    // Do not surface JDBC internals or credentials in UI / console.
                    statusLabel.setText("Không thể kết nối hoặc xác thực. Kiểm tra môi trường thử nghiệm.");
                }
                SwingUtilities.invokeLater(() -> passwordField.requestFocusInWindow());
            }
        }.execute();
    }
}
