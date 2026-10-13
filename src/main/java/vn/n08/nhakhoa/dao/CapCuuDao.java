package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.dto.ChiTietKiemTraView;
import vn.n08.nhakhoa.dto.KiemTraCapCuuView;
import vn.n08.nhakhoa.model.ChiTietKiemTraCapCuu;
import vn.n08.nhakhoa.model.DanhMucCapCuu;
import vn.n08.nhakhoa.model.KetQuaKiemTra;
import vn.n08.nhakhoa.model.KiemTraCapCuu;
import vn.n08.nhakhoa.util.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Bảng DanhMucCapCuu, KiemTraCapCuu, ChiTietKiemTraCapCuu. */
public class CapCuuDao {
    static final String COLS = "m.maMuc, m.tenMuc, m.donViTinh, m.soLuongToiThieu, m.soLuongHienCo, m.hanSuDungGanNhat, m.maThuoc, m.maVatTu, "
            + "m.dangApDung, m.laDuLieuMau, m.ghiChu";

    static DanhMucCapCuu map(ResultSet rs) throws SQLException {
        return new DanhMucCapCuu(rs.getString("maMuc"), rs.getString("tenMuc"), rs.getString("donViTinh"), rs.getInt("soLuongToiThieu"),
                rs.getInt("soLuongHienCo"), Jdbc.date(rs, "hanSuDungGanNhat"), rs.getString("maThuoc"), rs.getString("maVatTu"),
                rs.getBoolean("dangApDung"), rs.getBoolean("laDuLieuMau"), rs.getString("ghiChu"));
    }

    /** Mục danh mục kèm tên thuốc / vật tư liên kết. */
    public record MucLienKet(DanhMucCapCuu muc, String tenLienKet) {}

    public List<MucLienKet> listWithLink(Connection c) throws SQLException {
        return Jdbc.list(c, "SELECT " + COLS + ", COALESCE(t.tenThuoc, v.tenVatTu) AS tenLk FROM DanhMucCapCuu m "
                        + "LEFT JOIN Thuoc t ON t.maThuoc = m.maThuoc LEFT JOIN VatTu v ON v.maVatTu = m.maVatTu ORDER BY m.dangApDung DESC, m.maMuc",
                rs -> new MucLienKet(map(rs), rs.getString("tenLk")));
    }

    public List<DanhMucCapCuu> active(Connection c) throws SQLException {
        return Jdbc.list(c, "SELECT " + COLS + " FROM DanhMucCapCuu m WHERE m.dangApDung = 1 ORDER BY m.maMuc", CapCuuDao::map);
    }

    public Optional<DanhMucCapCuu> find(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM DanhMucCapCuu m WHERE m.maMuc = ?", CapCuuDao::map, ma);
    }

    public void insert(Connection c, DanhMucCapCuu m) throws SQLException {
        Jdbc.update(c, "INSERT INTO DanhMucCapCuu (maMuc, tenMuc, donViTinh, soLuongToiThieu, soLuongHienCo, hanSuDungGanNhat, maThuoc, maVatTu, dangApDung, laDuLieuMau, ghiChu) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                m.maMuc(), m.tenMuc(), m.donViTinh(), m.soLuongToiThieu(), m.soLuongHienCo(), m.hanSuDungGanNhat(), m.maThuoc(), m.maVatTu(),
                m.dangApDung(), m.laDuLieuMau(), m.ghiChu());
    }

    /** Sửa cấu hình mục; sau khi người dùng sửa, mục không còn được đánh dấu là dữ liệu mẫu. */
    public int update(Connection c, DanhMucCapCuu m) throws SQLException {
        return Jdbc.update(c, "UPDATE DanhMucCapCuu SET tenMuc = ?, donViTinh = ?, soLuongToiThieu = ?, maThuoc = ?, maVatTu = ?, ghiChu = ?, laDuLieuMau = 0 WHERE maMuc = ?",
                m.tenMuc(), m.donViTinh(), m.soLuongToiThieu(), m.maThuoc(), m.maVatTu(), m.ghiChu(), m.maMuc());
    }

