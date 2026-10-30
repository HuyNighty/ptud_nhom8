package com.quanlynhakhoa.benhnhan.dao;

import com.quanlynhakhoa.benhnhan.model.HoSoBenhAn;
import com.quanlynhakhoa.benhnhan.model.LuotKham;
import com.quanlynhakhoa.dev.TeamDevConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

// DAO: chỉ làm việc với bảng dbo.HoSoBenhAn và đọc dbo.PhieuKham (lịch sử khám).
// Không kiểm tra quyền, không kiểm tra nghiệp vụ. Kết nối bằng tài khoản n08dev_*, KHÔNG dùng sa.
public class HoSoBenhAnDao {
    // Thời gian chờ tối đa cho mỗi câu SQL (giống BenhNhanDao)
    private static final int QUERY_TIMEOUT_SECONDS = 15;

    private final BenhNhanDao.ConnectionProvider connections;

    public HoSoBenhAnDao() {
        this(TeamDevConnection::open);
    }

    public HoSoBenhAnDao(BenhNhanDao.ConnectionProvider connections) {
        this.connections = java.util.Objects.requireNonNull(connections, "connections");
    }

    // ===================== XEM HỒ SƠ BỆNH ÁN =====================

    // Lấy hồ sơ bệnh án của 1 bệnh nhân, chưa có thì trả về null
    public HoSoBenhAn timTheoBenhNhan(String maBenhNhan) throws SQLException {
        String sql = "SELECT maHoSo, maBenhNhan, ngayLap, tienSuBenh, diUng "
                + "FROM dbo.HoSoBenhAn WHERE maBenhNhan = ?";

        try (Connection con = connections.open();
             PreparedStatement stmt = prepareStatement(con, sql)) {
            stmt.setString(1, maBenhNhan);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new HoSoBenhAn(
                            rs.getString(1),
                            rs.getString(2),
                            rs.getDate(3).toLocalDate(),
                            rs.getNString(4),
                            rs.getNString(5));
                }
            }
        }
        return null;
    }

    // Danh sách các lần khám của 1 bệnh nhân (mới nhất ở trên, tối đa 200 lần)
    public ArrayList<LuotKham> dsLuotKham(String maBenhNhan) throws SQLException {
        ArrayList<LuotKham> dsLuotKham = new ArrayList<>();
        String sql = "SELECT TOP 200 p.maPhieuKham, p.thoiGianTiepNhan, n.hoTen, "
                + "p.trieuChung, p.chanDoan, p.ketLuan, p.trangThai "
                + "FROM dbo.PhieuKham AS p "
                + "LEFT JOIN dbo.NhanVien AS n ON n.maNhanVien = p.maNhanVien "
                + "WHERE p.maBenhNhan = ? "
                + "ORDER BY p.thoiGianTiepNhan DESC";

        try (Connection con = connections.open();
             PreparedStatement stmt = prepareStatement(con, sql)) {
            stmt.setString(1, maBenhNhan);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp tg = rs.getTimestamp(2);
                    dsLuotKham.add(new LuotKham(
                            rs.getString(1),
                            tg == null ? null : tg.toLocalDateTime(),
                            rs.getNString(3),
                            rs.getNString(4),
                            rs.getNString(5),
                            rs.getNString(6),
                            rs.getString(7)));
                }
            }
        }
        return dsLuotKham;
    }

    // Ghi nhật ký "ai đã xem hồ sơ bệnh án của bệnh nhân nào" (UC009 bước 2: mỗi lần xem đều ghi nhật ký).
    // Không ghi nội dung hồ sơ vào nhật ký.
    public void ghiNhatKyXem(String maTaiKhoan, String maBenhNhan) throws SQLException {
        String sql = "INSERT INTO dbo.NhatKyThaoTac "
                + "(loaiTacNhan, maTaiKhoan, chucNang, hanhDong, ketQua, doiTuong, maDoiTuong) "
                + "VALUES ('ACCOUNT', ?, N'HoSoBenhAn', N'Xem hồ sơ bệnh án', 'SUCCESS', 'BenhNhan', ?)";
        try (Connection con = connections.open();
             PreparedStatement stmt = prepareStatement(con, sql)) {
            stmt.setString(1, maTaiKhoan);
            stmt.setString(2, maBenhNhan);
            stmt.executeUpdate();
        }
    }

    // ===================== CẬP NHẬT TIỀN SỬ / DỊ ỨNG =====================
    // Các hàm dưới đây nhận Connection từ Service (BenhNhanService mở 1 transaction khi sửa bệnh nhân)

    // Lấy mã hồ sơ của bệnh nhân (có khóa dòng trong transaction), chưa có thì trả về null
    public String timMaHoSo(Connection con, String maBenhNhan) throws SQLException {
        String sql = "SELECT maHoSo FROM dbo.HoSoBenhAn WITH (UPDLOCK, HOLDLOCK) WHERE maBenhNhan = ?";
        try (PreparedStatement stmt = prepareStatement(con, sql)) {
            stmt.setString(1, maBenhNhan);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    // Cập nhật tiền sử bệnh và dị ứng, trả về true nếu có dòng được sửa
    public boolean capNhatTienSu(Connection con, String maHoSo, String tienSuBenh, String diUng)
            throws SQLException {
        String sql = "UPDATE dbo.HoSoBenhAn SET tienSuBenh = ?, diUng = ? WHERE maHoSo = ?";
        try (PreparedStatement stmt = prepareStatement(con, sql)) {
            stmt.setNString(1, tienSuBenh);
            stmt.setNString(2, diUng);
            stmt.setString(3, maHoSo);
            return stmt.executeUpdate() > 0;
        }
    }

    // Tạo PreparedStatement kèm thời gian chờ tối đa; lỗi thì đóng lại ngay (giống BenhNhanDao)
    private PreparedStatement prepareStatement(Connection con, String sql) throws SQLException {
        PreparedStatement stmt = con.prepareStatement(sql);
        try {
            stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            return stmt;
        } catch (SQLException | RuntimeException ex) {
            try {
                stmt.close();
            } catch (SQLException closeError) {
                ex.addSuppressed(closeError);
            }
            throw ex;
        }
    }
}
