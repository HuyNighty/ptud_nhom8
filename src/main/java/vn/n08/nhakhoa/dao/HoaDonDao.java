package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.dto.ChiTietHoaDonView;
import vn.n08.nhakhoa.dto.HoaDonView;
import vn.n08.nhakhoa.model.ChiTietHoaDon;
import vn.n08.nhakhoa.model.HoaDon;
import vn.n08.nhakhoa.model.TrangThaiHoaDon;
import vn.n08.nhakhoa.util.Jdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Bảng HoaDon + ChiTietHoaDon. Tổng tiền / đã trả tính bằng truy vấn con, không lưu. */
public class HoaDonDao {
    static final String COLS = "hd.maHoaDon, hd.maNhanVien, hd.maBenhNhan, hd.maPhieuKham, hd.ngayLap, hd.trangThai";
    static final String TONG = "(SELECT COALESCE(SUM(ct.soLuong * ct.donGiaApDung), 0) FROM ChiTietHoaDon ct WHERE ct.maHoaDon = hd.maHoaDon)";
    static final String DA_TRA = "(SELECT COALESCE(SUM(tt.soTien), 0) FROM ThanhToan tt WHERE tt.maHoaDon = hd.maHoaDon AND tt.trangThai = 'THANH_CONG')";
    private static final String VIEW = "SELECT " + COLS + ", bn.hoTen AS tenBn, bn.soDienThoai AS sdtBn, nv.hoTen AS tenNv, " + TONG + " AS tongTien, "
            + DA_TRA + " AS daTra, (SELECT COUNT(*) FROM YeuCauHuyHoaDon y WHERE y.maHoaDon = hd.maHoaDon AND y.trangThai = 'CHO_DUYET') AS soYc "
            + "FROM HoaDon hd JOIN BenhNhan bn ON bn.maBenhNhan = hd.maBenhNhan JOIN NhanVien nv ON nv.maNhanVien = hd.maNhanVien ";

    static HoaDon map(ResultSet rs) throws SQLException {
        return new HoaDon(rs.getString("maHoaDon"), rs.getString("maNhanVien"), rs.getString("maBenhNhan"), rs.getString("maPhieuKham"),
                Jdbc.dateTime(rs, "ngayLap"), TrangThaiHoaDon.of(rs.getString("trangThai")));
    }

    static HoaDonView mapView(ResultSet rs) throws SQLException {
        return new HoaDonView(map(rs), rs.getString("tenBn"), rs.getString("sdtBn"), rs.getString("tenNv"), Jdbc.money(rs, "tongTien"),
                Jdbc.money(rs, "daTra"), rs.getInt("soYc") > 0);
    }

    public Optional<HoaDon> find(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM HoaDon hd WHERE hd.maHoaDon = ?", HoaDonDao::map, ma);
    }

    /** Khóa dòng hóa đơn trong transaction (UPDATE không đổi giá trị) để các lần thu đồng thời được xử lý tuần tự. */
    public int lock(Connection c, String ma) throws SQLException {
        return Jdbc.update(c, "UPDATE HoaDon SET trangThai = trangThai WHERE maHoaDon = ?", ma);
    }

