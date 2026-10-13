package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.dto.TonKhoItem;
import vn.n08.nhakhoa.model.Thuoc;
import vn.n08.nhakhoa.model.VatTu;
import vn.n08.nhakhoa.util.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Bảng Thuoc + VatTu và truy vấn tồn kho tổng hợp theo lô. */
public class HangHoaDao {
    static final String T_COLS = "t.maThuoc, t.tenThuoc, t.donViTinh, t.kiemSoatDacBiet, t.hamLuong, t.hoatChat, t.dinhMucToiThieu, t.daDatHang";
    static final String V_COLS = "v.maVatTu, v.tenVatTu, v.donViTinh, v.moTa, v.dinhMucToiThieu, v.daDatHang";

    static Thuoc mapThuoc(ResultSet rs) throws SQLException {
        return new Thuoc(rs.getString("maThuoc"), rs.getString("tenThuoc"), rs.getString("donViTinh"), rs.getBoolean("kiemSoatDacBiet"),
                rs.getString("hamLuong"), rs.getString("hoatChat"), rs.getInt("dinhMucToiThieu"), rs.getBoolean("daDatHang"));
    }

    static VatTu mapVatTu(ResultSet rs) throws SQLException {
        return new VatTu(rs.getString("maVatTu"), rs.getString("tenVatTu"), rs.getString("donViTinh"), rs.getString("moTa"),
                rs.getInt("dinhMucToiThieu"), rs.getBoolean("daDatHang"));
    }

    public List<Thuoc> allThuoc(Connection c) throws SQLException {
        return Jdbc.list(c, "SELECT " + T_COLS + " FROM Thuoc t ORDER BY t.tenThuoc", HangHoaDao::mapThuoc);
    }

    public List<VatTu> allVatTu(Connection c) throws SQLException {
        return Jdbc.list(c, "SELECT " + V_COLS + " FROM VatTu v ORDER BY v.tenVatTu", HangHoaDao::mapVatTu);
    }

