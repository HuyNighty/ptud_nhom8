package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.dto.TaiKhoanView;
import vn.n08.nhakhoa.model.TaiKhoan;
import vn.n08.nhakhoa.model.TrangThaiNhanVien;
import vn.n08.nhakhoa.model.TrangThaiTaiKhoan;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.util.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Bảng TaiKhoan. Mọi lệnh đổi trạng thái đều kèm điều kiện trạng thái cũ để tránh ghi đè thay đổi đồng thời. */
public class TaiKhoanDao {
    static final String COLS = "tk.maTaiKhoan, tk.tenDangNhap, tk.matKhau, tk.trangThai, tk.vaiTro, tk.maNhanVien, tk.soLanSai, tk.lanDangNhapCuoi, tk.lyDoKhoa";

    static TaiKhoan map(ResultSet rs) throws SQLException {
        return new TaiKhoan(rs.getString("maTaiKhoan"), rs.getString("tenDangNhap"), rs.getString("matKhau"),
                TrangThaiTaiKhoan.of(rs.getString("trangThai")), VaiTro.of(rs.getString("vaiTro")), rs.getString("maNhanVien"),
                rs.getInt("soLanSai"), Jdbc.dateTime(rs, "lanDangNhapCuoi"), rs.getString("lyDoKhoa"));
    }

    public Optional<TaiKhoan> findByUsername(Connection c, String ten) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM TaiKhoan tk WHERE tk.tenDangNhap = ?", TaiKhoanDao::map, ten);
    }

    public Optional<TaiKhoan> find(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM TaiKhoan tk WHERE tk.maTaiKhoan = ?", TaiKhoanDao::map, ma);
    }

    public Optional<TaiKhoan> findByNhanVien(Connection c, String maNv) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM TaiKhoan tk WHERE tk.maNhanVien = ?", TaiKhoanDao::map, maNv);
    }

    public List<TaiKhoanView> search(Connection c, String tuKhoa, VaiTro vaiTro, TrangThaiTaiKhoan trangThai) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT " + COLS + ", nv.hoTen, nv.chucDanh, nv.trangThai AS ttNv FROM TaiKhoan tk "
                + "JOIN NhanVien nv ON nv.maNhanVien = tk.maNhanVien WHERE 1 = 1 ");
        List<Object> p = new ArrayList<>();
        if (tuKhoa != null && !tuKhoa.isBlank()) {
            sql.append("AND (LOWER(tk.tenDangNhap) LIKE ? OR LOWER(nv.hoTen) LIKE ? OR LOWER(tk.maNhanVien) LIKE ?) ");
            String l = Jdbc.like(tuKhoa);
            p.add(l); p.add(l); p.add(l);
        }
        if (vaiTro != null) { sql.append("AND tk.vaiTro = ? "); p.add(vaiTro); }
        if (trangThai != null) { sql.append("AND tk.trangThai = ? "); p.add(trangThai); }
        sql.append("ORDER BY tk.maTaiKhoan");
        return Jdbc.list(c, sql.toString(), rs -> new TaiKhoanView(map(rs), rs.getString("hoTen"), VaiTro.of(rs.getString("chucDanh")),
                TrangThaiNhanVien.of(rs.getString("ttNv"))), p.toArray());
    }

    public boolean usernameUsed(Connection c, String ten) throws SQLException {
        return Jdbc.count(c, "SELECT COUNT(*) FROM TaiKhoan WHERE tenDangNhap = ?", ten) > 0;
    }

    public void insert(Connection c, TaiKhoan t) throws SQLException {
        Jdbc.update(c, "INSERT INTO TaiKhoan (maTaiKhoan, tenDangNhap, matKhau, trangThai, vaiTro, maNhanVien, soLanSai, lanDangNhapCuoi, lyDoKhoa) "
                        + "VALUES (?,?,?,?,?,?,?,?,?)",
                t.maTaiKhoan(), t.tenDangNhap(), t.matKhau(), t.trangThai(), t.vaiTro(), t.maNhanVien(), t.soLanSai(), t.lanDangNhapCuoi(), t.lyDoKhoa());
    }

    public int setTrangThai(Connection c, String ma, TrangThaiTaiKhoan cu, TrangThaiTaiKhoan moi, String lyDo) throws SQLException {
        String resetSai = moi == TrangThaiTaiKhoan.HOAT_DONG ? ", soLanSai = 0" : "";
        return Jdbc.update(c, "UPDATE TaiKhoan SET trangThai = ?, lyDoKhoa = ?" + resetSai + " WHERE maTaiKhoan = ? AND trangThai = ?",
                moi, lyDo, ma, cu);
    }

    /** Vô hiệu tài khoản (không phụ thuộc trạng thái cũ, trừ khi đã vô hiệu). */
    public int voHieu(Connection c, String ma, String lyDo) throws SQLException {
        return Jdbc.update(c, "UPDATE TaiKhoan SET trangThai = 'VO_HIEU', lyDoKhoa = ? WHERE maTaiKhoan = ? AND trangThai <> 'VO_HIEU'", lyDo, ma);
    }

    public int setPassword(Connection c, String ma, String hash) throws SQLException {
        return Jdbc.update(c, "UPDATE TaiKhoan SET matKhau = ?, soLanSai = 0 WHERE maTaiKhoan = ? AND trangThai <> 'VO_HIEU'", hash, ma);
    }

    public int setVaiTro(Connection c, String ma, VaiTro v) throws SQLException {
        return Jdbc.update(c, "UPDATE TaiKhoan SET vaiTro = ? WHERE maTaiKhoan = ? AND trangThai <> 'VO_HIEU'", v, ma);
    }

    public int loginFailed(Connection c, String ma, int soLanSai) throws SQLException {
        return Jdbc.update(c, "UPDATE TaiKhoan SET soLanSai = ? WHERE maTaiKhoan = ?", soLanSai, ma);
    }

    public int loginOk(Connection c, String ma, LocalDateTime t) throws SQLException {
        return Jdbc.update(c, "UPDATE TaiKhoan SET soLanSai = 0, lanDangNhapCuoi = ? WHERE maTaiKhoan = ?", t, ma);
    }
}