    public int setActive(Connection c, String ma, boolean active) throws SQLException {
        return Jdbc.update(c, "UPDATE DanhMucCapCuu SET dangApDung = ? WHERE maMuc = ?", active, ma);
    }

    public int setHienCo(Connection c, String ma, int soLuong, LocalDate hsd) throws SQLException {
        return Jdbc.update(c, "UPDATE DanhMucCapCuu SET soLuongHienCo = ?, hanSuDungGanNhat = ? WHERE maMuc = ?", soLuong, hsd, ma);
    }

    public void insertKiemTra(Connection c, KiemTraCapCuu k) throws SQLException {
        Jdbc.update(c, "INSERT INTO KiemTraCapCuu (maKiemTra, thoiGian, maNhanVien, ketQua, ghiChu) VALUES (?,?,?,?,?)",
                k.maKiemTra(), k.thoiGian(), k.maNhanVien(), k.ketQua(), k.ghiChu());
    }

    public int setKetQua(Connection c, String ma, KetQuaKiemTra kq, String ghiChu) throws SQLException {
        return Jdbc.update(c, "UPDATE KiemTraCapCuu SET ketQua = ?, ghiChu = ? WHERE maKiemTra = ?", kq, ghiChu, ma);
    }

    public void insertKiemTraLine(Connection c, ChiTietKiemTraCapCuu d) throws SQLException {
        Jdbc.update(c, "INSERT INTO ChiTietKiemTraCapCuu (maKiemTra, maMuc, soLuongThucTe, hanSuDung, dat) VALUES (?,?,?,?,?)",
                d.maKiemTra(), d.maMuc(), d.soLuongThucTe(), d.hanSuDung(), d.dat());
    }

    public List<KiemTraCapCuuView> history(Connection c, int max) throws SQLException {
        return Jdbc.list(c, "SELECT k.maKiemTra, k.thoiGian, k.maNhanVien, k.ketQua, k.ghiChu, nv.hoTen, "
                        + "(SELECT COUNT(*) FROM ChiTietKiemTraCapCuu d WHERE d.maKiemTra = k.maKiemTra) AS soMuc, "
                        + "(SELECT COUNT(*) FROM ChiTietKiemTraCapCuu d2 WHERE d2.maKiemTra = k.maKiemTra AND d2.dat = 1) AS soDat "
                        + "FROM KiemTraCapCuu k JOIN NhanVien nv ON nv.maNhanVien = k.maNhanVien ORDER BY k.thoiGian DESC OFFSET 0 ROWS FETCH NEXT " + max + " ROWS ONLY",
                rs -> new KiemTraCapCuuView(new KiemTraCapCuu(rs.getString("maKiemTra"), Jdbc.dateTime(rs, "thoiGian"), rs.getString("maNhanVien"),
                        KetQuaKiemTra.of(rs.getString("ketQua")), rs.getString("ghiChu")), rs.getString("hoTen"), rs.getInt("soMuc"), rs.getInt("soDat")));
    }

    public List<ChiTietKiemTraView> lines(Connection c, String maKt) throws SQLException {
        return Jdbc.list(c, "SELECT d.maKiemTra, d.maMuc, d.soLuongThucTe, d.hanSuDung, d.dat, m.tenMuc, m.donViTinh, m.soLuongToiThieu "
                        + "FROM ChiTietKiemTraCapCuu d JOIN DanhMucCapCuu m ON m.maMuc = d.maMuc WHERE d.maKiemTra = ? ORDER BY d.maMuc",
                rs -> new ChiTietKiemTraView(new ChiTietKiemTraCapCuu(rs.getString("maKiemTra"), rs.getString("maMuc"), rs.getInt("soLuongThucTe"),
                        Jdbc.date(rs, "hanSuDung"), rs.getBoolean("dat")), rs.getString("tenMuc"), rs.getString("donViTinh"), rs.getInt("soLuongToiThieu")), maKt);
    }
}
