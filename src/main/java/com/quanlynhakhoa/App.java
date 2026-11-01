package com.quanlynhakhoa;

import com.quanlynhakhoa.hatang.cosodulieu.Database;
import com.quanlynhakhoa.hatang.cosodulieu.DbInspector;
import com.quanlynhakhoa.hethong.xacthuc.BCryptPasswordVerifier;
import com.quanlynhakhoa.hethong.xacthuc.JdbcAuthService;
import com.quanlynhakhoa.hethong.xacthuc.LoginPolicy;
import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import com.quanlynhakhoa.hethong.giaodien.LoginFrame;
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
        if (args.length > 0 && "--team-db".equals(args[0])) {
            try (java.sql.Connection ignored = TeamDevConnection.open()) {
                System.out.println("PASS: N08 team local JDBC connected");
            } catch (java.sql.SQLException failure) {
                System.err.println("TEAM DB CHECK FAILED: SQLState=" + failure.getSQLState()
                        + ", code=" + failure.getErrorCode());
                System.exit(4);
            }
            return;
        }
        SwingUtilities.invokeLater(() -> new LoginFrame(
                new JdbcAuthService(TeamDevConnection::open,
                        new LoginPolicy(new BCryptPasswordVerifier()), Clock.systemUTC()))
                .setVisible(true));
    }
}
