package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.dto.ChiDinhView;
import vn.n08.nhakhoa.dto.PhieuKhamView;
import vn.n08.nhakhoa.model.ChiDinhCanLamSang;
import vn.n08.nhakhoa.model.PhieuKham;
import vn.n08.nhakhoa.model.TrangThaiCLS;
import vn.n08.nhakhoa.model.TrangThaiPhieuKham;
import vn.n08.nhakhoa.util.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Bảng PhieuKham + ChiDinhCanLamSang. */
public class PhieuKhamDao {
    static final String COLS = "pk.maPhieuKham, pk.maBenhNhan, pk.maHoSo, pk.maNhanVien, pk.maLichHen, pk.thoiGianTiepNhan, pk.trieuChung, "
            + "pk.chanDoan, pk.ketLuan, pk.trangThai, pk.lyDoHuy";
    private static final String VIEW = "SELECT " + COLS + ", bn.hoTen AS tenBn, bn.soDienThoai AS sdtBn, bn.ngaySinh AS nsBn, bs.hoTen AS tenBs, "
            + "hs.tienSuBenh, hs.diUng, (SELECT COUNT(*) FROM ChiDinhCanLamSang cd WHERE cd.maPhieuKham = pk.maPhieuKham AND cd.trangThai = 'CHO_THUC_HIEN') AS soCls "
            + "FROM PhieuKham pk JOIN BenhNhan bn ON bn.maBenhNhan = pk.maBenhNhan JOIN NhanVien bs ON bs.maNhanVien = pk.maNhanVien "
            + "JOIN HoSoBenhAn hs ON hs.maHoSo = pk.maHoSo ";

    static PhieuKham map(ResultSet rs) throws SQLException {
        return new PhieuKham(rs.getString("maPhieuKham"), rs.getString("maBenhNhan"), rs.getString("maHoSo"), rs.getString("maNhanVien"),
                rs.getString("maLichHen"), Jdbc.dateTime(rs, "thoiGianTiepNhan"), rs.getString("trieuChung"), rs.getString("chanDoan"),
                rs.getString("ketLuan"), TrangThaiPhieuKham.of(rs.getString("trangThai")), rs.getString("lyDoHuy"));
    }

    static PhieuKhamView mapView(ResultSet rs) throws SQLException {
        return new PhieuKhamView(map(rs), rs.getString("tenBn"), rs.getString("sdtBn"), Jdbc.date(rs, "nsBn"), rs.getString("tenBs"),
                rs.getString("tienSuBenh"), rs.getString("diUng"), rs.getInt("soCls"));
    }

    public Optional<PhieuKham> find(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM PhieuKham pk WHERE pk.maPhieuKham = ?", PhieuKhamDao::map, ma);
    }

