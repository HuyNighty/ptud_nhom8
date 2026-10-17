package com.quanlynhakhoa.features.inventory.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Ràng buộc của migration danh mục kho trên SQL Server thật. Mọi thay đổi đều rollback. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "n08.desktop.enabled=false"
)
class InventoryCatalogSchemaIT {

    @Autowired
    private DataSource dataSource;

    @FunctionalInterface
    interface SqlWork {
        void run(Connection connection) throws Exception;
    }

    private void withRollback(SqlWork work) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                work.run(connection);
            } finally {
                connection.rollback();
            }
        }
    }

    private static void insertThuoc(Connection c, String ma, String ten, int dinhMuc) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO dbo.Thuoc (maThuoc, tenThuoc, donViTinh, dinhMucToiThieu) VALUES (?, ?, N'ống', ?)")) {
            ps.setString(1, ma);
            ps.setNString(2, ten);
            ps.setInt(3, dinhMuc);
            ps.executeUpdate();
        }
    }

    @Test
    void validMedicineKeepsVietnameseText() throws Exception {
        withRollback(c -> {
            insertThuoc(c, "TH90001", "Thuốc tê Lidocaine 2% — ống", 10);
            try (PreparedStatement ps = c.prepareStatement("SELECT tenThuoc FROM dbo.Thuoc WHERE maThuoc = 'TH90001'");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals("Thuốc tê Lidocaine 2% — ống", rs.getNString(1));
            }
        });
    }

    @Test
    void codeFormatIsEnforced() throws Exception {
        withRollback(c -> assertThrows(SQLException.class, () -> insertThuoc(c, "TH01", "A", 0)));
        withRollback(c -> assertThrows(SQLException.class, () -> insertThuoc(c, "TH001X", "A", 0)));
        withRollback(c -> assertThrows(SQLException.class, () -> insertThuoc(c, "VT001", "A", 0)));
    }

    @Test
    void blankNameAndNegativeMinimumAreRejected() throws Exception {
        withRollback(c -> assertThrows(SQLException.class, () -> insertThuoc(c, "TH90002", "   ", 0)));
        withRollback(c -> assertThrows(SQLException.class, () -> insertThuoc(c, "TH90003", "A", -1)));
    }

    @Test
    void supplyCodeFormatIsEnforced() throws Exception {
        withRollback(c -> assertThrows(SQLException.class, () -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO dbo.VatTu (maVatTu, tenVatTu, donViTinh) VALUES ('TH001', N'Bông', N'gói')")) {
                ps.executeUpdate();
            }
        }));
    }
}
