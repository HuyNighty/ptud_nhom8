package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.model.DichVu;
import vn.n08.nhakhoa.util.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Bảng DichVu (bảng giá). */
public class DichVuDao {
    static DichVu map(ResultSet rs) throws SQLException {
        return new DichVu(rs.getString("maDichVu"), rs.getString("tenDichVu"), Jdbc.money(rs, "donGia"), rs.getString("moTa"),
                rs.getBoolean("laCanLamSang"), rs.getBoolean("dangApDung"));
    }

    public List<DichVu> all(Connection c, boolean chiDangApDung) throws SQLException {
        return Jdbc.list(c, "SELECT maDichVu, tenDichVu, donGia, moTa, laCanLamSang, dangApDung FROM DichVu "
                + (chiDangApDung ? "WHERE dangApDung = 1 " : "") + "ORDER BY maDichVu", DichVuDao::map);
    }

    public void insert(Connection c, DichVu d) throws SQLException {
        Jdbc.update(c, "INSERT INTO DichVu (maDichVu, tenDichVu, donGia, moTa, laCanLamSang, dangApDung) VALUES (?,?,?,?,?,?)",
                d.maDichVu(), d.tenDichVu(), d.donGia(), d.moTa(), d.laCanLamSang(), d.dangApDung());
    }

    public int update(Connection c, DichVu d) throws SQLException {
        return Jdbc.update(c, "UPDATE DichVu SET tenDichVu = ?, donGia = ?, moTa = ?, laCanLamSang = ?, dangApDung = ? WHERE maDichVu = ?",
                d.tenDichVu(), d.donGia(), d.moTa(), d.laCanLamSang(), d.dangApDung(), d.maDichVu());
    }

    public boolean nameUsed(Connection c, String ten, String exceptMa) throws SQLException {
        return Jdbc.count(c, "SELECT COUNT(*) FROM DichVu WHERE LOWER(tenDichVu) = ? AND maDichVu <> ?", ten.trim().toLowerCase(), exceptMa == null ? "" : exceptMa) > 0;
    }

    public Optional<DichVu> find(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, "SELECT maDichVu, tenDichVu, donGia, moTa, laCanLamSang, dangApDung FROM DichVu WHERE maDichVu = ?", DichVuDao::map, ma);
    }
}
