package com.quanlynhakhoa.nghiepvu.kho.workflow.dao;

import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** JDBC operations for receipt, issue and stocktake records. The service owns transactions. */
public final class InventoryWorkflowDao {
    public List<Object[]> danhSachNhap() throws SQLException {
        return query("SELECT TOP (200) p.maPhieuNhap,p.ngayNhap,n.tenNhaCungCap,p.trangThai,d.maLo,d.soLuong,d.donGiaNhap "
                + "FROM dbo.PhieuNhapKho p LEFT JOIN dbo.NhaCungCap n ON n.maNhaCungCap=p.maNhaCungCap "
                + "LEFT JOIN dbo.ChiTietPhieuNhap d ON d.maPhieuNhap=p.maPhieuNhap ORDER BY p.ngayNhap DESC", 7);
    }

    public List<Object[]> danhSachXuat() throws SQLException {
        return query("SELECT TOP (200) p.maPhieuXuat,p.ngayXuat,p.boPhanNhan,p.nguoiNhan,p.maNhanVien,p.trangThai,d.maLo,d.soLuong "
                + "FROM dbo.PhieuXuatKho p LEFT JOIN dbo.ChiTietPhieuXuat d ON d.maPhieuXuat=p.maPhieuXuat "
                + "ORDER BY p.ngayXuat DESC", 8);
    }

    public List<Object[]> danhSachKiemKe() throws SQLException {
        return query("SELECT TOP (200) p.maPhieu,p.thoiGianTao,p.maTaiKhoanTao,p.lyDo,p.trangThai,d.maLo,d.soLuongHeThong,d.soLuongThucTe "
                + "FROM dbo.PhieuDieuChinhKho p LEFT JOIN dbo.ChiTietDieuChinhKho d ON d.maPhieu=p.maPhieu "
                + "WHERE p.loai='KIEM_KE' ORDER BY p.thoiGianTao DESC", 8);
    }

    public void taoPhieuNhap(Connection con, String accountId, String maPhieu, String maLo,
                             String soLo, String maNcc, String loaiHang, String maHang, LocalDate ngaySanXuat,
                             LocalDate hanSuDung, int soLuong, long donGia, String ghiChu) throws SQLException {
        String employeeId = layMaNhanVien(con, accountId);
        try (PreparedStatement stmt = con.prepareStatement("INSERT INTO dbo.PhieuNhapKho "
                + "(maPhieuNhap,ngayNhap,trangThai,loaiNhap,maNhaCungCap,maPhieuXuatGoc,maNhanVien,ghiChu) "
                + "VALUES (?,?,'HOAN_TAT','NHAP_NCC',?,NULL,?,?)")) {
            stmt.setString(1, maPhieu);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(3, maNcc);
            stmt.setString(4, employeeId);
            stmt.setNString(5, ghiChu);
            stmt.executeUpdate();
        }
        try (PreparedStatement stmt = con.prepareStatement("INSERT INTO dbo.LoHang "
                + "(maLo,soLo,ngaySanXuat,hanSuDung,maThuoc,maVatTu,maNhaCungCap,soLuongTon) VALUES (?,?,?,?,?,?,?,0)")) {
            stmt.setString(1, maLo);
            stmt.setString(2, soLo);
            setDate(stmt, 3, ngaySanXuat);
            setDate(stmt, 4, hanSuDung);
            stmt.setString(5, "THUOC".equals(loaiHang) ? maHang : null);
            stmt.setString(6, "VATTU".equals(loaiHang) ? maHang : null);
            stmt.setString(7, maNcc);
            stmt.executeUpdate();
        }
        try (PreparedStatement stmt = con.prepareStatement("INSERT INTO dbo.ChiTietPhieuNhap "
                + "(maPhieuNhap,maLo,soLuong,donGiaNhap) VALUES (?,?,?,?)")) {
            stmt.setString(1, maPhieu);
            stmt.setString(2, maLo);
            stmt.setInt(3, soLuong);
            stmt.setLong(4, donGia);
            stmt.executeUpdate();
        }
        try (PreparedStatement stmt = con.prepareStatement("UPDATE dbo.LoHang SET soLuongTon=? WHERE maLo=? AND soLuongTon=0")) {
            stmt.setInt(1, soLuong);
            stmt.setString(2, maLo);
            if (stmt.executeUpdate() != 1) throw new SQLException("Không cập nhật được tồn của lô mới.");
        }
        ghiNhatKy(con, accountId, "Tạo phiếu nhập kho", "PhieuNhapKho", maPhieu);
    }

