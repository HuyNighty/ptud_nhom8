package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.dto.YeuCauHuyView;
import vn.n08.nhakhoa.model.TrangThaiDuyet;
import vn.n08.nhakhoa.model.TrangThaiHoaDon;
import vn.n08.nhakhoa.model.YeuCauHuyHoaDon;
import vn.n08.nhakhoa.util.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Bảng YeuCauHuyHoaDon. Lệnh duyệt kèm điều kiện người duyệt khác người tạo (thêm vào CHECK của CSDL). */
public class YeuCauHuyDao {
    static final String COLS = "yc.maYeuCau, yc.maHoaDon, yc.maTaiKhoanTao, yc.thoiGianTao, yc.lyDo, yc.trangThai, yc.maTaiKhoanDuyet, yc.thoiGianDuyet, yc.lyDoDuyet";

    static YeuCauHuyHoaDon map(ResultSet rs) throws SQLException {
        return new YeuCauHuyHoaDon(rs.getString("maYeuCau"), rs.getString("maHoaDon"), rs.getString("maTaiKhoanTao"), Jdbc.dateTime(rs, "thoiGianTao"),
                rs.getString("lyDo"), TrangThaiDuyet.of(rs.getString("trangThai")), rs.getString("maTaiKhoanDuyet"), Jdbc.dateTime(rs, "thoiGianDuyet"),
                rs.getString("lyDoDuyet"));
    }

    private static final String VIEW = "SELECT " + COLS + ", t1.tenDangNhap AS tkTao, n1.hoTen AS tenTao, t2.tenDangNhap AS tkDuyet, n2.hoTen AS tenDuyet, "
            + "bn.hoTen AS tenBn, hd.trangThai AS ttHd, " + HoaDonDao.TONG + " AS tongTien, " + HoaDonDao.DA_TRA + " AS daTra "
            + "FROM YeuCauHuyHoaDon yc JOIN HoaDon hd ON hd.maHoaDon = yc.maHoaDon JOIN BenhNhan bn ON bn.maBenhNhan = hd.maBenhNhan "
            + "JOIN TaiKhoan t1 ON t1.maTaiKhoan = yc.maTaiKhoanTao JOIN NhanVien n1 ON n1.maNhanVien = t1.maNhanVien "
            + "LEFT JOIN TaiKhoan t2 ON t2.maTaiKhoan = yc.maTaiKhoanDuyet LEFT JOIN NhanVien n2 ON n2.maNhanVien = t2.maNhanVien ";

    static YeuCauHuyView mapView(ResultSet rs) throws SQLException {
        return new YeuCauHuyView(map(rs), rs.getString("tkTao"), rs.getString("tenTao"), rs.getString("tkDuyet"), rs.getString("tenDuyet"),
                rs.getString("tenBn"), Jdbc.money(rs, "tongTien"), Jdbc.money(rs, "daTra"), TrangThaiHoaDon.of(rs.getString("ttHd")));
    }

    public Optional<YeuCauHuyHoaDon> find(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM YeuCauHuyHoaDon yc WHERE yc.maYeuCau = ?", YeuCauHuyDao::map, ma);
    }

    public List<YeuCauHuyView> search(Connection c, TrangThaiDuyet tt) throws SQLException {
        if (tt == null) return Jdbc.list(c, VIEW + "ORDER BY yc.thoiGianTao DESC", YeuCauHuyDao::mapView);
        return Jdbc.list(c, VIEW + "WHERE yc.trangThai = ? ORDER BY yc.thoiGianTao DESC", YeuCauHuyDao::mapView, tt);
    }

    public List<YeuCauHuyView> byInvoice(Connection c, String maHd) throws SQLException {
        return Jdbc.list(c, VIEW + "WHERE yc.maHoaDon = ? ORDER BY yc.thoiGianTao", YeuCauHuyDao::mapView, maHd);
    }

    public boolean hasPending(Connection c, String maHd) throws SQLException {
        return Jdbc.count(c, "SELECT COUNT(*) FROM YeuCauHuyHoaDon WHERE maHoaDon = ? AND trangThai = 'CHO_DUYET'", maHd) > 0;
    }

    public long countPending(Connection c) throws SQLException {
        return Jdbc.count(c, "SELECT COUNT(*) FROM YeuCauHuyHoaDon WHERE trangThai = 'CHO_DUYET'");
    }

    public void insert(Connection c, YeuCauHuyHoaDon y) throws SQLException {
        Jdbc.update(c, "INSERT INTO YeuCauHuyHoaDon (maYeuCau, maHoaDon, maTaiKhoanTao, thoiGianTao, lyDo, trangThai) VALUES (?,?,?,?,?,?)",
                y.maYeuCau(), y.maHoaDon(), y.maTaiKhoanTao(), y.thoiGianTao(), y.lyDo(), y.trangThai());
    }

    public int decide(Connection c, String ma, TrangThaiDuyet ketQua, String maTkDuyet, LocalDateTime t, String lyDoDuyet) throws SQLException {
        return Jdbc.update(c, "UPDATE YeuCauHuyHoaDon SET trangThai = ?, maTaiKhoanDuyet = ?, thoiGianDuyet = ?, lyDoDuyet = ? "
                + "WHERE maYeuCau = ? AND trangThai = 'CHO_DUYET' AND maTaiKhoanTao <> ?", ketQua, maTkDuyet, t, lyDoDuyet, ma, maTkDuyet);
    }
}
