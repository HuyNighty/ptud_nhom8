package com.quanlynhakhoa.dao.nhanvien;

import com.quanlynhakhoa.model.nhanvien.YeuCauTaoNhanVien;
import com.quanlynhakhoa.entity.NhanVien;
import com.quanlynhakhoa.enums.TrangThaiNhanVien;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

/** Workforce SQL writes. Caller owns the JDBC transaction and authorization. */
public final class TaoNhanVienDao {
    private static final String CHECK_DUPLICATE_SQL = """
            SELECT maNhanVien
            FROM dbo.NhanVien WITH (UPDLOCK, HOLDLOCK)
            WHERE maNhanVien = ? OR soDienThoai = ?
            """;

    private static final String INSERT_SQL = """
            INSERT INTO dbo.NhanVien
                (maNhanVien, hoTen, soDienThoai, chucDanh, trangThai,
                 ngaySinh, gioiTinh, email, ngayVaoLam, chuyenMon, ngayNghiViec)
            VALUES (?, ?, ?, ?, 'DANG_LAM_VIEC', ?, ?, ?, ?, ?, NULL)
            """;

    private static final String AUDIT_SQL = """
            INSERT INTO dbo.NhatKyThaoTac
                (loaiTacNhan, maTaiKhoan, chucNang, hanhDong,
                 ketQua, doiTuong, maDoiTuong, noiDung)
            VALUES ('ACCOUNT', ?, N'NhanVien', N'Tạo nhân viên',
                    'SUCCESS', N'NhanVien', ?, N'Tạo hồ sơ nhân viên')
            """;

    public void requireUnique(Connection connection, YeuCauTaoNhanVien request) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        try (PreparedStatement statement = connection.prepareStatement(CHECK_DUPLICATE_SQL)) {
            statement.setQueryTimeout(12);
            statement.setString(1, request.employeeId());
            statement.setString(2, request.phone());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    throw new IllegalArgumentException("Mã nhân viên hoặc số điện thoại đã tồn tại.");
                }
            }
        }
    }

    public void insert(Connection connection, YeuCauTaoNhanVien request) throws SQLException {
        NhanVien nhanVien = new NhanVien(request.employeeId(), request.fullName(), request.phone(),
                request.jobTitle(), TrangThaiNhanVien.DANG_LAM_VIEC, request.birthDate(),
                request.gender(), request.email(), request.joinedAt(), request.specialty(), null);
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setQueryTimeout(12);
            statement.setString(1, nhanVien.getMaNhanVien());
            statement.setNString(2, nhanVien.getHoTen());
            statement.setString(3, nhanVien.getSoDienThoai());
            statement.setString(4, nhanVien.getChucDanh());
            if (nhanVien.getNgaySinh() == null) statement.setNull(5, Types.DATE);
            else statement.setDate(5, java.sql.Date.valueOf(nhanVien.getNgaySinh()));
            if (nhanVien.getGioiTinh() == null) statement.setNull(6, Types.VARCHAR);
            else statement.setString(6, nhanVien.getGioiTinh());
            if (nhanVien.getEmail() == null) statement.setNull(7, Types.VARCHAR);
            else statement.setString(7, nhanVien.getEmail());
            statement.setDate(8, java.sql.Date.valueOf(nhanVien.getNgayVaoLam()));
            if (nhanVien.getChuyenMon() == null) statement.setNull(9, Types.NVARCHAR);
            else statement.setNString(9, nhanVien.getChuyenMon());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Expected exactly one employee insert");
            }
        }
    }

    public void auditCreated(Connection connection, String actorId, String employeeId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(AUDIT_SQL)) {
            statement.setQueryTimeout(12);
            statement.setString(1, actorId);
            statement.setString(2, employeeId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Expected exactly one employee audit insert");
            }
        }
    }
}
