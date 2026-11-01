package com.quanlynhakhoa.nghiepvu.kho.hopcapcuu.dao;

import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import com.quanlynhakhoa.nghiepvu.kho.workflow.dao.InventoryWorkflowDao;
import com.quanlynhakhoa.nghiepvu.kho.hopcapcuu.service.EmergencyKitService.CheckLine;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class EmergencyKitDao {
    public List<Object[]> danhMuc() throws SQLException {
        String sql = "SELECT maMuc,tenMuc,donViTinh,soLuongToiThieu,soLuongHienCo,hanSuDungGanNhat,maThuoc,maVatTu,ghiChu FROM dbo.DanhMucCapCuu WHERE dangApDung=1 ORDER BY tenMuc";
        return query(sql, 9);
    }

    public List<Object[]> lichSu() throws SQLException {
        String sql = "SELECT TOP (200) k.maKiemTra,k.thoiGian,nv.hoTen,k.ketQua,k.ghiChu,COUNT(d.maMuc) FROM dbo.KiemTraCapCuu k INNER JOIN dbo.NhanVien nv ON nv.maNhanVien=k.maNhanVien LEFT JOIN dbo.ChiTietKiemTraCapCuu d ON d.maKiemTra=k.maKiemTra GROUP BY k.maKiemTra,k.thoiGian,nv.hoTen,k.ketQua,k.ghiChu ORDER BY k.thoiGian DESC";
        return query(sql, 6);
    }

    public void ghiKiemTra(Connection con, String accountId, String checkId, String note, List<CheckLine> lines) throws SQLException {
        String employee = InventoryWorkflowDao.layMaNhanVien(con, accountId);
        boolean allOk = true;
        List<Object[]> values = new ArrayList<>();
        String select = "SELECT soLuongToiThieu,hanSuDungGanNhat FROM dbo.DanhMucCapCuu WHERE maMuc=? AND dangApDung=1";

        for (CheckLine line : lines) {
            int min;
            Date nearestExpiry;
            try (PreparedStatement stmt = con.prepareStatement(select)) {
                stmt.setString(1, line.itemId());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Mã mục hộp cấp cứu không còn áp dụng: " + line.itemId());
                    min = rs.getInt(1);
                    nearestExpiry = rs.getDate(2);
                }
            }
            boolean expiryOk = line.expiry() != null ? !line.expiry().isBefore(java.time.LocalDate.now()) : nearestExpiry == null;
            boolean passed = line.actualQuantity() >= min && expiryOk;
            allOk &= passed;
            values.add(new Object[]{line, passed});
        }

        try (PreparedStatement stmt = con.prepareStatement("INSERT INTO dbo.KiemTraCapCuu(maKiemTra,thoiGian,maNhanVien,ketQua,ghiChu) VALUES (?,SYSUTCDATETIME(),?,?,?)")) {
            stmt.setString(1, checkId);
            stmt.setString(2, employee);
            stmt.setString(3, allOk ? "DAT" : "CAN_BO_SUNG");
            stmt.setNString(4, note);
            stmt.executeUpdate();
        }

        try (PreparedStatement stmt = con.prepareStatement("INSERT INTO dbo.ChiTietKiemTraCapCuu(maKiemTra,maMuc,soLuongThucTe,hanSuDung,dat) VALUES (?,?,?,?,?)")) {
            for (Object[] value : values) {
                CheckLine line = (CheckLine) value[0];
                stmt.setString(1, checkId);
                stmt.setString(2, line.itemId());
                stmt.setInt(3, line.actualQuantity());
                if (line.expiry() == null) {
                    stmt.setDate(4, null);
                } else {
                    stmt.setDate(4, Date.valueOf(line.expiry()));
                }
                stmt.setBoolean(5, (Boolean) value[1]);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
        InventoryWorkflowDao.ghiNhatKy(con, accountId, "Ghi nhận kiểm tra hộp cấp cứu", "KiemTraCapCuu", checkId);
    }

    private List<Object[]> query(String sql, int width) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        try (Connection con = TeamDevConnection.open();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Object[] row = new Object[width];
                for (int i = 0; i < width; i++) row[i] = rs.getObject(i + 1);
                rows.add(row);
            }
        }
        return rows;
    }
}