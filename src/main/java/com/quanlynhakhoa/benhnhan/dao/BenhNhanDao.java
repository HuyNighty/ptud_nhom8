package com.quanlynhakhoa.benhnhan.dao;

import com.quanlynhakhoa.benhnhan.model.BenhNhan;
import com.quanlynhakhoa.dev.TeamDevConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

// DAO: chỉ làm việc với SQL Server, không kiểm tra quyền, không kiểm tra nghiệp vụ
// Kết nối bằng TeamDevConnection (tài khoản n08dev_*, KHÔNG dùng sa)
public class BenhNhanDao {
    // Seconds per statement, including lock waits inside the service transaction.
    private static final int QUERY_TIMEOUT_SECONDS = 15;

    @FunctionalInterface
    public interface ConnectionProvider {
        Connection open() throws SQLException;
    }

    private final ConnectionProvider connections;

    public BenhNhanDao() {
        this(TeamDevConnection::open);
    }

    public BenhNhanDao(ConnectionProvider connections) {
        this.connections = java.util.Objects.requireNonNull(connections, "connections");
    }

    // Liệt kê rõ tên cột (không dùng SELECT *) để thứ tự cột luôn đúng khi đọc rs.getXxx(1..8)
    private static final String COT =
            "maBenhNhan, hoTen, ngaySinh, gioiTinh, soDienThoai, diaChi, nguoiLienHe, ngayTao ";

    // Lấy tất cả bệnh nhân (tối đa 200 dòng, mới nhất ở trên)
    public ArrayList<BenhNhan> getAllBenhNhan() throws SQLException {
        ArrayList<BenhNhan> dsBenhNhan = new ArrayList<>();
        String sql = "SELECT TOP 200 " + COT + "FROM dbo.BenhNhan ORDER BY maBenhNhan DESC";

        // try (...) tự đóng kết nối sau khi dùng xong
        try (Connection con = connections.open();
             PreparedStatement stmt = prepareStatement(con, sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                dsBenhNhan.add(docMotDong(rs));
            }
        }
        return dsBenhNhan;
    }

