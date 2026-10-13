package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.dto.BenhNhanView;
import vn.n08.nhakhoa.model.BenhNhan;
import vn.n08.nhakhoa.model.GioiTinh;
import vn.n08.nhakhoa.model.HoSoBenhAn;
import vn.n08.nhakhoa.util.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Bảng BenhNhan + HoSoBenhAn. Không có lệnh xóa: hồ sơ bệnh nhân không bị xóa. */
public class BenhNhanDao {
    static final String COLS = "bn.maBenhNhan, bn.hoTen, bn.ngaySinh, bn.gioiTinh, bn.soDienThoai, bn.diaChi, bn.nguoiLienHe, bn.ngayTao, bn.maTaiKhoanTao";
    /** Công nợ = tổng chi tiết − tổng thanh toán thành công, trên các hóa đơn chưa hủy. */
    static final String CONG_NO_BN = "(SELECT COALESCE(SUM(ct.soLuong * ct.donGiaApDung), 0) FROM ChiTietHoaDon ct JOIN HoaDon h ON h.maHoaDon = ct.maHoaDon "
            + "WHERE h.maBenhNhan = bn.maBenhNhan AND h.trangThai <> 'DA_HUY') - "
            + "(SELECT COALESCE(SUM(tt.soTien), 0) FROM ThanhToan tt JOIN HoaDon h2 ON h2.maHoaDon = tt.maHoaDon "
            + "WHERE h2.maBenhNhan = bn.maBenhNhan AND h2.trangThai <> 'DA_HUY' AND tt.trangThai = 'THANH_CONG')";
    private static final String VIEW = "SELECT " + COLS + ", hs.maHoSo, "
            + "(SELECT MAX(pk.thoiGianTiepNhan) FROM PhieuKham pk WHERE pk.maBenhNhan = bn.maBenhNhan AND pk.trangThai <> 'DA_HUY') AS lanKhamCuoi, "
            + CONG_NO_BN + " AS congNo FROM BenhNhan bn LEFT JOIN HoSoBenhAn hs ON hs.maBenhNhan = bn.maBenhNhan ";

    static BenhNhan map(ResultSet rs) throws SQLException {
        return new BenhNhan(rs.getString("maBenhNhan"), rs.getString("hoTen"), Jdbc.date(rs, "ngaySinh"), GioiTinh.of(rs.getString("gioiTinh")),
                rs.getString("soDienThoai"), rs.getString("diaChi"), rs.getString("nguoiLienHe"), Jdbc.dateTime(rs, "ngayTao"), rs.getString("maTaiKhoanTao"));
    }

    static BenhNhanView mapView(ResultSet rs) throws SQLException {
        return new BenhNhanView(map(rs), rs.getString("maHoSo"), Jdbc.dateTime(rs, "lanKhamCuoi"), Jdbc.money(rs, "congNo"));
    }

    public Optional<BenhNhan> find(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM BenhNhan bn WHERE bn.maBenhNhan = ?", BenhNhanDao::map, ma);
    }

