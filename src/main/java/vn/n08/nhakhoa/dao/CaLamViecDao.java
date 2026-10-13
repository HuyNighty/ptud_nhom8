package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.model.CaLamViec;
import vn.n08.nhakhoa.util.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Bảng CaLamViec. */
public class CaLamViecDao {
    static CaLamViec map(ResultSet rs) throws SQLException {
        return new CaLamViec(rs.getString("maCa"), rs.getString("tenCa"), Jdbc.time(rs, "gioBatDau"), Jdbc.time(rs, "gioKetThuc"),
                rs.getInt("soBacSi"), rs.getInt("soYTa"), rs.getInt("soLeTan"));
    }

    public List<CaLamViec> all(Connection c) throws SQLException {
        return Jdbc.list(c, "SELECT maCa, tenCa, gioBatDau, gioKetThuc, soBacSi, soYTa, soLeTan FROM CaLamViec ORDER BY gioBatDau", CaLamViecDao::map);
    }

    public Optional<CaLamViec> find(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, "SELECT maCa, tenCa, gioBatDau, gioKetThuc, soBacSi, soYTa, soLeTan FROM CaLamViec WHERE maCa = ?", CaLamViecDao::map, ma);
    }

    public int update(Connection c, CaLamViec ca) throws SQLException {
        return Jdbc.update(c, "UPDATE CaLamViec SET tenCa = ?, gioBatDau = ?, gioKetThuc = ?, soBacSi = ?, soYTa = ?, soLeTan = ? WHERE maCa = ?",
                ca.tenCa(), ca.gioBatDau(), ca.gioKetThuc(), ca.soBacSi(), ca.soYTa(), ca.soLeTan(), ca.maCa());
    }
}
