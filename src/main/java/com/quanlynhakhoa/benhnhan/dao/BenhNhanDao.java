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

    // Liệt kê rõ tên cột (không dùng SELECT *) để thứ tự cột luôn đúng khi đọc rs.getXxx(1..8)
    private static final String COT =
            "maBenhNhan, hoTen, ngaySinh, gioiTinh, soDienThoai, diaChi, nguoiLienHe, ngayTao ";

    // Lấy tất cả bệnh nhân (tối đa 200 dòng, mới nhất ở trên)
    public ArrayList<BenhNhan> getAllBenhNhan() throws SQLException {
        ArrayList<BenhNhan> dsBenhNhan = new ArrayList<>();
        String sql = "SELECT TOP 200 " + COT + "FROM dbo.BenhNhan ORDER BY maBenhNhan DESC";

        // try (...) tự đóng kết nối sau khi dùng xong
        try (Connection con = TeamDevConnection.open();
             PreparedStatement stmt = con.prepareStatement(sql);
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

        try (Connection con = TeamDevConnection.open();
             PreparedStatement stmt = con.prepareStatement(sql)) {
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
    public BenhNhan timTheoMa(String maBenhNhan) throws SQLException {
        String sql = "SELECT " + COT + "FROM dbo.BenhNhan WHERE maBenhNhan = ?";

        try (Connection con = TeamDevConnection.open();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maBenhNhan);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return docMotDong(rs);
                }
            }
        }
        return null;
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