    public Optional<Thuoc> findThuoc(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, "SELECT " + T_COLS + " FROM Thuoc t WHERE t.maThuoc = ?", HangHoaDao::mapThuoc, ma);
    }

    public Optional<VatTu> findVatTu(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, "SELECT " + V_COLS + " FROM VatTu v WHERE v.maVatTu = ?", HangHoaDao::mapVatTu, ma);
    }

    public boolean nameUsed(Connection c, boolean laThuoc, String ten, String exceptMa) throws SQLException {
        String sql = laThuoc ? "SELECT COUNT(*) FROM Thuoc WHERE LOWER(tenThuoc) = ? AND maThuoc <> ?"
                : "SELECT COUNT(*) FROM VatTu WHERE LOWER(tenVatTu) = ? AND maVatTu <> ?";
        return Jdbc.count(c, sql, ten.trim().toLowerCase(), exceptMa == null ? "" : exceptMa) > 0;
    }

    public void insertThuoc(Connection c, Thuoc t) throws SQLException {
        Jdbc.update(c, "INSERT INTO Thuoc (maThuoc, tenThuoc, donViTinh, kiemSoatDacBiet, hamLuong, hoatChat, dinhMucToiThieu, daDatHang) VALUES (?,?,?,?,?,?,?,?)",
                t.maThuoc(), t.tenThuoc(), t.donViTinh(), t.kiemSoatDacBiet(), t.hamLuong(), t.hoatChat(), t.dinhMucToiThieu(), t.daDatHang());
    }

    public int updateThuoc(Connection c, Thuoc t) throws SQLException {
        return Jdbc.update(c, "UPDATE Thuoc SET tenThuoc = ?, donViTinh = ?, kiemSoatDacBiet = ?, hamLuong = ?, hoatChat = ?, dinhMucToiThieu = ? WHERE maThuoc = ?",
                t.tenThuoc(), t.donViTinh(), t.kiemSoatDacBiet(), t.hamLuong(), t.hoatChat(), t.dinhMucToiThieu(), t.maThuoc());
    }

    public void insertVatTu(Connection c, VatTu v) throws SQLException {
        Jdbc.update(c, "INSERT INTO VatTu (maVatTu, tenVatTu, donViTinh, moTa, dinhMucToiThieu, daDatHang) VALUES (?,?,?,?,?,?)",
                v.maVatTu(), v.tenVatTu(), v.donViTinh(), v.moTa(), v.dinhMucToiThieu(), v.daDatHang());
    }

    public int updateVatTu(Connection c, VatTu v) throws SQLException {
        return Jdbc.update(c, "UPDATE VatTu SET tenVatTu = ?, donViTinh = ?, moTa = ?, dinhMucToiThieu = ? WHERE maVatTu = ?",
                v.tenVatTu(), v.donViTinh(), v.moTa(), v.dinhMucToiThieu(), v.maVatTu());
    }

    public int setDaDatHang(Connection c, boolean laThuoc, String ma, boolean v) throws SQLException {
        return Jdbc.update(c, laThuoc ? "UPDATE Thuoc SET daDatHang = ? WHERE maThuoc = ?" : "UPDATE VatTu SET daDatHang = ? WHERE maVatTu = ?", v, ma);
    }

    /** Tồn kho tổng hợp: khả dụng (lô chưa hết hạn), còn trong lô hết hạn, số lô còn hàng, HSD gần nhất của lô khả dụng. */
    public List<TonKhoItem> tonKho(Connection c, LocalDate homNay, String tuKhoa) throws SQLException {
        String agg = "COALESCE(SUM(CASE WHEN lo.hanSuDung IS NULL OR lo.hanSuDung > ? THEN lo.soLuongTon ELSE 0 END), 0) AS khaDung, "
                + "COALESCE(SUM(CASE WHEN lo.hanSuDung <= ? THEN lo.soLuongTon ELSE 0 END), 0) AS hetHan, "
                + "COALESCE(SUM(CASE WHEN lo.soLuongTon > 0 THEN 1 ELSE 0 END), 0) AS soLo, "
                + "MIN(CASE WHEN lo.soLuongTon > 0 AND lo.hanSuDung > ? THEN lo.hanSuDung END) AS hsd ";
        String l = Jdbc.like(tuKhoa);
        boolean filter = tuKhoa != null && !tuKhoa.isBlank();
        List<TonKhoItem> out = new ArrayList<>();
        List<Object> p = new ArrayList<>(List.of(homNay, homNay, homNay));
        if (filter) { p.add(l); p.add(l); p.add(l); }
        out.addAll(Jdbc.list(c, "SELECT " + T_COLS + ", " + agg + "FROM Thuoc t LEFT JOIN LoHang lo ON lo.maThuoc = t.maThuoc "
                        + (filter ? "WHERE LOWER(t.tenThuoc) LIKE ? OR LOWER(t.maThuoc) LIKE ? OR LOWER(t.hoatChat) LIKE ? " : "")
                        + "GROUP BY " + T_COLS + " ORDER BY t.tenThuoc",
                rs -> {
                    Thuoc t = mapThuoc(rs);
                    String mo = (t.hamLuong() == null ? "" : t.hamLuong()) + (t.hoatChat() == null ? "" : " · " + t.hoatChat());
                    return new TonKhoItem(t.maThuoc(), t.tenThuoc(), true, t.donViTinh(), mo.trim(), t.kiemSoatDacBiet(), t.dinhMucToiThieu(),
                            t.daDatHang(), rs.getInt("khaDung"), rs.getInt("hetHan"), rs.getInt("soLo"), Jdbc.date(rs, "hsd"));
                }, p.toArray()));
        p = new ArrayList<>(List.of(homNay, homNay, homNay));
        if (filter) { p.add(l); p.add(l); }
        out.addAll(Jdbc.list(c, "SELECT " + V_COLS + ", " + agg + "FROM VatTu v LEFT JOIN LoHang lo ON lo.maVatTu = v.maVatTu "
                        + (filter ? "WHERE LOWER(v.tenVatTu) LIKE ? OR LOWER(v.maVatTu) LIKE ? " : "")
                        + "GROUP BY " + V_COLS + " ORDER BY v.tenVatTu",
                rs -> {
                    VatTu v = mapVatTu(rs);
                    return new TonKhoItem(v.maVatTu(), v.tenVatTu(), false, v.donViTinh(), v.moTa(), false, v.dinhMucToiThieu(), v.daDatHang(),
                            rs.getInt("khaDung"), rs.getInt("hetHan"), rs.getInt("soLo"), Jdbc.date(rs, "hsd"));
                }, p.toArray()));
        return out;
    }
}
