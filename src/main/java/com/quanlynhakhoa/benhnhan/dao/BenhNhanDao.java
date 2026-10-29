package com.quanlynhakhoa.benhnhan.dao;

import com.quanlynhakhoa.benhnhan.model.BenhNhan;
import com.quanlynhakhoa.benhnhan.model.GioiTinh;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Truy cập bảng dbo.BenhNhan bằng PreparedStatement.
 * DAO KHÔNG kiểm tra quyền và KHÔNG chứa quy tắc nghiệp vụ (việc đó thuộc BenhNhanService).
 */
public final class BenhNhanDao {
    /** Số dòng tối đa trả về cho một lần tìm kiếm. */
    public static final int GIOI_HAN_DONG = 200;

    private static final String COT = """
            maBenhNhan, hoTen, ngaySinh, gioiTinh, soDienThoai,
            diaChi, nguoiLienHe, ngayTao, maTaiKhoanTao
            """;

    // Lấy 201 dòng để biết danh sách có bị cắt bớt hay không.
    private static final String SQL_TIM_KIEM = "SELECT TOP (201) " + COT + """
            FROM dbo.BenhNhan
            WHERE (? = N''
                   OR CHARINDEX(?, hoTen) > 0
                   OR CHARINDEX(?, maBenhNhan) > 0
                   OR (? <> '' AND CHARINDEX(?, soDienThoai) > 0))
              AND (? IS NULL OR gioiTinh = ?)
            ORDER BY maBenhNhan DESC
            """;

    private static final String SQL_TIM_THEO_MA = "SELECT " + COT + """
            FROM dbo.BenhNhan
            WHERE maBenhNhan = ?
            """;

    /** Kết quả tìm kiếm: danh sách + cờ báo còn dữ liệu chưa hiển thị. */
    public record KetQuaTimKiem(List<BenhNhan> danhSach, boolean biCatBot) {
        public KetQuaTimKiem {
            danhSach = List.copyOf(danhSach);
        }
    }

    /**
     * @param tuKhoa        từ khóa đã chuẩn hóa (họ tên hoặc mã BN), chuỗi rỗng = không lọc
     * @param soDienThoai   chỉ gồm chữ số; chuỗi rỗng nếu từ khóa không phải số điện thoại
     * @param gioiTinh      null = tất cả
     */
    public KetQuaTimKiem timKiem(Connection ketNoi, String tuKhoa, String soDienThoai,
                                 GioiTinh gioiTinh) throws SQLException {
        Objects.requireNonNull(ketNoi, "ketNoi");
        Objects.requireNonNull(tuKhoa, "tuKhoa");
        Objects.requireNonNull(soDienThoai, "soDienThoai");
        String maGioiTinh = gioiTinh == null ? null : gioiTinh.name();
        try (PreparedStatement lenh = ketNoi.prepareStatement(SQL_TIM_KIEM)) {
            lenh.setQueryTimeout(12);
            lenh.setNString(1, tuKhoa);
            lenh.setNString(2, tuKhoa);
            lenh.setNString(3, tuKhoa);
            lenh.setString(4, soDienThoai);
            lenh.setString(5, soDienThoai);
            lenh.setString(6, maGioiTinh);
            lenh.setString(7, maGioiTinh);
            List<BenhNhan> danhSach = new ArrayList<>();
            boolean biCatBot = false;
            try (ResultSet rs = lenh.executeQuery()) {
                while (rs.next()) {
                    if (danhSach.size() >= GIOI_HAN_DONG) {
                        biCatBot = true;
                        break;
                    }
                    danhSach.add(docDong(rs));
                }
            }
            return new KetQuaTimKiem(danhSach, biCatBot);
        }
    }

    public Optional<BenhNhan> timTheoMa(Connection ketNoi, String maBenhNhan) throws SQLException {
        Objects.requireNonNull(ketNoi, "ketNoi");
        try (PreparedStatement lenh = ketNoi.prepareStatement(SQL_TIM_THEO_MA)) {
            lenh.setQueryTimeout(10);
            lenh.setString(1, maBenhNhan);
            try (ResultSet rs = lenh.executeQuery()) {
                return rs.next() ? Optional.of(docDong(rs)) : Optional.empty();
            }
        }
    }

    /** Đổi một dòng ResultSet thành đối tượng BenhNhan. */
    static BenhNhan docDong(ResultSet rs) throws SQLException {
        Date ngaySinh = rs.getDate("ngaySinh");
        Timestamp ngayTao = rs.getTimestamp("ngayTao");
        return new BenhNhan(
                rs.getString("maBenhNhan"),
                rs.getNString("hoTen"),
                ngaySinh == null ? null : ngaySinh.toLocalDate(),
                GioiTinh.valueOf(rs.getString("gioiTinh")),
                rs.getString("soDienThoai"),
                rs.getNString("diaChi"),
                rs.getNString("nguoiLienHe"),
                ngayTao == null ? null : ngayTao.toLocalDateTime(),
                rs.getString("maTaiKhoanTao"));
    }
}
