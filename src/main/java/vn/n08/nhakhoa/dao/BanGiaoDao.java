package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.dto.BanGiaoView;
import vn.n08.nhakhoa.model.BanGiaoCa;
import vn.n08.nhakhoa.model.TrangThaiBanGiao;
import vn.n08.nhakhoa.util.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Bảng BanGiaoCa. */
public class BanGiaoDao {
    static final String COLS = "bg.maBanGiao, bg.thoiGian, bg.noiDung, bg.trangThai, bg.ngay, bg.maNguoiGiao, bg.maNguoiNhan, bg.maCaGiao, bg.maCaNhan, "
            + "bg.coChenhLech, bg.maTaiKhoanTao, bg.maTaiKhoanXacNhan, bg.thoiGianXacNhan, bg.ghiChuXacNhan, bg.maTaiKhoanHuy, bg.thoiGianHuy, bg.lyDoHuy";
    private static final String VIEW = "SELECT " + COLS + ", g.hoTen AS tenGiao, n.hoTen AS tenNhan, cg.tenCa AS caGiao, cn.tenCa AS caNhan, "
            + "t1.tenDangNhap AS tkTao, t2.tenDangNhap AS tkXn, t3.tenDangNhap AS tkHuy FROM BanGiaoCa bg "
            + "JOIN NhanVien g ON g.maNhanVien = bg.maNguoiGiao JOIN NhanVien n ON n.maNhanVien = bg.maNguoiNhan "
            + "JOIN CaLamViec cg ON cg.maCa = bg.maCaGiao JOIN CaLamViec cn ON cn.maCa = bg.maCaNhan "
            + "JOIN TaiKhoan t1 ON t1.maTaiKhoan = bg.maTaiKhoanTao LEFT JOIN TaiKhoan t2 ON t2.maTaiKhoan = bg.maTaiKhoanXacNhan "
            + "LEFT JOIN TaiKhoan t3 ON t3.maTaiKhoan = bg.maTaiKhoanHuy ";

    static BanGiaoCa map(ResultSet rs) throws SQLException {
        return new BanGiaoCa(rs.getString("maBanGiao"), Jdbc.dateTime(rs, "thoiGian"), rs.getString("noiDung"),
                TrangThaiBanGiao.of(rs.getString("trangThai")), Jdbc.date(rs, "ngay"), rs.getString("maNguoiGiao"), rs.getString("maNguoiNhan"),
                rs.getString("maCaGiao"), rs.getString("maCaNhan"), rs.getBoolean("coChenhLech"), rs.getString("maTaiKhoanTao"),
                rs.getString("maTaiKhoanXacNhan"), Jdbc.dateTime(rs, "thoiGianXacNhan"), rs.getString("ghiChuXacNhan"),
                rs.getString("maTaiKhoanHuy"), Jdbc.dateTime(rs, "thoiGianHuy"), rs.getString("lyDoHuy"));
    }

    static BanGiaoView mapView(ResultSet rs) throws SQLException {
        return new BanGiaoView(map(rs), rs.getString("tenGiao"), rs.getString("tenNhan"), rs.getString("caGiao"), rs.getString("caNhan"),
                rs.getString("tkTao"), rs.getString("tkXn"), rs.getString("tkHuy"));
    }

    public Optional<BanGiaoCa> find(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM BanGiaoCa bg WHERE bg.maBanGiao = ?", BanGiaoDao::map, ma);
    }

    public List<BanGiaoView> search(Connection c, LocalDate tu, LocalDate den, TrangThaiBanGiao tt) throws SQLException {
        StringBuilder sql = new StringBuilder(VIEW).append("WHERE bg.ngay >= ? AND bg.ngay <= ? ");
        List<Object> p = new ArrayList<>(List.of(tu, den));
        if (tt != null) { sql.append("AND bg.trangThai = ? "); p.add(tt); }
        sql.append("ORDER BY bg.thoiGian DESC");
        return Jdbc.list(c, sql.toString(), BanGiaoDao::mapView, p.toArray());
    }

    /** Biên bản đang chờ mà nhân viên là người giao hoặc người nhận. */
    public List<BanGiaoView> pendingOf(Connection c, String maNv) throws SQLException {
        return Jdbc.list(c, VIEW + "WHERE bg.trangThai = 'CHO_BAN_GIAO' AND (bg.maNguoiGiao = ? OR bg.maNguoiNhan = ?) ORDER BY bg.thoiGian",
                BanGiaoDao::mapView, maNv, maNv);
    }

    public long countPending(Connection c) throws SQLException {
        return Jdbc.count(c, "SELECT COUNT(*) FROM BanGiaoCa WHERE trangThai = 'CHO_BAN_GIAO'");
    }

    public void insert(Connection c, BanGiaoCa b) throws SQLException {
        Jdbc.update(c, "INSERT INTO BanGiaoCa (maBanGiao, thoiGian, noiDung, trangThai, ngay, maNguoiGiao, maNguoiNhan, maCaGiao, maCaNhan, coChenhLech, maTaiKhoanTao) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                b.maBanGiao(), b.thoiGian(), b.noiDung(), b.trangThai(), b.ngay(), b.maNguoiGiao(), b.maNguoiNhan(), b.maCaGiao(), b.maCaNhan(),
                b.coChenhLech(), b.maTaiKhoanTao());
    }

    public int confirm(Connection c, String ma, String maTk, LocalDateTime t, String ghiChu) throws SQLException {
        return Jdbc.update(c, "UPDATE BanGiaoCa SET trangThai = 'DA_BAN_GIAO', maTaiKhoanXacNhan = ?, thoiGianXacNhan = ?, ghiChuXacNhan = ? "
                + "WHERE maBanGiao = ? AND trangThai = 'CHO_BAN_GIAO' AND maTaiKhoanTao <> ?", maTk, t, ghiChu, ma, maTk);
    }

    public int cancel(Connection c, String ma, String maTk, LocalDateTime t, String lyDo) throws SQLException {
        return Jdbc.update(c, "UPDATE BanGiaoCa SET trangThai = 'DA_HUY', maTaiKhoanHuy = ?, thoiGianHuy = ?, lyDoHuy = ? "
                + "WHERE maBanGiao = ? AND trangThai = 'CHO_BAN_GIAO'", maTk, t, lyDo, ma);
    }
}
