package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.model.NhaCungCap;
import vn.n08.nhakhoa.model.TrangThaiNCC;
import vn.n08.nhakhoa.util.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Bảng NhaCungCap. Không xóa (lô hàng tham chiếu); ngừng hợp tác = đổi trạng thái. */
public class NhaCungCapDao {
    static NhaCungCap map(ResultSet rs) throws SQLException {
        return new NhaCungCap(rs.getString("maNhaCungCap"), rs.getString("tenNhaCungCap"), rs.getString("diaChi"), rs.getString("soDienThoai"),
                rs.getString("email"), TrangThaiNCC.of(rs.getString("trangThai")));
    }

    private static final String SEL = "SELECT maNhaCungCap, tenNhaCungCap, diaChi, soDienThoai, email, trangThai FROM NhaCungCap ";

    public List<NhaCungCap> search(Connection c, String tuKhoa) throws SQLException {
        if (tuKhoa == null || tuKhoa.isBlank()) return Jdbc.list(c, SEL + "ORDER BY maNhaCungCap", NhaCungCapDao::map);
        String l = Jdbc.like(tuKhoa);
        return Jdbc.list(c, SEL + "WHERE LOWER(tenNhaCungCap) LIKE ? OR LOWER(maNhaCungCap) LIKE ? OR soDienThoai LIKE ? ORDER BY maNhaCungCap",
                NhaCungCapDao::map, l, l, l);
    }

    public List<NhaCungCap> active(Connection c) throws SQLException {
        return Jdbc.list(c, SEL + "WHERE trangThai = 'DANG_HOP_TAC' ORDER BY tenNhaCungCap", NhaCungCapDao::map);
    }

    public Optional<NhaCungCap> find(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, SEL + "WHERE maNhaCungCap = ?", NhaCungCapDao::map, ma);
    }

    public void insert(Connection c, NhaCungCap n) throws SQLException {
        Jdbc.update(c, "INSERT INTO NhaCungCap (maNhaCungCap, tenNhaCungCap, diaChi, soDienThoai, email, trangThai) VALUES (?,?,?,?,?,?)",
                n.maNhaCungCap(), n.tenNhaCungCap(), n.diaChi(), n.soDienThoai(), n.email(), n.trangThai());
    }

    public int update(Connection c, NhaCungCap n) throws SQLException {
        return Jdbc.update(c, "UPDATE NhaCungCap SET tenNhaCungCap = ?, diaChi = ?, soDienThoai = ?, email = ? WHERE maNhaCungCap = ?",
                n.tenNhaCungCap(), n.diaChi(), n.soDienThoai(), n.email(), n.maNhaCungCap());
    }

    public int setTrangThai(Connection c, String ma, TrangThaiNCC tt) throws SQLException {
        return Jdbc.update(c, "UPDATE NhaCungCap SET trangThai = ? WHERE maNhaCungCap = ?", tt, ma);
    }

    public long countLots(Connection c, String ma) throws SQLException {
        return Jdbc.count(c, "SELECT COUNT(*) FROM LoHang WHERE maNhaCungCap = ?", ma);
    }
}