    // Tìm bệnh nhân theo từ khóa (họ tên / mã BN / SĐT) và giới tính
    // gioiTinh = "" nghĩa là lấy tất cả giới tính
    public ArrayList<BenhNhan> timKiem(String tuKhoa, String gioiTinh) throws SQLException {
        ArrayList<BenhNhan> dsBenhNhan = new ArrayList<>();
        String sql = "SELECT TOP 200 " + COT
                + "FROM dbo.BenhNhan "
                + "WHERE (hoTen LIKE ? OR maBenhNhan LIKE ? OR soDienThoai LIKE ?) "
                + "AND (? = '' OR gioiTinh = ?) "
                + "ORDER BY maBenhNhan DESC";

        try (Connection con = connections.open();
             PreparedStatement stmt = prepareStatement(con, sql)) {
            String mau = "%" + tuKhoa + "%";
            stmt.setNString(1, mau);
            stmt.setString(2, mau);
            stmt.setString(3, mau);
            stmt.setString(4, gioiTinh);
            stmt.setString(5, gioiTinh);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    dsBenhNhan.add(docMotDong(rs));
                }
            }
        }
        return dsBenhNhan;
    }

    // Lấy 1 bệnh nhân theo mã, không có thì trả về null
    // Đọc kèm tiền sử bệnh + dị ứng từ bảng HoSoBenhAn (LEFT JOIN: chưa có hồ sơ thì 2 cột này NULL)
    public BenhNhan timTheoMa(String maBenhNhan) throws SQLException {
        String sql = "SELECT b.maBenhNhan, b.hoTen, b.ngaySinh, b.gioiTinh, b.soDienThoai, "
                + "b.diaChi, b.nguoiLienHe, b.ngayTao, h.tienSuBenh, h.diUng "
                + "FROM dbo.BenhNhan AS b "
                + "LEFT JOIN dbo.HoSoBenhAn AS h ON h.maBenhNhan = b.maBenhNhan "
                + "WHERE b.maBenhNhan = ?";

        try (Connection con = connections.open();
             PreparedStatement stmt = prepareStatement(con, sql)) {
            stmt.setString(1, maBenhNhan);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BenhNhan bn = docMotDong(rs);     // cột 1..8
                    bn.setTienSuBenh(rs.getNString(9));
                    bn.setDiUng(rs.getNString(10));
                    return bn;
                }
            }
        }
        return null;
    }

    // ===================== Phần THÊM / SỬA =====================
    // Các hàm dưới đây nhận Connection từ Service, vì Service mở 1 transaction
    // cho nhiều câu lệnh (thêm bệnh nhân + thêm hồ sơ + ghi nhật ký).

    // Tìm hồ sơ nghi trùng: cùng SĐT, hoặc cùng họ tên + ngày sinh
    // boQuaMa: mã của chính bệnh nhân đang sửa (để không tự báo trùng với mình), null khi thêm mới
    public ArrayList<BenhNhan> timNghiTrung(String soDienThoai, String hoTen,
                                            java.time.LocalDate ngaySinh, String boQuaMa) throws SQLException {
        ArrayList<BenhNhan> dsTrung = new ArrayList<>();
        String sql = "SELECT TOP 10 " + COT
                + "FROM dbo.BenhNhan "
                + "WHERE (soDienThoai = ? OR (hoTen = ? AND ngaySinh = ?)) "
                + "AND (? IS NULL OR maBenhNhan <> ?)";

        try (Connection con = connections.open();
             PreparedStatement stmt = prepareStatement(con, sql)) {
            stmt.setString(1, soDienThoai);
            stmt.setNString(2, hoTen);
            stmt.setDate(3, java.sql.Date.valueOf(ngaySinh));
            stmt.setString(4, boQuaMa);
            stmt.setString(5, boQuaMa);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    dsTrung.add(docMotDong(rs));
                }
            }
        }
        return dsTrung;
    }

    // Sinh mã bệnh nhân mới: BN + 6 chữ số (BN000001, BN000002, ...)
    // UPDLOCK, HOLDLOCK: khóa lại để 2 người bấm Lưu cùng lúc không bị trùng mã
    public String taoMaBenhNhanMoi(Connection con) throws SQLException {
        String sql = "SELECT ISNULL(MAX(CAST(SUBSTRING(maBenhNhan, 3, 6) AS INT)), 0) + 1 "
                + "FROM dbo.BenhNhan WITH (UPDLOCK, HOLDLOCK) "
                + "WHERE maBenhNhan LIKE 'BN[0-9][0-9][0-9][0-9][0-9][0-9]'";
        return taoMa(con, sql, "BN");
    }

    // Sinh mã hồ sơ bệnh án mới: HS + 6 chữ số (HS000001, ...)
    public String taoMaHoSoMoi(Connection con) throws SQLException {
        String sql = "SELECT ISNULL(MAX(CAST(SUBSTRING(maHoSo, 3, 6) AS INT)), 0) + 1 "
                + "FROM dbo.HoSoBenhAn WITH (UPDLOCK, HOLDLOCK) "
                + "WHERE maHoSo LIKE 'HS[0-9][0-9][0-9][0-9][0-9][0-9]'";
        return taoMa(con, sql, "HS");
    }

    private String taoMa(Connection con, String sql, String tienTo) throws SQLException {
        try (PreparedStatement stmt = prepareStatement(con, sql);
             ResultSet rs = stmt.executeQuery()) {
            rs.next();
            int soTiepTheo = rs.getInt(1);
            if (soTiepTheo > 999999) {
                throw new SQLException("Đã hết mã " + tienTo);
            }
            return tienTo + String.format("%06d", soTiepTheo); // ví dụ 7 → BN000007
        }
    }

    // Thêm 1 dòng vào bảng BenhNhan
    public void themBenhNhan(Connection con, BenhNhan bn, String maTaiKhoanTao) throws SQLException {
        String sql = "INSERT INTO dbo.BenhNhan "
                + "(maBenhNhan, hoTen, ngaySinh, gioiTinh, soDienThoai, diaChi, nguoiLienHe, maTaiKhoanTao) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = prepareStatement(con, sql)) {
            stmt.setString(1, bn.getMaBenhNhan());
            stmt.setNString(2, bn.getHoTen());
            stmt.setDate(3, java.sql.Date.valueOf(bn.getNgaySinh()));
            stmt.setString(4, bn.getGioiTinh());
            stmt.setString(5, bn.getSoDienThoai());
            stmt.setNString(6, bn.getDiaChi());
            stmt.setNString(7, bn.getNguoiLienHe());
            stmt.setString(8, maTaiKhoanTao);
            stmt.executeUpdate();
        }
    }

    // Thêm hồ sơ bệnh án trống (chưa có tiền sử / dị ứng)
    public void themHoSoBenhAn(Connection con, String maHoSo, String maBenhNhan) throws SQLException {
        themHoSoBenhAn(con, maHoSo, maBenhNhan, null, null);
    }

    // Thêm hồ sơ bệnh án cho bệnh nhân mới (mỗi bệnh nhân có đúng 1 hồ sơ),
    // kèm tiền sử bệnh + dị ứng (có thể null)
    public void themHoSoBenhAn(Connection con, String maHoSo, String maBenhNhan,
                               String tienSuBenh, String diUng) throws SQLException {
        String sql = "INSERT INTO dbo.HoSoBenhAn (maHoSo, maBenhNhan, ngayLap, tienSuBenh, diUng) "
                + "VALUES (?, ?, CAST(GETDATE() AS DATE), ?, ?)";
        try (PreparedStatement stmt = prepareStatement(con, sql)) {
            stmt.setString(1, maHoSo);
            stmt.setString(2, maBenhNhan);
            stmt.setNString(3, tienSuBenh);
            stmt.setNString(4, diUng);
            stmt.executeUpdate();
        }
    }

    // Cập nhật thông tin bệnh nhân, trả về true nếu có dòng được sửa
    public boolean capNhatBenhNhan(Connection con, BenhNhan bn) throws SQLException {
        String sql = "UPDATE dbo.BenhNhan SET hoTen = ?, ngaySinh = ?, gioiTinh = ?, "
                + "soDienThoai = ?, diaChi = ?, nguoiLienHe = ? "
                + "WHERE maBenhNhan = ?";
        try (PreparedStatement stmt = prepareStatement(con, sql)) {
            stmt.setNString(1, bn.getHoTen());
            stmt.setDate(2, java.sql.Date.valueOf(bn.getNgaySinh()));
            stmt.setString(3, bn.getGioiTinh());
            stmt.setString(4, bn.getSoDienThoai());
            stmt.setNString(5, bn.getDiaChi());
            stmt.setNString(6, bn.getNguoiLienHe());
            stmt.setString(7, bn.getMaBenhNhan());
            return stmt.executeUpdate() > 0;
        }
    }

    // Ghi nhật ký thao tác (không ghi họ tên / SĐT bệnh nhân vào nhật ký)
    public void ghiNhatKy(Connection con, String maTaiKhoan, String hanhDong, String maBenhNhan)
            throws SQLException {
        String sql = "INSERT INTO dbo.NhatKyThaoTac "
                + "(loaiTacNhan, maTaiKhoan, chucNang, hanhDong, ketQua, doiTuong, maDoiTuong) "
                + "VALUES ('ACCOUNT', ?, N'BenhNhan', ?, 'SUCCESS', 'BenhNhan', ?)";
        try (PreparedStatement stmt = prepareStatement(con, sql)) {
            stmt.setString(1, maTaiKhoan);
            stmt.setNString(2, hanhDong);
            stmt.setString(3, maBenhNhan);
            stmt.executeUpdate();
        }
    }

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

    // Đọc 1 dòng kết quả thành 1 đối tượng BenhNhan (thứ tự cột giống hằng COT ở trên)
    private BenhNhan docMotDong(ResultSet rs) throws SQLException {
        String maBenhNhan = rs.getString(1);
        String hoTen = rs.getNString(2);
        java.time.LocalDate ngaySinh = rs.getDate(3).toLocalDate();
        String gioiTinh = rs.getString(4);
        String soDienThoai = rs.getString(5);
        String diaChi = rs.getNString(6);
        String nguoiLienHe = rs.getNString(7);
        Timestamp tg = rs.getTimestamp(8);
        java.time.LocalDateTime ngayTao = tg == null ? null : tg.toLocalDateTime();

        return new BenhNhan(maBenhNhan, hoTen, ngaySinh, gioiTinh,
                soDienThoai, diaChi, nguoiLienHe, ngayTao);
    }
}
