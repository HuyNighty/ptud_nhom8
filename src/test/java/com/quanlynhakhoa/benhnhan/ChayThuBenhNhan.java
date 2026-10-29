package com.quanlynhakhoa.benhnhan;

import com.quanlynhakhoa.auth.AuthDecision;
import com.quanlynhakhoa.auth.BCryptPasswordVerifier;
import com.quanlynhakhoa.auth.DesktopLoginGate;
import com.quanlynhakhoa.auth.JdbcAuthService;
import com.quanlynhakhoa.auth.LoginPolicy;
import com.quanlynhakhoa.benhnhan.view.BenhNhanPanel;
import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.GridLayout;
import java.time.Clock;
import java.util.Arrays;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Chạy thử RIÊNG màn hình Bệnh nhân trên database development (N08Dev_*),
 * trong lúc chờ Huy gắn BenhNhanPanel vào FeatureBootstrap (file Core).
 * Đăng nhập THẬT bằng tài khoản ứng dụng; kết nối dùng TeamDevConnection (n08dev_*, không phải sa).
 *
 * Windows PowerShell:
 *   .\mvnw.cmd -q test-compile dependency:build-classpath "-Dmdep.outputFile=cp.txt"
 *   java -cp "target/classes;target/test-classes;$(Get-Content cp.txt)" com.quanlynhakhoa.benhnhan.ChayThuBenhNhan
 */
public final class ChayThuBenhNhan {
    private ChayThuBenhNhan() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChayThuBenhNhan::dangNhapRoiMo);
    }

    private static void dangNhapRoiMo() {
        if (!TeamDevConnection.hasLocalConfig()) {
            JOptionPane.showMessageDialog(null,
                    "Chưa có .n08-dev.local.properties. Chạy scripts\\setup-local.ps1 trước.",
                    "Chạy thử Bệnh nhân", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JTextField txtTenDangNhap = new JTextField(18);
        JPasswordField txtMatKhau = new JPasswordField(18);
        JPanel pnlDangNhap = new JPanel(new GridLayout(0, 1, 0, 4));
        pnlDangNhap.add(new JLabel("Tên đăng nhập"));
        pnlDangNhap.add(txtTenDangNhap);
        pnlDangNhap.add(new JLabel("Mật khẩu"));
        pnlDangNhap.add(txtMatKhau);
        int chon = JOptionPane.showConfirmDialog(null, pnlDangNhap, "Đăng nhập chạy thử",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        char[] matKhau = txtMatKhau.getPassword();
        if (chon != JOptionPane.OK_OPTION) return;
        try {
            JdbcAuthService xacThuc = new JdbcAuthService(TeamDevConnection::open,
                    new LoginPolicy(new BCryptPasswordVerifier()), Clock.systemUTC());
            AuthDecision ketQua = xacThuc.authenticate(txtTenDangNhap.getText().trim(), new String(matKhau));
            if (!DesktopLoginGate.mayOpenMain(ketQua)) {
                JOptionPane.showMessageDialog(null, "Đăng nhập không thành công.");
                return;
            }
            DesktopSession phien = DesktopSession.fromSuccessfulLogin(ketQua);
            JFrame winChayThu = new JFrame("N08 — Chạy thử Hồ sơ bệnh nhân (" + phien.role() + ")");
            winChayThu.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            winChayThu.setContentPane(new BenhNhanPanel(phien));
            winChayThu.setSize(1200, 720);
            winChayThu.setLocationRelativeTo(null);
            winChayThu.setVisible(true);
        } catch (Exception loi) {
            JOptionPane.showMessageDialog(null, "Không đăng nhập được: " + loi.getClass().getSimpleName());
        } finally {
            Arrays.fill(matKhau, '\0');
        }
    }
}
