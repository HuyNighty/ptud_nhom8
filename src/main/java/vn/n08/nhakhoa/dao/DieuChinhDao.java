package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.dto.DieuChinhDong;
import vn.n08.nhakhoa.dto.DieuChinhView;
import vn.n08.nhakhoa.model.ChiTietDieuChinhKho;
import vn.n08.nhakhoa.model.LoaiDieuChinh;
import vn.n08.nhakhoa.model.PhieuDieuChinhKho;
import vn.n08.nhakhoa.model.TrangThaiDuyet;
import vn.n08.nhakhoa.util.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Bảng PhieuDieuChinhKho + ChiTietDieuChinhKho. */
public class DieuChinhDao {
    static final String COLS = "p.maPhieu, p.loai, p.maTaiKhoanTao, p.thoiGianTao, p.lyDo, p.trangThai, p.maTaiKhoanDuyet, p.thoiGianDuyet, p.ghiChuDuyet";

    static PhieuDieuChinhKho map(ResultSet rs) throws SQLException {
        return new PhieuDieuChinhKho(rs.getString("maPhieu"), LoaiDieuChinh.of(rs.getString("loai")), rs.getString("maTaiKhoanTao"),
                Jdbc.dateTime(rs, "thoiGianTao"), rs.getString("lyDo"), TrangThaiDuyet.of(rs.getString("trangThai")), rs.getString("maTaiKhoanDuyet"),
                Jdbc.dateTime(rs, "thoiGianDuyet"), rs.getString("ghiChuDuyet"));
    }

    private static final String VIEW = "SELECT " + COLS + ", t1.tenDangNhap AS tkTao, n1.hoTen AS tenTao, t2.tenDangNhap AS tkDuyet, n2.hoTen AS tenDuyet, "
            + "(SELECT COUNT(*) FROM ChiTietDieuChinhKho d WHERE d.maPhieu = p.maPhieu) AS soDong, "
            + "(SELECT COALESCE(SUM(d2.soLuongThucTe - d2.soLuongHeThong), 0) FROM ChiTietDieuChinhKho d2 WHERE d2.maPhieu = p.maPhieu) AS chenh "
            + "FROM PhieuDieuChinhKho p JOIN TaiKhoan t1 ON t1.maTaiKhoan = p.maTaiKhoanTao JOIN NhanVien n1 ON n1.maNhanVien = t1.maNhanVien "
            + "LEFT JOIN TaiKhoan t2 ON t2.maTaiKhoan = p.maTaiKhoanDuyet LEFT JOIN NhanVien n2 ON n2.maNhanVien = t2.maNhanVien ";

    static DieuChinhView mapView(ResultSet rs) throws SQLException {
        return new DieuChinhView(map(rs), rs.getString("tkTao"), rs.getString("tenTao"), rs.getString("tkDuyet"), rs.getString("tenDuyet"),
                rs.getInt("soDong"), rs.getInt("chenh"));
    }

    public Optional<PhieuDieuChinhKho> find(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM PhieuDieuChinhKho p WHERE p.maPhieu = ?", DieuChinhDao::map, ma);
    }

    public List<DieuChinhView> search(Connection c, TrangThaiDuyet tt) throws SQLException {
        if (tt == null) return Jdbc.list(c, VIEW + "ORDER BY p.thoiGianTao DESC", DieuChinhDao::mapView);
        return Jdbc.list(c, VIEW + "WHERE p.trangThai = ? ORDER BY p.thoiGianTao DESC", DieuChinhDao::mapView, tt);
    }

    public List<DieuChinhDong> lines(Connection c, String ma) throws SQLException {
        return Jdbc.list(c, "SELECT d.maPhieu, d.maLo, d.soLuongHeThong, d.soLuongThucTe, lo.soLo, lo.hanSuDung, lo.soLuongTon, "
                        + "COALESCE(t.tenThuoc, v.tenVatTu) AS tenHang FROM ChiTietDieuChinhKho d JOIN LoHang lo ON lo.maLo = d.maLo "
                        + "LEFT JOIN Thuoc t ON t.maThuoc = lo.maThuoc LEFT JOIN VatTu v ON v.maVatTu = lo.maVatTu WHERE d.maPhieu = ? ORDER BY d.maLo",
                rs -> new DieuChinhDong(new ChiTietDieuChinhKho(rs.getString("maPhieu"), rs.getString("maLo"), rs.getInt("soLuongHeThong"),
                        rs.getInt("soLuongThucTe")), rs.getString("soLo"), rs.getString("tenHang"), Jdbc.date(rs, "hanSuDung"), rs.getInt("soLuongTon")), ma);
    }

    /** Lô đang nằm trong một phiếu chờ duyệt khác. */
    public boolean lotPending(Connection c, String maLo) throws SQLException {
        return Jdbc.count(c, "SELECT COUNT(*) FROM ChiTietDieuChinhKho d JOIN PhieuDieuChinhKho p ON p.maPhieu = d.maPhieu "
                + "WHERE d.maLo = ? AND p.trangThai = 'CHO_DUYET'", maLo) > 0;
    }

    public long countPending(Connection c) throws SQLException {
        return Jdbc.count(c, "SELECT COUNT(*) FROM PhieuDieuChinhKho WHERE trangThai = 'CHO_DUYET'");
    }

    public void insert(Connection c, PhieuDieuChinhKho p) throws SQLException {
        Jdbc.update(c, "INSERT INTO PhieuDieuChinhKho (maPhieu, loai, maTaiKhoanTao, thoiGianTao, lyDo, trangThai) VALUES (?,?,?,?,?,?)",
                p.maPhieu(), p.loai(), p.maTaiKhoanTao(), p.thoiGianTao(), p.lyDo(), p.trangThai());
    }

    public void insertLine(Connection c, ChiTietDieuChinhKho d) throws SQLException {
        Jdbc.update(c, "INSERT INTO ChiTietDieuChinhKho (maPhieu, maLo, soLuongHeThong, soLuongThucTe) VALUES (?,?,?,?)",
                d.maPhieu(), d.maLo(), d.soLuongHeThong(), d.soLuongThucTe());
    }

    public int decide(Connection c, String ma, TrangThaiDuyet kq, String maTk, LocalDateTime t, String ghiChu) throws SQLException {
        return Jdbc.update(c, "UPDATE PhieuDieuChinhKho SET trangThai = ?, maTaiKhoanDuyet = ?, thoiGianDuyet = ?, ghiChuDuyet = ? "
                + "WHERE maPhieu = ? AND trangThai = 'CHO_DUYET' AND maTaiKhoanTao <> ?", kq, maTk, t, ghiChu, ma, maTk);
    }
}
