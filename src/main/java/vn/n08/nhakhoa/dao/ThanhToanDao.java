package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.dto.ThanhToanView;
import vn.n08.nhakhoa.model.PhuongThucThanhToan;
import vn.n08.nhakhoa.model.ThanhToan;
import vn.n08.nhakhoa.model.TrangThaiThanhToan;
import vn.n08.nhakhoa.util.Jdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Bảng ThanhToan — lịch sử các lần thu, không xóa; hủy giao dịch = đổi trạng thái DA_HUY có lý do. */
public class ThanhToanDao {
    static final String COLS = "tt.maThanhToan, tt.maHoaDon, tt.thoiGian, tt.soTien, tt.phuongThuc, tt.maGiaoDich, tt.tienKhachDua, tt.trangThai, "
            + "tt.maNhanVien, tt.maTaiKhoanHuy, tt.thoiGianHuy, tt.lyDoHuy";

    static ThanhToan map(ResultSet rs) throws SQLException {
        return new ThanhToan(rs.getString("maThanhToan"), rs.getString("maHoaDon"), Jdbc.dateTime(rs, "thoiGian"), Jdbc.money(rs, "soTien"),
                PhuongThucThanhToan.of(rs.getString("phuongThuc")), rs.getString("maGiaoDich"), rs.getBigDecimal("tienKhachDua"),
                TrangThaiThanhToan.of(rs.getString("trangThai")), rs.getString("maNhanVien"), rs.getString("maTaiKhoanHuy"),
                Jdbc.dateTime(rs, "thoiGianHuy"), rs.getString("lyDoHuy"));
    }

    public Optional<ThanhToan> find(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM ThanhToan tt WHERE tt.maThanhToan = ?", ThanhToanDao::map, ma);
    }

    public List<ThanhToanView> byInvoice(Connection c, String maHd) throws SQLException {
        return Jdbc.list(c, "SELECT " + COLS + ", nv.hoTen AS tenNv, tk.tenDangNhap AS tkHuy FROM ThanhToan tt JOIN NhanVien nv ON nv.maNhanVien = tt.maNhanVien "
                        + "LEFT JOIN TaiKhoan tk ON tk.maTaiKhoan = tt.maTaiKhoanHuy WHERE tt.maHoaDon = ? ORDER BY tt.thoiGian, tt.maThanhToan",
                rs -> new ThanhToanView(map(rs), rs.getString("tenNv"), rs.getString("tkHuy")), maHd);
    }

    public boolean transactionCodeUsed(Connection c, PhuongThucThanhToan pt, String maGd) throws SQLException {
        return Jdbc.count(c, "SELECT COUNT(*) FROM ThanhToan WHERE phuongThuc = ? AND maGiaoDich = ? AND trangThai = 'THANH_CONG'", pt, maGd) > 0;
    }

    public void insert(Connection c, ThanhToan t) throws SQLException {
        Jdbc.update(c, "INSERT INTO ThanhToan (maThanhToan, maHoaDon, thoiGian, soTien, phuongThuc, maGiaoDich, tienKhachDua, trangThai, maNhanVien) "
                        + "VALUES (?,?,?,?,?,?,?,?,?)",
                t.maThanhToan(), t.maHoaDon(), t.thoiGian(), t.soTien(), t.phuongThuc(), t.maGiaoDich(), t.tienKhachDua(), t.trangThai(), t.maNhanVien());
    }

    public int voidPayment(Connection c, String ma, String maTk, LocalDateTime t, String lyDo) throws SQLException {
        return Jdbc.update(c, "UPDATE ThanhToan SET trangThai = 'DA_HUY', maTaiKhoanHuy = ?, thoiGianHuy = ?, lyDoHuy = ? WHERE maThanhToan = ? AND trangThai = 'THANH_CONG'",
                maTk, t, lyDo, ma);
    }

    /** Tổng thu thành công trong khoảng thời gian theo phương thức (báo cáo tổng quan). */
    public BigDecimal sumBetween(Connection c, LocalDateTime tu, LocalDateTime den, PhuongThucThanhToan pt) throws SQLException {
        return Jdbc.sum(c, "SELECT SUM(soTien) FROM ThanhToan WHERE trangThai = 'THANH_CONG' AND thoiGian >= ? AND thoiGian < ? AND phuongThuc = ?", tu, den, pt);
    }
}
