package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.dto.NhanVienView;
import vn.n08.nhakhoa.model.GioiTinh;
import vn.n08.nhakhoa.model.NhanVien;
import vn.n08.nhakhoa.model.TrangThaiNhanVien;
import vn.n08.nhakhoa.model.TrangThaiTaiKhoan;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.util.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Bảng NhanVien. */
public class NhanVienDao {
    static final String COLS = "nv.maNhanVien, nv.hoTen, nv.soDienThoai, nv.chucDanh, nv.trangThai, nv.ngaySinh, nv.gioiTinh, "
            + "nv.email, nv.ngayVaoLam, nv.chuyenMon, nv.ngayNghiViec";

    public static NhanVien map(ResultSet rs) throws SQLException {
        return new NhanVien(rs.getString("maNhanVien"), rs.getString("hoTen"), rs.getString("soDienThoai"),
                VaiTro.of(rs.getString("chucDanh")), TrangThaiNhanVien.of(rs.getString("trangThai")), Jdbc.date(rs, "ngaySinh"),
                GioiTinh.of(rs.getString("gioiTinh")), rs.getString("email"), Jdbc.date(rs, "ngayVaoLam"),
                rs.getString("chuyenMon"), Jdbc.date(rs, "ngayNghiViec"));
    }

    public Optional<NhanVien> find(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM NhanVien nv WHERE nv.maNhanVien = ?", NhanVienDao::map, ma);
    }

    public List<NhanVienView> search(Connection c, String tuKhoa, VaiTro chucDanh, TrangThaiNhanVien trangThai) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT " + COLS + ", tk.maTaiKhoan, tk.tenDangNhap, tk.trangThai AS ttTk "
                + "FROM NhanVien nv LEFT JOIN TaiKhoan tk ON tk.maNhanVien = nv.maNhanVien WHERE 1 = 1 ");
        List<Object> p = new ArrayList<>();
        if (tuKhoa != null && !tuKhoa.isBlank()) {
            sql.append("AND (LOWER(nv.hoTen) LIKE ? OR LOWER(nv.maNhanVien) LIKE ? OR nv.soDienThoai LIKE ?) ");
            String l = Jdbc.like(tuKhoa);
            p.add(l); p.add(l); p.add(l);
        }
        if (chucDanh != null) { sql.append("AND nv.chucDanh = ? "); p.add(chucDanh); }
        if (trangThai != null) { sql.append("AND nv.trangThai = ? "); p.add(trangThai); }
        sql.append("ORDER BY nv.maNhanVien");
        return Jdbc.list(c, sql.toString(), rs -> new NhanVienView(map(rs), rs.getString("maTaiKhoan"), rs.getString("tenDangNhap"),
                TrangThaiTaiKhoan.of(rs.getString("ttTk"))), p.toArray());
    }

    /** Nhân viên đang làm việc theo chức danh (VD: danh sách bác sĩ để chọn). */
    public List<NhanVien> active(Connection c, VaiTro chucDanh) throws SQLException {
        if (chucDanh == null) {
            return Jdbc.list(c, "SELECT " + COLS + " FROM NhanVien nv WHERE nv.trangThai = 'DANG_LAM_VIEC' ORDER BY nv.hoTen", NhanVienDao::map);
        }
        return Jdbc.list(c, "SELECT " + COLS + " FROM NhanVien nv WHERE nv.trangThai = 'DANG_LAM_VIEC' AND nv.chucDanh = ? ORDER BY nv.hoTen",
                NhanVienDao::map, chucDanh);
    }

    public boolean phoneUsed(Connection c, String sdt, String exceptMa) throws SQLException {
        return Jdbc.count(c, "SELECT COUNT(*) FROM NhanVien WHERE soDienThoai = ? AND maNhanVien <> ?", sdt, exceptMa == null ? "" : exceptMa) > 0;
    }

    public void insert(Connection c, NhanVien n) throws SQLException {
        Jdbc.update(c, "INSERT INTO NhanVien (maNhanVien, hoTen, soDienThoai, chucDanh, trangThai, ngaySinh, gioiTinh, email, ngayVaoLam, chuyenMon, ngayNghiViec) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                n.maNhanVien(), n.hoTen(), n.soDienThoai(), n.chucDanh(), n.trangThai(), n.ngaySinh(), n.gioiTinh(), n.email(),
                n.ngayVaoLam(), n.chuyenMon(), n.ngayNghiViec());
    }

    /** Sửa thông tin hồ sơ (không đổi chức danh, trạng thái). */
    public int update(Connection c, NhanVien n) throws SQLException {
        return Jdbc.update(c, "UPDATE NhanVien SET hoTen = ?, soDienThoai = ?, ngaySinh = ?, gioiTinh = ?, email = ?, ngayVaoLam = ?, chuyenMon = ? "
                        + "WHERE maNhanVien = ? AND trangThai = 'DANG_LAM_VIEC'",
                n.hoTen(), n.soDienThoai(), n.ngaySinh(), n.gioiTinh(), n.email(), n.ngayVaoLam(), n.chuyenMon(), n.maNhanVien());
    }

    public int setNghiViec(Connection c, String ma, LocalDate ngay) throws SQLException {
        return Jdbc.update(c, "UPDATE NhanVien SET trangThai = 'NGHI_VIEC', ngayNghiViec = ? WHERE maNhanVien = ? AND trangThai = 'DANG_LAM_VIEC'", ngay, ma);
    }
}