    public Optional<HoaDonView> findView(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, VIEW + "WHERE hd.maHoaDon = ?", HoaDonDao::mapView, ma);
    }

    public List<HoaDonView> search(Connection c, LocalDateTime tu, LocalDateTime den, TrangThaiHoaDon tt, String tuKhoa) throws SQLException {
        StringBuilder sql = new StringBuilder(VIEW).append("WHERE hd.ngayLap >= ? AND hd.ngayLap < ? ");
        List<Object> p = new ArrayList<>(List.of(tu, den));
        if (tt != null) { sql.append("AND hd.trangThai = ? "); p.add(tt); }
        if (tuKhoa != null && !tuKhoa.isBlank()) {
            sql.append("AND (LOWER(bn.hoTen) LIKE ? OR bn.soDienThoai LIKE ? OR LOWER(hd.maHoaDon) LIKE ? OR LOWER(bn.maBenhNhan) LIKE ?) ");
            String l = Jdbc.like(tuKhoa);
            p.add(l); p.add(l); p.add(l); p.add(l);
        }
        sql.append("ORDER BY hd.ngayLap DESC, hd.maHoaDon DESC");
        return Jdbc.list(c, sql.toString(), HoaDonDao::mapView, p.toArray());
    }

    public List<HoaDonView> byPatient(Connection c, String maBn) throws SQLException {
        return Jdbc.list(c, VIEW + "WHERE hd.maBenhNhan = ? ORDER BY hd.ngayLap DESC", HoaDonDao::mapView, maBn);
    }

    /** Hóa đơn chưa hủy còn nợ (tổng > đã trả). */
    public List<HoaDonView> withDebt(Connection c) throws SQLException {
        return Jdbc.list(c, VIEW + "WHERE hd.trangThai IN ('CHUA_THANH_TOAN','THANH_TOAN_MOT_PHAN') AND " + TONG + " > " + DA_TRA
                + " ORDER BY hd.ngayLap", HoaDonDao::mapView);
    }

    public boolean phieuKhamHasInvoice(Connection c, String maPk) throws SQLException {
        return Jdbc.count(c, "SELECT COUNT(*) FROM HoaDon WHERE maPhieuKham = ? AND trangThai <> 'DA_HUY'", maPk) > 0;
    }

    public BigDecimal tongTien(Connection c, String ma) throws SQLException {
        return Jdbc.sum(c, "SELECT SUM(soLuong * donGiaApDung) FROM ChiTietHoaDon WHERE maHoaDon = ?", ma);
    }

    public BigDecimal daThanhToan(Connection c, String ma) throws SQLException {
        return Jdbc.sum(c, "SELECT SUM(soTien) FROM ThanhToan WHERE maHoaDon = ? AND trangThai = 'THANH_CONG'", ma);
    }

    public void insert(Connection c, HoaDon h) throws SQLException {
        Jdbc.update(c, "INSERT INTO HoaDon (maHoaDon, maNhanVien, maBenhNhan, maPhieuKham, ngayLap, trangThai) VALUES (?,?,?,?,?,?)",
                h.maHoaDon(), h.maNhanVien(), h.maBenhNhan(), h.maPhieuKham(), h.ngayLap(), h.trangThai());
    }

    public int setTrangThai(Connection c, String ma, TrangThaiHoaDon tt) throws SQLException {
        return Jdbc.update(c, "UPDATE HoaDon SET trangThai = ? WHERE maHoaDon = ? AND trangThai <> 'DA_HUY'", tt, ma);
    }

    public long insertLine(Connection c, ChiTietHoaDon l) throws SQLException {
        return Jdbc.insertReturningKey(c, "INSERT INTO ChiTietHoaDon (maHoaDon, maDichVu, maThuoc, maVatTu, soLuong, donGiaApDung) VALUES (?,?,?,?,?,?)",
                l.maHoaDon(), l.maDichVu(), l.maThuoc(), l.maVatTu(), l.soLuong(), l.donGiaApDung());
    }

    public List<ChiTietHoaDonView> lines(Connection c, String ma) throws SQLException {
        return Jdbc.list(c, "SELECT ct.maChiTiet, ct.maHoaDon, ct.maDichVu, ct.maThuoc, ct.maVatTu, ct.soLuong, ct.donGiaApDung, "
                        + "dv.tenDichVu, t.tenThuoc, vt.tenVatTu FROM ChiTietHoaDon ct LEFT JOIN DichVu dv ON dv.maDichVu = ct.maDichVu "
                        + "LEFT JOIN Thuoc t ON t.maThuoc = ct.maThuoc LEFT JOIN VatTu vt ON vt.maVatTu = ct.maVatTu WHERE ct.maHoaDon = ? ORDER BY ct.maChiTiet",
                rs -> {
                    ChiTietHoaDon ct = new ChiTietHoaDon(rs.getInt("maChiTiet"), rs.getString("maHoaDon"), rs.getString("maDichVu"),
                            rs.getString("maThuoc"), rs.getString("maVatTu"), rs.getInt("soLuong"), Jdbc.money(rs, "donGiaApDung"));
                    String ten = ct.maDichVu() != null ? rs.getString("tenDichVu") : ct.maThuoc() != null ? rs.getString("tenThuoc") : rs.getString("tenVatTu");
                    String loai = ct.maDichVu() != null ? "Dịch vụ" : ct.maThuoc() != null ? "Thuốc" : "Vật tư";
                    return new ChiTietHoaDonView(ct, ten, loai);
                }, ma);
    }
}
