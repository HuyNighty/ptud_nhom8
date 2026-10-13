package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.dto.LoHangView;
import vn.n08.nhakhoa.model.LoHang;
import vn.n08.nhakhoa.util.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Bảng LoHang. Cộng/trừ tồn luôn có điều kiện để không âm (kèm CHECK soLuongTon >= 0 trong CSDL). */
public class LoHangDao {
    static final String COLS = "lo.maLo, lo.soLo, lo.ngaySanXuat, lo.hanSuDung, lo.maThuoc, lo.maVatTu, lo.maNhaCungCap, lo.soLuongTon";
    private static final String VIEW = "SELECT " + COLS + ", COALESCE(t.tenThuoc, v.tenVatTu) AS tenHang, COALESCE(t.donViTinh, v.donViTinh) AS dvt, "
            + "ncc.tenNhaCungCap, COALESCE(t.kiemSoatDacBiet, 0) AS ksdb FROM LoHang lo LEFT JOIN Thuoc t ON t.maThuoc = lo.maThuoc "
            + "LEFT JOIN VatTu v ON v.maVatTu = lo.maVatTu JOIN NhaCungCap ncc ON ncc.maNhaCungCap = lo.maNhaCungCap ";

    static LoHang map(ResultSet rs) throws SQLException {
        return new LoHang(rs.getString("maLo"), rs.getString("soLo"), Jdbc.date(rs, "ngaySanXuat"), Jdbc.date(rs, "hanSuDung"),
                rs.getString("maThuoc"), rs.getString("maVatTu"), rs.getString("maNhaCungCap"), rs.getInt("soLuongTon"));
    }

    static LoHangView mapView(ResultSet rs) throws SQLException {
        return new LoHangView(map(rs), rs.getString("tenHang"), rs.getString("dvt"), rs.getString("tenNhaCungCap"), rs.getInt("ksdb") == 1);
    }

    public Optional<LoHang> find(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM LoHang lo WHERE lo.maLo = ?", LoHangDao::map, ma);
    }

    public Optional<LoHangView> findView(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, VIEW + "WHERE lo.maLo = ?", LoHangDao::mapView, ma);
    }

    public List<LoHangView> byItem(Connection c, boolean laThuoc, String maHang) throws SQLException {
        return Jdbc.list(c, VIEW + (laThuoc ? "WHERE lo.maThuoc = ? " : "WHERE lo.maVatTu = ? ") + "ORDER BY lo.hanSuDung, lo.maLo", LoHangDao::mapView, maHang);
    }

    /** Lô còn hàng và chưa hết hạn, hạn gần nhất trước (FEFO). */
    public List<LoHangView> availableFefo(Connection c, boolean laThuoc, String maHang, LocalDate homNay) throws SQLException {
        return Jdbc.list(c, VIEW + (laThuoc ? "WHERE lo.maThuoc = ? " : "WHERE lo.maVatTu = ? ")
                + "AND lo.soLuongTon > 0 AND (lo.hanSuDung IS NULL OR lo.hanSuDung > ?) ORDER BY lo.hanSuDung, lo.maLo", LoHangDao::mapView, maHang, homNay);
    }

    /** Mọi lô còn tồn (dùng cho kiểm kê). */
    public List<LoHangView> inStock(Connection c) throws SQLException {
        return Jdbc.list(c, VIEW + "WHERE lo.soLuongTon > 0 ORDER BY tenHang, lo.hanSuDung", LoHangDao::mapView);
    }

    /** Lô còn tồn có hạn dùng trước hoặc bằng ngày cho trước (gồm lô đã hết hạn) — UC37. */
    public List<LoHangView> expiringBy(Connection c, LocalDate denNgay) throws SQLException {
        return Jdbc.list(c, VIEW + "WHERE lo.soLuongTon > 0 AND lo.hanSuDung IS NOT NULL AND lo.hanSuDung <= ? ORDER BY lo.hanSuDung", LoHangDao::mapView, denNgay);
    }

    public Optional<LoHang> findBySoLo(Connection c, boolean laThuoc, String maHang, String soLo, String maNcc) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM LoHang lo WHERE " + (laThuoc ? "lo.maThuoc = ?" : "lo.maVatTu = ?")
                + " AND lo.soLo = ? AND lo.maNhaCungCap = ?", LoHangDao::map, maHang, soLo, maNcc);
    }

    public void insert(Connection c, LoHang l) throws SQLException {
        Jdbc.update(c, "INSERT INTO LoHang (maLo, soLo, ngaySanXuat, hanSuDung, maThuoc, maVatTu, maNhaCungCap, soLuongTon) VALUES (?,?,?,?,?,?,?,?)",
                l.maLo(), l.soLo(), l.ngaySanXuat(), l.hanSuDung(), l.maThuoc(), l.maVatTu(), l.maNhaCungCap(), l.soLuongTon());
    }

    public int addStock(Connection c, String maLo, int soLuong) throws SQLException {
        return Jdbc.update(c, "UPDATE LoHang SET soLuongTon = soLuongTon + ? WHERE maLo = ?", soLuong, maLo);
    }

    /** Trừ tồn; trả 0 nếu tồn không đủ (không trừ). */
    public int takeStock(Connection c, String maLo, int soLuong) throws SQLException {
        return Jdbc.update(c, "UPDATE LoHang SET soLuongTon = soLuongTon - ? WHERE maLo = ? AND soLuongTon >= ?", soLuong, maLo, soLuong);
    }

    /** Đặt tồn theo kết quả điều chỉnh, chỉ khi tồn hiện tại vẫn bằng số lúc lập phiếu. */
    public int setStock(Connection c, String maLo, int tonCu, int tonMoi) throws SQLException {
        return Jdbc.update(c, "UPDATE LoHang SET soLuongTon = ? WHERE maLo = ? AND soLuongTon = ?", tonMoi, maLo, tonCu);
    }

    public long countExpiredInStock(Connection c, LocalDate homNay) throws SQLException {
        return Jdbc.count(c, "SELECT COUNT(*) FROM LoHang WHERE soLuongTon > 0 AND hanSuDung IS NOT NULL AND hanSuDung <= ?", homNay);
    }

    public long countExpiring(Connection c, LocalDate homNay, LocalDate denNgay) throws SQLException {
        return Jdbc.count(c, "SELECT COUNT(*) FROM LoHang WHERE soLuongTon > 0 AND hanSuDung > ? AND hanSuDung <= ?", homNay, denNgay);
    }
}
