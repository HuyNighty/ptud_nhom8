package com.quanlynhakhoa.billing.ui;

import com.quanlynhakhoa.auth.*;
import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.dichvu.ui.DichVuPanel;
import com.quanlynhakhoa.security.*;
import java.awt.*;
import java.awt.event.*;
import java.time.Clock;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import javax.swing.*;

/** Feature-only harness. Delegates real authentication to the existing JdbcAuthService. */
public final class ServiceBillingLauncher {
    private ServiceBillingLauncher() { }
    public static void main(String[] args) {
        if (!TeamDevConnection.hasLocalConfig()) {
            System.err.println("Thiếu .n08-dev.local.properties của database N08Dev đã onboarding; launcher không dùng sandbox khác.");
            System.exit(2);
        }
        SwingUtilities.invokeLater(new Runnable() { public void run() { login(); } });
    }
    private static void login() {
        final JFrame winLogin = new JFrame("Khang · Đăng nhập Service & Billing M1");
        final JTextField txtUsername = new JTextField(22);
        final JPasswordField txtPassword = new JPasswordField(22);
        final JLabel lblStatus = new JLabel("Dùng tài khoản Swing thật trên database development.");
        final JButton btnLogin = new JButton("Đăng nhập");
        JPanel pnlForm = new JPanel(new GridLayout(0, 1, 8, 8)); pnlForm.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        pnlForm.add(new JLabel("Tên đăng nhập")); pnlForm.add(txtUsername); pnlForm.add(new JLabel("Mật khẩu"));
        pnlForm.add(txtPassword); pnlForm.add(btnLogin); pnlForm.add(lblStatus);
        winLogin.setContentPane(pnlForm); winLogin.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        winLogin.pack(); winLogin.setLocationRelativeTo(null); winLogin.setVisible(true);
        winLogin.getRootPane().setDefaultButton(btnLogin);
        btnLogin.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (!btnLogin.isEnabled()) return;
                final String username = txtUsername.getText().trim(); final char[] password = txtPassword.getPassword();
                txtPassword.setText(""); btnLogin.setEnabled(false); txtUsername.setEnabled(false); txtPassword.setEnabled(false);
                lblStatus.setText("Đang xác thực...");
                new SwingWorker<AuthDecision, Void>() {
                    protected AuthDecision doInBackground() throws Exception {
                        try {
                            return new JdbcAuthService(TeamDevConnection::open, new LoginPolicy(new BCryptPasswordVerifier()),
                                    Clock.systemUTC()).authenticate(username, new String(password));
                        } finally { Arrays.fill(password, '\0'); }
                    }
                    protected void done() {
                        btnLogin.setEnabled(true); txtUsername.setEnabled(true); txtPassword.setEnabled(true);
                        try {
                            AuthDecision decision = get();
                            if (!winLogin.isDisplayable()) return;
                            if (!DesktopLoginGate.mayOpenMain(decision)) {
                                lblStatus.setText(decision.forcePasswordChange() ? "Cần đổi mật khẩu qua Core trước khi đăng nhập." : "Đăng nhập bị từ chối.");
                                return;
                            }
                            open(DesktopSession.fromSuccessfulLogin(decision)); winLogin.dispose();
                        } catch (InterruptedException failure) { Thread.currentThread().interrupt(); lblStatus.setText("Đăng nhập bị gián đoạn."); }
                        catch (ExecutionException failure) { lblStatus.setText(BillingUi.error(failure.getCause())); }
                    }
                }.execute();
            }
        });
    }
    private static void open(final DesktopSession session) {
        final JFrame winFeature = new JFrame("Khang · Service & Billing M1 · " + session.role());
        AuthorizationService authorization = new AuthorizationService(new RolePermissionPolicy());
        JTabbedPane tabFeatures = new JTabbedPane();
        if (authorization.can(session, Permission.SERVICE_CATALOG_MANAGE)) tabFeatures.addTab("Bảng giá dịch vụ", new DichVuPanel(session, TeamDevConnection::open));
        if (authorization.can(session, Permission.BILLING_READ)) tabFeatures.addTab("Hóa đơn & công nợ", new HoaDonPanel(session, TeamDevConnection::open));
        if (tabFeatures.getTabCount() == 0) tabFeatures.addTab("Quyền truy cập", new JLabelPanel());
        JPanel pnlRoot = new JPanel(new BorderLayout()); pnlRoot.add(tabFeatures, BorderLayout.CENTER);
        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent event) { session.logout(); winFeature.dispose(); login(); } });
        pnlRoot.add(btnLogout, BorderLayout.SOUTH);
        winFeature.addWindowListener(new WindowAdapter() { @Override public void windowClosed(WindowEvent event) { session.logout(); } });
        winFeature.setContentPane(pnlRoot); winFeature.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        winFeature.setSize(1400, 820); winFeature.setLocationRelativeTo(null); winFeature.setVisible(true);
    }
    private static final class JLabelPanel extends JPanel {
        JLabelPanel() { add(new JLabel("Tài khoản không có quyền truy cập hai module này.")); }
    }
}