    public void taoPhieuXuat(Connection con, String accountId, String maPhieu, String maLo,
                             String boPhanNhan, String nguoiNhan, String lyDo, int soLuong) throws SQLException {
        String employeeId = layMaNhanVien(con, accountId);
        try (PreparedStatement stmt = con.prepareStatement("INSERT INTO dbo.PhieuXuatKho "
                + "(maPhieuXuat,ngayXuat,boPhanNhan,nguoiNhan,lyDo,trangThai,maNhanVien) VALUES (?,?,?,?,?,'HOAN_TAT',?)")) {
            stmt.setString(1, maPhieu);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setNString(3, boPhanNhan);
            stmt.setNString(4, nguoiNhan);
            stmt.setNString(5, lyDo);
            stmt.setString(6, employeeId);
            stmt.executeUpdate();
        }
         stmt.setInt(1, soLuong);
            stmt.setString(2, maLo);
            stmt.setInt(3, soLuong);
            if (stmt.executeUpdate() != 1) throw new IllegalArgumentException("Lô không tồn tại hoặc không đủ tồn kho.");
        }
        try (PreparedStatement stmt = con.prepareStatement("INSERT INTO dbo.ChiTietPhieuXuat (maPhieuXuat,maLo,soLuong) VALUES (?,?,?)")) {
            stmt.setString(1, maPhieu);
            stmt.setString(2, maLo);
            stmt.setInt(3, soLuong);
            stmt.executeUpdate();
        }
        ghiNhatKy(con, accountId, "Tạo phiếu xuất kho", "PhieuXuatKho", maPhieu);
    }

    public void taoYeuCauKiemKe(Connection con, String accountId, String maPhieu, String maLo,
                                int soLuongThucTe, String lyDo) throws SQLException {
        int soLuongHeThong;
        try (PreparedStatement stmt = con.prepareStatement(
                "SELECT soLuongTon FROM dbo.LoHang WITH (UPDLOCK, ROWLOCK) WHERE maLo=?")) {
            stmt.setString(1, maLo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy lô hàng.");
                soLuongHeThong = rs.getInt(1);
            }
        }
        if (soLuongHeThong == soLuongThucTe) throw new IllegalArgumentException("Số thực tế bằng số hệ thống; không cần phiếu điều chỉnh.");
        try (PreparedStatement stmt = con.prepareStatement("INSERT INTO dbo.PhieuDieuChinhKho "
                + "(maPhieu,loai,maTaiKhoanTao,thoiGianTao,lyDo,trangThai) VALUES (?,'KIEM_KE',?,SYSUTCDATETIME(),?,'CHO_DUYET')")) {
            stmt.setString(1, maPhieu);
            stmt.setString(2, accountId);
            stmt.setNString(3, lyDo);
            stmt.executeUpdate();
        }
        try (PreparedStatement stmt = con.prepareStatement("INSERT INTO dbo.ChiTietDieuChinhKho "
                + "(maPhieu,maLo,soLuongHeThong,soLuongThucTe) VALUES (?,?,?,?)")) {
            stmt.setString(1, maPhieu);
            stmt.setString(2, maLo);
            stmt.setInt(3, soLuongHeThong);
            stmt.setInt(4, soLuongThucTe);
            stmt.executeUpdate();
        }
        ghiNhatKy(con, accountId, "Gửi yêu cầu kiểm kê", "PhieuDieuChinhKho", maPhieu);
    }

        String sql = "SELECT nv.maNhanVien FROM dbo.TaiKhoan tk INNER JOIN dbo.NhanVien nv ON nv.maNhanVien=tk.maNhanVien "
                + "WHERE tk.maTaiKhoan=? AND tk.trangThai='HOAT_DONG' AND nv.trangThai='DANG_LAM_VIEC'";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Tài khoản chưa gắn với nhân viên đang làm việc; không thể lập chứng từ.");
                return rs.getString(1);
            }
        }
    }

    public static void ghiNhatKy(Connection con, String accountId, String action, String object, String id) throws SQLException {
        String sql = "INSERT INTO dbo.NhatKyThaoTac (loaiTacNhan,maTaiKhoan,chucNang,hanhDong,ketQua,doiTuong,maDoiTuong) "
                + "VALUES ('ACCOUNT',?,N'Kho',?,'SUCCESS',?,?)";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, accountId);
            stmt.setNString(2, action);
            stmt.setString(3, object);
            stmt.setString(4, id);
            stmt.executeUpdate();
        }
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

    private static void setDate(PreparedStatement stmt, int index, LocalDate value) throws SQLException {
        if (value == null) stmt.setDate(index, null);
        else stmt.setDate(index, Date.valueOf(value));
    }
}