    public Optional<BenhNhanView> findView(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, VIEW + "WHERE bn.maBenhNhan = ?", BenhNhanDao::mapView, ma);
    }

    /** Tìm theo SĐT (chuỗi số), mã hoặc họ tên. Trả tối đa max dòng, mới nhất trước. */
    public List<BenhNhanView> search(Connection c, String q, int max) throws SQLException {
        String tail = "ORDER BY bn.ngayTao DESC, bn.maBenhNhan DESC OFFSET 0 ROWS FETCH NEXT " + max + " ROWS ONLY";
        if (q == null || q.isBlank()) return Jdbc.list(c, VIEW + tail, BenhNhanDao::mapView);
        String s = q.trim();
        if (s.replaceAll("[\\s.\\-]", "").matches("\\d+")) {
            String digits = s.replaceAll("[\\s.\\-]", "");
            return Jdbc.list(c, VIEW + "WHERE bn.soDienThoai LIKE ? OR LOWER(bn.maBenhNhan) LIKE ? " + tail, BenhNhanDao::mapView,
                    "%" + digits + "%", "%" + digits + "%");
        }
        String l = Jdbc.like(s);
        return Jdbc.list(c, VIEW + "WHERE LOWER(bn.hoTen) LIKE ? OR LOWER(bn.maBenhNhan) LIKE ? OR LOWER(hs.maHoSo) LIKE ? " + tail,
                BenhNhanDao::mapView, l, l, l);
    }

    /** Hồ sơ nghi trùng: cùng SĐT, hoặc cùng họ tên (không phân biệt hoa thường) và ngày sinh. */
    public List<BenhNhan> duplicates(Connection c, String hoTen, LocalDate ngaySinh, String sdt, String exceptMa) throws SQLException {
        return Jdbc.list(c, "SELECT " + COLS + " FROM BenhNhan bn WHERE bn.maBenhNhan <> ? AND (bn.soDienThoai = ? OR (LOWER(bn.hoTen) = ? AND bn.ngaySinh = ?)) "
                        + "ORDER BY bn.maBenhNhan",
                BenhNhanDao::map, exceptMa == null ? "" : exceptMa, sdt, hoTen.trim().toLowerCase(), ngaySinh);
    }

    public void insert(Connection c, BenhNhan b) throws SQLException {
        Jdbc.update(c, "INSERT INTO BenhNhan (maBenhNhan, hoTen, ngaySinh, gioiTinh, soDienThoai, diaChi, nguoiLienHe, ngayTao, maTaiKhoanTao) VALUES (?,?,?,?,?,?,?,?,?)",
                b.maBenhNhan(), b.hoTen(), b.ngaySinh(), b.gioiTinh(), b.soDienThoai(), b.diaChi(), b.nguoiLienHe(), b.ngayTao(), b.maTaiKhoanTao());
    }

    public int update(Connection c, BenhNhan b) throws SQLException {
        return Jdbc.update(c, "UPDATE BenhNhan SET hoTen = ?, ngaySinh = ?, gioiTinh = ?, soDienThoai = ?, diaChi = ?, nguoiLienHe = ? WHERE maBenhNhan = ?",
                b.hoTen(), b.ngaySinh(), b.gioiTinh(), b.soDienThoai(), b.diaChi(), b.nguoiLienHe(), b.maBenhNhan());
    }

    // ---------- HoSoBenhAn ----------
    static HoSoBenhAn mapHoSo(ResultSet rs) throws SQLException {
        return new HoSoBenhAn(rs.getString("maHoSo"), rs.getString("maBenhNhan"), Jdbc.date(rs, "ngayLap"), rs.getString("tienSuBenh"), rs.getString("diUng"));
    }

    public Optional<HoSoBenhAn> hoSoOf(Connection c, String maBn) throws SQLException {
        return Jdbc.one(c, "SELECT maHoSo, maBenhNhan, ngayLap, tienSuBenh, diUng FROM HoSoBenhAn WHERE maBenhNhan = ?", BenhNhanDao::mapHoSo, maBn);
    }

    public void insertHoSo(Connection c, HoSoBenhAn h) throws SQLException {
        Jdbc.update(c, "INSERT INTO HoSoBenhAn (maHoSo, maBenhNhan, ngayLap, tienSuBenh, diUng) VALUES (?,?,?,?,?)",
                h.maHoSo(), h.maBenhNhan(), h.ngayLap(), h.tienSuBenh(), h.diUng());
    }

    public int updateTienSu(Connection c, String maHoSo, String tienSu, String diUng) throws SQLException {
        return Jdbc.update(c, "UPDATE HoSoBenhAn SET tienSuBenh = ?, diUng = ? WHERE maHoSo = ?", tienSu, diUng, maHoSo);
    }
}
