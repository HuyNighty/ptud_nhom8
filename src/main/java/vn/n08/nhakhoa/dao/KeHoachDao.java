package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.dto.ChiTietDieuTriView;
import vn.n08.nhakhoa.dto.KeHoachView;
import vn.n08.nhakhoa.dto.TaiKhamItem;
import vn.n08.nhakhoa.model.ChiTietDieuTri;
import vn.n08.nhakhoa.model.KeHoachDieuTri;
import vn.n08.nhakhoa.model.TrangThaiDieuTri;
import vn.n08.nhakhoa.model.TrangThaiKeHoach;
import vn.n08.nhakhoa.util.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Bảng KeHoachDieuTri + ChiTietDieuTri. */
public class KeHoachDao {
    static final String COLS = "kh.maKeHoach, kh.maHoSo, kh.maPhieuKham, kh.ngayLap, kh.mucTieu, kh.trangThai, kh.maNhanVien, kh.lyDoHuy";
    private static final String VIEW = "SELECT " + COLS + ", bn.maBenhNhan, bn.hoTen AS tenBn, bs.hoTen AS tenBs, "
            + "(SELECT COUNT(*) FROM ChiTietDieuTri c1 WHERE c1.maKeHoach = kh.maKeHoach AND c1.trangThai <> 'DA_HUY') AS soHm, "
            + "(SELECT COUNT(*) FROM ChiTietDieuTri c2 WHERE c2.maKeHoach = kh.maKeHoach AND c2.trangThai = 'DA_THUC_HIEN') AS soXong, "
            + "(SELECT COALESCE(SUM(d.donGia), 0) FROM ChiTietDieuTri c3 JOIN DichVu d ON d.maDichVu = c3.maDichVu "
            + "WHERE c3.maKeHoach = kh.maKeHoach AND c3.trangThai <> 'DA_HUY') AS chiPhi "
            + "FROM KeHoachDieuTri kh JOIN HoSoBenhAn hs ON hs.maHoSo = kh.maHoSo JOIN BenhNhan bn ON bn.maBenhNhan = hs.maBenhNhan "
            + "JOIN NhanVien bs ON bs.maNhanVien = kh.maNhanVien ";
    static final String CT_COLS = "ct.maChiTiet, ct.maKeHoach, ct.maDichVu, ct.viTriRang, ct.ngayDuKien, ct.ngayThucHien, ct.ketQua, ct.trangThai, "
            + "ct.maNhanVienThucHien, ct.ghiChu";

    static KeHoachDieuTri map(ResultSet rs) throws SQLException {
        return new KeHoachDieuTri(rs.getString("maKeHoach"), rs.getString("maHoSo"), rs.getString("maPhieuKham"), Jdbc.date(rs, "ngayLap"),
                rs.getString("mucTieu"), TrangThaiKeHoach.of(rs.getString("trangThai")), rs.getString("maNhanVien"), rs.getString("lyDoHuy"));
    }

    static KeHoachView mapView(ResultSet rs) throws SQLException {
        return new KeHoachView(map(rs), rs.getString("maBenhNhan"), rs.getString("tenBn"), rs.getString("tenBs"), rs.getInt("soHm"),
                rs.getInt("soXong"), Jdbc.money(rs, "chiPhi"));
    }

    static ChiTietDieuTri mapCt(ResultSet rs) throws SQLException {
        return new ChiTietDieuTri(rs.getInt("maChiTiet"), rs.getString("maKeHoach"), rs.getString("maDichVu"), rs.getString("viTriRang"),
                Jdbc.date(rs, "ngayDuKien"), Jdbc.date(rs, "ngayThucHien"), rs.getString("ketQua"), TrangThaiDieuTri.of(rs.getString("trangThai")),
                rs.getString("maNhanVienThucHien"), rs.getString("ghiChu"));
    }

