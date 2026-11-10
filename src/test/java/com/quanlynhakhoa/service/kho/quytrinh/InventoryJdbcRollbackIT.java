package com.quanlynhakhoa.service.kho.quytrinh;

import com.quanlynhakhoa.config.phattrien.TeamDevConnection;
import com.quanlynhakhoa.dao.kho.quytrinh.QuyTrinhKhoDao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/** READ THE PRECONDITIONS: run only with an existing N08Dev login tied to an active employee.
 * No database creation or demo reset. All synthetic writes are rolled back.
 */
public final class InventoryJdbcRollbackIT {
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || args[0].isBlank()) throw new IllegalArgumentException("Usage: InventoryJdbcRollbackIT <activeAccountId>");
        if (!TeamDevConnection.hasLocalConfig()) throw new IllegalStateException("No explicit .n08-dev.local.properties: refusing synthetic fallback.");
        String voucher = "IT" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String nonexistentLot = "ZZ" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        try (Connection con = TeamDevConnection.open()) {
            if (exists(con, "SELECT COUNT(*) FROM dbo.LoHang WHERE maLo=?", nonexistentLot)) {
                throw new IllegalStateException("Generated test lot unexpectedly exists.");
            }
            if (exists(con, "SELECT COUNT(*) FROM dbo.PhieuXuatKho WHERE maPhieuXuat=?", voucher)) {
                throw new IllegalStateException("Generated test voucher unexpectedly exists.");
            }
            con.setAutoCommit(false);
            try {
                new QuyTrinhKhoDao().taoPhieuXuat(con, args[0], voucher, nonexistentLot, "INVENTORY_IT", "", "ROLLBACK_TEST", 1);
                throw new AssertionError("Expected insufficient/nonexistent-lot failure");
            } catch (IllegalArgumentException expected) {
                con.rollback();
            } catch (Exception unexpected) {
                con.rollback();
                throw unexpected;
            }
        }
        try (Connection verify = TeamDevConnection.open()) {
            if (exists(verify, "SELECT COUNT(*) FROM dbo.PhieuXuatKho WHERE maPhieuXuat=?", voucher)
                    || exists(verify, "SELECT COUNT(*) FROM dbo.NhatKyThaoTac WHERE doiTuong='PhieuXuatKho' AND maDoiTuong=?", voucher)) {
                throw new AssertionError("ROLLBACK FAILED: synthetic voucher/audit persisted");
            }
        }
        verifyNegativeStockGuard();
        System.out.println("PASS InventoryJdbcRollbackIT: voucher and audit rollback verified; see stock constraint PASS/SKIP above");
    }
    private static void verifyNegativeStockGuard() throws SQLException {
        try (Connection con = TeamDevConnection.open()) {
            String lot = null;
            try (PreparedStatement stmt = con.prepareStatement("SELECT TOP(1) maLo FROM dbo.LoHang ORDER BY maLo");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) lot = rs.getString(1);
            }
            if (lot == null) {
                System.out.println("SKIP stock-negative SQL assertion: no existing lot in isolated dev DB");
                return;
            }
            con.setAutoCommit(false);
            boolean rejected = false;
            try (PreparedStatement stmt = con.prepareStatement("UPDATE dbo.LoHang SET soLuongTon=-1 WHERE maLo=?")) {
                stmt.setString(1, lot);
                try { stmt.executeUpdate(); }
                catch (SQLException expected) { rejected = true; }
            } finally {
                con.rollback(); // never keep writes; CHECK failure is expected
            }
            if (!rejected) throw new AssertionError("Negative stock was accepted by SQL Server");
            System.out.println("PASS SQL CK_LoHang_Ton rejected negative inventory");
        }
    }
    private static boolean exists(Connection c, String sql, String key) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, key);
            try (ResultSet r = s.executeQuery()) { r.next(); return r.getInt(1) != 0; }
        }
    }
}
