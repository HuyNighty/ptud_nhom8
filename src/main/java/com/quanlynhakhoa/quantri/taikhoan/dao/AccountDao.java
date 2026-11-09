package com.quanlynhakhoa.quantri.taikhoan.dao;

import com.quanlynhakhoa.model.taikhoan.ThongTinTaiKhoan;
import com.quanlynhakhoa.quantri.taikhoan.entity.AccountStatus;
import com.quanlynhakhoa.quantri.taikhoan.entity.LockSource;
import com.quanlynhakhoa.quantri.vaitro.entity.Role;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/** R3-A1: read-only JDBC DAO. No write/transaction ownership in this class yet. */
public final class AccountDao {
    private static final String FIND_BY_USERNAME = """
            SELECT maTaiKhoan, maNhanVien, tenDangNhap, tenHienThi,
                   matKhauHash, vaiTro, trangThai, soLanSai, lanSaiDau,
                   khoaLuc, khoaDen, nguonKhoa, lyDoKhoa,
                   phaiDoiMatKhau, matKhauCapNhatLuc, lanDangNhapCuoi,
                   taoLuc, capNhatLuc, voHieuLuc, lyDoVoHieu
            FROM dbo.TaiKhoan
            WHERE tenDangNhap = ?
            """;

    /**
     * Returns a sensitive snapshot containing a BCrypt hash. Never display or log it.
     * The caller owns Connection; this DAO closes only statement and result set.
     * R3-A2 must add an explicit transaction and row lock before mutating login state.
     */
    public Optional<ThongTinTaiKhoan> findByUsername(Connection connection, String username)
            throws SQLException {
        if (connection == null) throw new IllegalArgumentException("connection required");
        if (username == null || username.isBlank()) return Optional.empty();
        try (PreparedStatement stmt = connection.prepareStatement(FIND_BY_USERNAME)) {
            stmt.setString(1, username);
            stmt.setQueryTimeout(10);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private static ThongTinTaiKhoan map(ResultSet rs) throws SQLException {
        String lock = rs.getString("nguonKhoa");
        return new ThongTinTaiKhoan(
                rs.getString("maTaiKhoan"),
                rs.getString("maNhanVien"),
                rs.getString("tenDangNhap"),
                rs.getString("tenHienThi"),
                rs.getString("matKhauHash"),
                Role.valueOf(rs.getString("vaiTro")),
                AccountStatus.valueOf(rs.getString("trangThai")),
                rs.getInt("soLanSai"),
                at(rs, "lanSaiDau"),
                at(rs, "khoaLuc"),
                at(rs, "khoaDen"),
                lock == null ? null : LockSource.valueOf(lock),
                rs.getString("lyDoKhoa"),
                rs.getBoolean("phaiDoiMatKhau"),
                at(rs, "matKhauCapNhatLuc"),
                at(rs, "lanDangNhapCuoi"),
                at(rs, "taoLuc"),
                at(rs, "capNhatLuc"),
                at(rs, "voHieuLuc"),
                rs.getString("lyDoVoHieu")
        );
    }

    private static LocalDateTime at(ResultSet rs, String name) throws SQLException {
        Timestamp ts = rs.getTimestamp(name);
        return ts == null ? null : ts.toLocalDateTime();
    }
}
