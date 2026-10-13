package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.dto.NhatKyView;
import vn.n08.nhakhoa.util.Jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Bảng NhatKyThaoTac — chỉ thêm, không sửa/xóa. */
public class NhatKyDao {
    public long insert(Connection c, LocalDateTime thoiGian, String maTaiKhoan, String chucNang, String hanhDong,
                       String doiTuong, String noiDung, String maNguoiChiuTrachNhiem) throws SQLException {
        return Jdbc.insertReturningKey(c,
                "INSERT INTO NhatKyThaoTac (thoiGian, maTaiKhoan, chucNang, hanhDong, doiTuong, noiDung, maNguoiChiuTrachNhiem) VALUES (?,?,?,?,?,?,?)",
                thoiGian, maTaiKhoan, cut(chucNang, 50), cut(hanhDong, 200), cut(doiTuong, 50), cut(noiDung, 1000), maNguoiChiuTrachNhiem);
    }

    private static final String SELECT = "SELECT nk.maNhatKy, nk.thoiGian, tk.tenDangNhap, nk.chucNang, nk.hanhDong, nk.doiTuong, nk.noiDung, "
            + "nk.maNguoiChiuTrachNhiem, nv.hoTen AS tenNguoi FROM NhatKyThaoTac nk "
            + "LEFT JOIN TaiKhoan tk ON tk.maTaiKhoan = nk.maTaiKhoan "
            + "LEFT JOIN NhanVien nv ON nv.maNhanVien = nk.maNguoiChiuTrachNhiem ";

    private static NhatKyView map(java.sql.ResultSet rs) throws SQLException {
        return new NhatKyView(rs.getLong("maNhatKy"), Jdbc.dateTime(rs, "thoiGian"), rs.getString("tenDangNhap"),
                rs.getString("chucNang"), rs.getString("hanhDong"), rs.getString("doiTuong"), rs.getString("noiDung"),
                rs.getString("maNguoiChiuTrachNhiem"), rs.getString("tenNguoi"));
    }

    /** Tìm nhật ký trong khoảng thời gian; tuKhoa khớp tên đăng nhập, hành động, đối tượng hoặc nội dung. */
    public List<NhatKyView> search(Connection c, LocalDateTime tu, LocalDateTime den, String chucNang, String tuKhoa, int max) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE nk.thoiGian >= ? AND nk.thoiGian < ? ");
        List<Object> p = new ArrayList<>(List.of(tu, den));
        if (chucNang != null) { sql.append("AND nk.chucNang = ? "); p.add(chucNang); }
        if (tuKhoa != null && !tuKhoa.isBlank()) {
            sql.append("AND (LOWER(tk.tenDangNhap) LIKE ? OR LOWER(nk.hanhDong) LIKE ? OR LOWER(nk.doiTuong) LIKE ? OR LOWER(nk.noiDung) LIKE ?) ");
            String l = Jdbc.like(tuKhoa);
            p.add(l); p.add(l); p.add(l); p.add(l);
        }
        sql.append("ORDER BY nk.maNhatKy DESC OFFSET 0 ROWS FETCH NEXT ").append(max).append(" ROWS ONLY");
        return Jdbc.list(c, sql.toString(), NhatKyDao::map, p.toArray());
    }

    /** Lịch sử xử lý của một đối tượng (doiTuong bắt đầu bằng mã). */
    public List<NhatKyView> byObject(Connection c, String ma) throws SQLException {
        return Jdbc.list(c, SELECT + "WHERE nk.doiTuong = ? OR nk.doiTuong LIKE ? ORDER BY nk.maNhatKy", NhatKyDao::map, ma, ma + " %");
    }

    public List<String> chucNangList(Connection c) throws SQLException {
        return Jdbc.list(c, "SELECT DISTINCT chucNang FROM NhatKyThaoTac ORDER BY chucNang", rs -> rs.getString(1));
    }

    static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
