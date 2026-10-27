package com.quanlynhakhoa;

import com.quanlynhakhoa.config.Database;
import com.quanlynhakhoa.config.DbInspector;
import com.quanlynhakhoa.auth.BCryptPasswordVerifier;
import com.quanlynhakhoa.auth.JdbcAuthService;
import com.quanlynhakhoa.auth.LoginPolicy;
import com.quanlynhakhoa.auth.SandboxLoginConnection;
import com.quanlynhakhoa.view.LoginFrame;
import java.time.Clock;
import javax.swing.SwingUtilities;

public final class App {
    private App() { }

    public static void main(String[] args) {
        if (args.length > 0 && "--smoke".equals(args[0])) {
            System.out.println("PASS: Java SE bootstrap works. No GUI or database needed.");
            return;
        }
        if (args.length > 0 && "--check-db".equals(args[0])) {
            try {
                Database.checkConnection();
            } catch (Exception e) {
                System.err.println("DB CHECK FAILED (" + e.getClass().getSimpleName() + "): " + e.getMessage());
                System.exit(2);
            }
            return;
        }
        if (args.length > 0 && "--inspect-db".equals(args[0])) {
            try {
                DbInspector.inspect();
            } catch (Exception e) {
                System.err.println("DB INSPECTION FAILED (" + e.getClass().getSimpleName() + "): " + e.getMessage());
                System.exit(3);
            }
            return;
        }
        SwingUtilities.invokeLater(() -> new LoginFrame(
                new JdbcAuthService(SandboxLoginConnection::open,
                        new LoginPolicy(new BCryptPasswordVerifier()), Clock.systemUTC()))
                .setVisible(true));
    }
}