    public Optional<PhieuKhamView> findView(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, VIEW + "WHERE pk.maPhieuKham = ?", PhieuKhamDao::mapView, ma);
    }

    public List<PhieuKhamView> search(Connection c, LocalDateTime tu, LocalDateTime den, String maBs, TrangThaiPhieuKham tt, String tuKhoa) throws SQLException {
        StringBuilder sql = new StringBuilder(VIEW).append("WHERE pk.thoiGianTiepNhan >= ? AND pk.thoiGianTiepNhan < ? ");
        List<Object> p = new ArrayList<>(List.of(tu, den));
        if (maBs != null) { sql.append("AND pk.maNhanVien = ? "); p.add(maBs); }
        if (tt != null) { sql.append("AND pk.trangThai = ? "); p.add(tt); }
        if (tuKhoa != null && !tuKhoa.isBlank()) {
            sql.append("AND (LOWER(bn.hoTen) LIKE ? OR bn.soDienThoai LIKE ? OR LOWER(pk.maPhieuKham) LIKE ? OR LOWER(bn.maBenhNhan) LIKE ?) ");
            String l = Jdbc.like(tuKhoa);
            p.add(l); p.add(l); p.add(l); p.add(l);
        }
        sql.append("ORDER BY pk.thoiGianTiepNhan DESC");
        return Jdbc.list(c, sql.toString(), PhieuKhamDao::mapView, p.toArray());
    }

    public List<PhieuKhamView> byPatient(Connection c, String maBn) throws SQLException {
        return Jdbc.list(c, VIEW + "WHERE pk.maBenhNhan = ? ORDER BY pk.thoiGianTiepNhan DESC", PhieuKhamDao::mapView, maBn);
    }

    /** Phiếu khám chưa kết thúc (chờ khám / đang khám) của bác sĩ — điều kiện thủ tục nghỉ việc. */
    public List<PhieuKhamView> openOfDoctor(Connection c, String maBs) throws SQLException {
        return Jdbc.list(c, VIEW + "WHERE pk.maNhanVien = ? AND pk.trangThai IN ('CHO_KHAM','DANG_KHAM') ORDER BY pk.thoiGianTiepNhan", PhieuKhamDao::mapView, maBs);
    }

    public boolean patientHasOpen(Connection c, String maBn) throws SQLException {
        return Jdbc.count(c, "SELECT COUNT(*) FROM PhieuKham WHERE maBenhNhan = ? AND trangThai IN ('CHO_KHAM','DANG_KHAM')", maBn) > 0;
    }

    /** Phiếu đã kết luận nhưng chưa có hóa đơn còn hiệu lực — gợi ý lập hóa đơn. */
    public List<PhieuKhamView> concludedWithoutInvoice(Connection c) throws SQLException {
        return Jdbc.list(c, VIEW + "WHERE pk.trangThai = 'DA_KET_LUAN' AND NOT EXISTS (SELECT 1 FROM HoaDon h WHERE h.maPhieuKham = pk.maPhieuKham "
                + "AND h.trangThai <> 'DA_HUY') ORDER BY pk.thoiGianTiepNhan DESC", PhieuKhamDao::mapView);
    }

    public void insert(Connection c, PhieuKham p) throws SQLException {
        Jdbc.update(c, "INSERT INTO PhieuKham (maPhieuKham, maBenhNhan, maHoSo, maNhanVien, maLichHen, thoiGianTiepNhan, trieuChung, chanDoan, ketLuan, trangThai, lyDoHuy) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                p.maPhieuKham(), p.maBenhNhan(), p.maHoSo(), p.maNhanVien(), p.maLichHen(), p.thoiGianTiepNhan(), p.trieuChung(), p.chanDoan(),
                p.ketLuan(), p.trangThai(), p.lyDoHuy());
    }

    public int startExam(Connection c, String ma, String maBs) throws SQLException {
        return Jdbc.update(c, "UPDATE PhieuKham SET trangThai = 'DANG_KHAM', maNhanVien = ? WHERE maPhieuKham = ? AND trangThai = 'CHO_KHAM'", maBs, ma);
    }

    public int saveResult(Connection c, String ma, String trieuChung, String chanDoan, String ketLuan) throws SQLException {
        return Jdbc.update(c, "UPDATE PhieuKham SET trieuChung = ?, chanDoan = ?, ketLuan = ? WHERE maPhieuKham = ? AND trangThai = 'DANG_KHAM'",
                trieuChung, chanDoan, ketLuan, ma);
    }

    public int conclude(Connection c, String ma) throws SQLException {
        return Jdbc.update(c, "UPDATE PhieuKham SET trangThai = 'DA_KET_LUAN' WHERE maPhieuKham = ? AND trangThai = 'DANG_KHAM' AND chanDoan IS NOT NULL", ma);
    }

    public int cancel(Connection c, String ma, String lyDo) throws SQLException {
        return Jdbc.update(c, "UPDATE PhieuKham SET trangThai = 'DA_HUY', lyDoHuy = ? WHERE maPhieuKham = ? AND trangThai = 'CHO_KHAM'", lyDo, ma);
    }

    public int changeDoctor(Connection c, String ma, String maBs) throws SQLException {
        return Jdbc.update(c, "UPDATE PhieuKham SET maNhanVien = ? WHERE maPhieuKham = ? AND trangThai = 'CHO_KHAM'", maBs, ma);
    }

    public long countToday(Connection c, LocalDateTime tu, LocalDateTime den, TrangThaiPhieuKham tt) throws SQLException {
        return Jdbc.count(c, "SELECT COUNT(*) FROM PhieuKham WHERE thoiGianTiepNhan >= ? AND thoiGianTiepNhan < ? AND trangThai = ?", tu, den, tt);
    }

    // ---------- Chỉ định cận lâm sàng ----------
    static final String CD_COLS = "cd.maChiDinh, cd.maPhieuKham, cd.maDichVu, cd.yeuCau, cd.ketQua, cd.maNhanVien, cd.thoiGianChiDinh, cd.thoiGianKetQua, cd.trangThai";

    static ChiDinhCanLamSang mapCd(ResultSet rs) throws SQLException {
        return new ChiDinhCanLamSang(rs.getString("maChiDinh"), rs.getString("maPhieuKham"), rs.getString("maDichVu"), rs.getString("yeuCau"),
                rs.getString("ketQua"), rs.getString("maNhanVien"), Jdbc.dateTime(rs, "thoiGianChiDinh"), Jdbc.dateTime(rs, "thoiGianKetQua"),
                TrangThaiCLS.of(rs.getString("trangThai")));
    }

    public List<ChiDinhView> chiDinhOf(Connection c, String maPk) throws SQLException {
        return Jdbc.list(c, "SELECT " + CD_COLS + ", dv.tenDichVu, dv.donGia, bs.hoTen AS tenBs FROM ChiDinhCanLamSang cd "
                        + "JOIN DichVu dv ON dv.maDichVu = cd.maDichVu JOIN NhanVien bs ON bs.maNhanVien = cd.maNhanVien "
                        + "WHERE cd.maPhieuKham = ? ORDER BY cd.thoiGianChiDinh",
                rs -> new ChiDinhView(mapCd(rs), rs.getString("tenDichVu"), Jdbc.money(rs, "donGia"), rs.getString("tenBs")), maPk);
    }

    public Optional<ChiDinhCanLamSang> findChiDinh(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, "SELECT " + CD_COLS + " FROM ChiDinhCanLamSang cd WHERE cd.maChiDinh = ?", PhieuKhamDao::mapCd, ma);
    }

    public void insertChiDinh(Connection c, ChiDinhCanLamSang d) throws SQLException {
        Jdbc.update(c, "INSERT INTO ChiDinhCanLamSang (maChiDinh, maPhieuKham, maDichVu, yeuCau, ketQua, maNhanVien, thoiGianChiDinh, thoiGianKetQua, trangThai) "
                        + "VALUES (?,?,?,?,?,?,?,?,?)",
                d.maChiDinh(), d.maPhieuKham(), d.maDichVu(), d.yeuCau(), d.ketQua(), d.maNhanVien(), d.thoiGianChiDinh(), d.thoiGianKetQua(), d.trangThai());
    }

    public int setKetQuaChiDinh(Connection c, String ma, String ketQua, LocalDateTime t) throws SQLException {
        return Jdbc.update(c, "UPDATE ChiDinhCanLamSang SET ketQua = ?, thoiGianKetQua = ?, trangThai = 'DA_CO_KET_QUA' WHERE maChiDinh = ? AND trangThai = 'CHO_THUC_HIEN'",
                ketQua, t, ma);
    }

    public long countChiDinhCho(Connection c, String maPk) throws SQLException {
        return Jdbc.count(c, "SELECT COUNT(*) FROM ChiDinhCanLamSang WHERE maPhieuKham = ? AND trangThai = 'CHO_THUC_HIEN'", maPk);
    }
}
