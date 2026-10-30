package com.quanlynhakhoa.benhnhan.dao;

import com.quanlynhakhoa.benhnhan.model.HoSoBenhAn;
import com.quanlynhakhoa.benhnhan.model.LuotKham;
import com.quanlynhakhoa.dev.TeamDevConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;

// DAO: chỉ làm việc với bảng dbo.HoSoBenhAn và đọc dbo.PhieuKham (lịch sử khám).
// Không kiểm tra quyền, không kiểm tra nghiệp vụ. Kết nối bằng tài khoản n08dev_*, KHÔNG dùng sa.
public class HoSoBenhAnDao {
    // Thời gian chờ tối đa (giây) cho mỗi câu SQL (giống BenhNhanDao)
    private static final int THOI_GIAN_CHO_TOI_DA = 15;

    private final BenhNhanDao.ConnectionProvider cachMoKetNoi;

    public HoSoBenhAnDao() {
        this(TeamDevConnection::open);
    }

    public HoSoBenhAnDao(BenhNhanDao.ConnectionProvider cachMoKetNoi) {
        this.cachMoKetNoi = java.util.Objects.requireNonNull(cachMoKetNoi, "cachMoKetNoi");
    }

    // ===================== XEM HỒ SƠ BỆNH ÁN =====================

    // Lấy hồ sơ bệnh án của 1 bệnh nhân, chưa có thì trả về null
    public HoSoBenhAn timTheoBenhNhan(String maBenhNhan) throws SQLException {
        String sql = "SELECT maHoSo, maBenhNhan, ngayLap, tienSuBenh, diUng "
                + "FROM dbo.HoSoBenhAn WHERE maBenhNhan = ?";

        try (Connection ketNoi = cachMoKetNoi.open();
             PreparedStatement cauLenh = taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, maBenhNhan);
            try (ResultSet ketQua = cauLenh.executeQuery()) {
                if (ketQua.next()) {
                    return new HoSoBenhAn(
                            ketQua.getString(1),
                            ketQua.getString(2),
                            ketQua.getDate(3).toLocalDate(),
                            ketQua.getNString(4),
                            ketQua.getNString(5));
                }
            }
        }
        return null;
    }

    // Danh sách các lần khám của 1 bệnh nhân (mới nhất ở trên, tối đa 200 lần)
    public ArrayList<LuotKham> layDanhSachLuotKham(String maBenhNhan) throws SQLException {
        ArrayList<LuotKham> danhSachLuotKham = new ArrayList<>();
        String sql = "SELECT TOP 200 p.maPhieuKham, p.thoiGianTiepNhan, n.hoTen, "
                + "p.trieuChung, p.chanDoan, p.ketLuan, p.trangThai "
                + "FROM dbo.PhieuKham AS p "
                + "LEFT JOIN dbo.NhanVien AS n ON n.maNhanVien = p.maNhanVien "
                + "WHERE p.maBenhNhan = ? "
                + "ORDER BY p.thoiGianTiepNhan DESC";

        try (Connection ketNoi = cachMoKetNoi.open();
             PreparedStatement cauLenh = taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, maBenhNhan);
            try (ResultSet ketQua = cauLenh.executeQuery()) {
                while (ketQua.next()) {
                    LocalDateTime thoiGianTiepNhan = null;
                    Timestamp thoiGianTrongDatabase = ketQua.getTimestamp(2);
                    if (thoiGianTrongDatabase != null) {
                        thoiGianTiepNhan = thoiGianTrongDatabase.toLocalDateTime();
                    }

                    LuotKham luotKham = new LuotKham(
                            ketQua.getString(1),       // mã phiếu khám
                            thoiGianTiepNhan,
                            ketQua.getNString(3),      // tên bác sĩ
                            ketQua.getNString(4),      // triệu chứng
                            ketQua.getNString(5),      // chẩn đoán
                            ketQua.getNString(6),      // kết luận
                            ketQua.getString(7));      // trạng thái
                    danhSachLuotKham.add(luotKham);
                }
            }
        }
        return danhSachLuotKham;
    }

    // Ghi nhật ký "ai đã xem hồ sơ bệnh án của bệnh nhân nào" (UC009 bước 2: mỗi lần xem đều ghi nhật ký).
    // Không ghi nội dung hồ sơ vào nhật ký.
    public void ghiNhatKyXem(String maTaiKhoan, String maBenhNhan) throws SQLException {
        String sql = "INSERT INTO dbo.NhatKyThaoTac "
                + "(loaiTacNhan, maTaiKhoan, chucNang, hanhDong, ketQua, doiTuong, maDoiTuong) "
                + "VALUES ('ACCOUNT', ?, N'HoSoBenhAn', N'Xem hồ sơ bệnh án', 'SUCCESS', 'BenhNhan', ?)";
        try (Connection ketNoi = cachMoKetNoi.open();
             PreparedStatement cauLenh = taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, maTaiKhoan);
            cauLenh.setString(2, maBenhNhan);
            cauLenh.executeUpdate();
        }
    }

    // ===================== CẬP NHẬT TIỀN SỬ / DỊ ỨNG =====================
    // Các hàm dưới đây nhận Connection từ Service (BenhNhanService mở 1 transaction khi sửa bệnh nhân)

    // Lấy mã hồ sơ của bệnh nhân (có khóa dòng trong transaction), chưa có thì trả về null
    public String timMaHoSo(Connection ketNoi, String maBenhNhan) throws SQLException {
        String sql = "SELECT maHoSo FROM dbo.HoSoBenhAn WITH (UPDLOCK, HOLDLOCK) WHERE maBenhNhan = ?";
        try (PreparedStatement cauLenh = taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, maBenhNhan);
            try (ResultSet ketQua = cauLenh.executeQuery()) {
                if (ketQua.next()) {
                    return ketQua.getString(1);
                }
                return null;
            }
        }
    }

    // Cập nhật tiền sử bệnh và dị ứng, trả về true nếu có dòng được sửa
    public boolean capNhatTienSu(Connection ketNoi, String maHoSo, String tienSuBenh, String diUng)
            throws SQLException {
        String sql = "UPDATE dbo.HoSoBenhAn SET tienSuBenh = ?, diUng = ? WHERE maHoSo = ?";
        try (PreparedStatement cauLenh = taoCauLenh(ketNoi, sql)) {
            cauLenh.setNString(1, tienSuBenh);
            cauLenh.setNString(2, diUng);
            cauLenh.setString(3, maHoSo);
            int soDongDaSua = cauLenh.executeUpdate();
            return soDongDaSua > 0;
        }
    }

    // Tạo PreparedStatement kèm thời gian chờ tối đa; lỗi thì đóng lại ngay (giống BenhNhanDao)
    private PreparedStatement taoCauLenh(Connection ketNoi, String sql) throws SQLException {
        PreparedStatement cauLenh = ketNoi.prepareStatement(sql);
        try {
            cauLenh.setQueryTimeout(THOI_GIAN_CHO_TOI_DA);
            return cauLenh;
        } catch (SQLException | RuntimeException loi) {
            try {
                cauLenh.close();
            } catch (SQLException loiKhiDong) {
                loi.addSuppressed(loiKhiDong);
            }
            throw loi;
        }
    }
}