    static ChiTietDieuTriView mapCtView(ResultSet rs) throws SQLException {
        return new ChiTietDieuTriView(mapCt(rs), rs.getString("tenDichVu"), Jdbc.money(rs, "donGia"), rs.getString("tenBsTh"));
    }

    private static final String CT_VIEW = "SELECT " + CT_COLS + ", dv.tenDichVu, dv.donGia, th.hoTen AS tenBsTh FROM ChiTietDieuTri ct "
            + "JOIN DichVu dv ON dv.maDichVu = ct.maDichVu LEFT JOIN NhanVien th ON th.maNhanVien = ct.maNhanVienThucHien ";

    public Optional<KeHoachDieuTri> find(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM KeHoachDieuTri kh WHERE kh.maKeHoach = ?", KeHoachDao::map, ma);
    }

    public Optional<KeHoachView> findView(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, VIEW + "WHERE kh.maKeHoach = ?", KeHoachDao::mapView, ma);
    }

    public List<KeHoachView> search(Connection c, TrangThaiKeHoach tt, String tuKhoa) throws SQLException {
        StringBuilder sql = new StringBuilder(VIEW).append("WHERE 1 = 1 ");
        List<Object> p = new ArrayList<>();
        if (tt != null) { sql.append("AND kh.trangThai = ? "); p.add(tt); }
        if (tuKhoa != null && !tuKhoa.isBlank()) {
            sql.append("AND (LOWER(bn.hoTen) LIKE ? OR LOWER(kh.maKeHoach) LIKE ? OR LOWER(bn.maBenhNhan) LIKE ? OR LOWER(kh.mucTieu) LIKE ?) ");
            String l = Jdbc.like(tuKhoa);
            p.add(l); p.add(l); p.add(l); p.add(l);
        }
        sql.append("ORDER BY kh.ngayLap DESC, kh.maKeHoach DESC");
        return Jdbc.list(c, sql.toString(), KeHoachDao::mapView, p.toArray());
    }

    public List<KeHoachView> byPatient(Connection c, String maBn) throws SQLException {
        return Jdbc.list(c, VIEW + "WHERE bn.maBenhNhan = ? ORDER BY kh.ngayLap DESC", KeHoachDao::mapView, maBn);
    }

    public void insert(Connection c, KeHoachDieuTri k) throws SQLException {
        Jdbc.update(c, "INSERT INTO KeHoachDieuTri (maKeHoach, maHoSo, maPhieuKham, ngayLap, mucTieu, trangThai, maNhanVien, lyDoHuy) VALUES (?,?,?,?,?,?,?,?)",
                k.maKeHoach(), k.maHoSo(), k.maPhieuKham(), k.ngayLap(), k.mucTieu(), k.trangThai(), k.maNhanVien(), k.lyDoHuy());
    }

    public int setTrangThai(Connection c, String ma, TrangThaiKeHoach moi, String lyDoHuy) throws SQLException {
        return Jdbc.update(c, "UPDATE KeHoachDieuTri SET trangThai = ?, lyDoHuy = ? WHERE maKeHoach = ? AND trangThai = 'DANG_THUC_HIEN'", moi, lyDoHuy, ma);
    }

    // ---------- Hạng mục ----------
    public List<ChiTietDieuTriView> items(Connection c, String maKh) throws SQLException {
        return Jdbc.list(c, CT_VIEW + "WHERE ct.maKeHoach = ? ORDER BY ct.ngayDuKien, ct.maChiTiet", KeHoachDao::mapCtView, maKh);
    }

    public Optional<ChiTietDieuTri> findItem(Connection c, int ma) throws SQLException {
        return Jdbc.one(c, "SELECT " + CT_COLS + " FROM ChiTietDieuTri ct WHERE ct.maChiTiet = ?", KeHoachDao::mapCt, ma);
    }

    public long insertItem(Connection c, ChiTietDieuTri i) throws SQLException {
        return Jdbc.insertReturningKey(c, "INSERT INTO ChiTietDieuTri (maKeHoach, maDichVu, viTriRang, ngayDuKien, ngayThucHien, ketQua, trangThai, maNhanVienThucHien, ghiChu) "
                        + "VALUES (?,?,?,?,?,?,?,?,?)",
                i.maKeHoach(), i.maDichVu(), i.viTriRang(), i.ngayDuKien(), i.ngayThucHien(), i.ketQua(), i.trangThai(), i.maNhanVienThucHien(), i.ghiChu());
    }

    public int setNgayDuKien(Connection c, int ma, LocalDate ngay) throws SQLException {
        return Jdbc.update(c, "UPDATE ChiTietDieuTri SET ngayDuKien = ? WHERE maChiTiet = ? AND trangThai = 'CHUA_THUC_HIEN'", ngay, ma);
    }

    public int cancelItem(Connection c, int ma, String lyDo) throws SQLException {
        return Jdbc.update(c, "UPDATE ChiTietDieuTri SET trangThai = 'DA_HUY', ghiChu = ? WHERE maChiTiet = ? AND trangThai = 'CHUA_THUC_HIEN'", lyDo, ma);
    }

    public int cancelPendingItems(Connection c, String maKh, String lyDo) throws SQLException {
        return Jdbc.update(c, "UPDATE ChiTietDieuTri SET trangThai = 'DA_HUY', ghiChu = ? WHERE maKeHoach = ? AND trangThai = 'CHUA_THUC_HIEN'", lyDo, maKh);
    }

    public int recordResult(Connection c, int ma, LocalDate ngay, String ketQua, String maBs) throws SQLException {
        return Jdbc.update(c, "UPDATE ChiTietDieuTri SET trangThai = 'DA_THUC_HIEN', ngayThucHien = ?, ketQua = ?, maNhanVienThucHien = ? "
                + "WHERE maChiTiet = ? AND trangThai = 'CHUA_THUC_HIEN'", ngay, ketQua, maBs, ma);
    }

    public long countItems(Connection c, String maKh, TrangThaiDieuTri tt) throws SQLException {
        return Jdbc.count(c, "SELECT COUNT(*) FROM ChiTietDieuTri WHERE maKeHoach = ? AND trangThai = ?", maKh, tt);
    }

    /** Hạng mục chưa thực hiện của kế hoạch đang thực hiện, đến hạn trước hoặc bằng ngày cho trước (gồm cả quá hạn). */
    public List<TaiKhamItem> dueItems(Connection c, LocalDate denNgay) throws SQLException {
        return Jdbc.list(c, "SELECT " + CT_COLS + ", dv.tenDichVu, dv.donGia, CAST(NULL AS VARCHAR(100)) AS tenBsTh, kh.mucTieu, bn.maBenhNhan, bn.hoTen AS tenBn, "
                        + "bn.soDienThoai, kh.maNhanVien AS maBsKh, bs.hoTen AS tenBsKh FROM ChiTietDieuTri ct "
                        + "JOIN DichVu dv ON dv.maDichVu = ct.maDichVu JOIN KeHoachDieuTri kh ON kh.maKeHoach = ct.maKeHoach "
                        + "JOIN HoSoBenhAn hs ON hs.maHoSo = kh.maHoSo JOIN BenhNhan bn ON bn.maBenhNhan = hs.maBenhNhan "
                        + "JOIN NhanVien bs ON bs.maNhanVien = kh.maNhanVien "
                        + "WHERE kh.trangThai = 'DANG_THUC_HIEN' AND ct.trangThai = 'CHUA_THUC_HIEN' AND ct.ngayDuKien <= ? ORDER BY ct.ngayDuKien, ct.maChiTiet",
                rs -> new TaiKhamItem(mapCtView(rs), rs.getString("maKeHoach"), rs.getString("mucTieu"), rs.getString("maBenhNhan"), rs.getString("tenBn"),
                        rs.getString("soDienThoai"), rs.getString("maBsKh"), rs.getString("tenBsKh"), null, null), denNgay);
    }
}
