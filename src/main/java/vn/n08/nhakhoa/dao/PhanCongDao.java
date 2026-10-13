package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.dto.PhanCongView;
import vn.n08.nhakhoa.model.PhanCong;
import vn.n08.nhakhoa.model.TrangThaiPhanCong;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.util.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Bảng PhanCong. */
public class PhanCongDao {
    static final String COLS = "pc.maPhanCong, pc.maNhanVien, pc.maCa, pc.ngayLam, pc.nhiemVu, pc.trangThai, pc.lyDoHuy, pc.maPhanCongGoc";
    private static final String VIEW = "SELECT " + COLS + ", nv.hoTen, nv.chucDanh, ca.tenCa, ca.gioBatDau, ca.gioKetThuc FROM PhanCong pc "
            + "JOIN NhanVien nv ON nv.maNhanVien = pc.maNhanVien JOIN CaLamViec ca ON ca.maCa = pc.maCa ";

    static PhanCong map(ResultSet rs) throws SQLException {
        return new PhanCong(rs.getString("maPhanCong"), rs.getString("maNhanVien"), rs.getString("maCa"), Jdbc.date(rs, "ngayLam"),
                rs.getString("nhiemVu"), TrangThaiPhanCong.of(rs.getString("trangThai")), rs.getString("lyDoHuy"), rs.getString("maPhanCongGoc"));
    }

    static PhanCongView mapView(ResultSet rs) throws SQLException {
        return new PhanCongView(map(rs), rs.getString("hoTen"), VaiTro.of(rs.getString("chucDanh")), rs.getString("tenCa"),
                Jdbc.time(rs, "gioBatDau"), Jdbc.time(rs, "gioKetThuc"));
    }

    public Optional<PhanCong> find(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM PhanCong pc WHERE pc.maPhanCong = ?", PhanCongDao::map, ma);
    }

    public Optional<PhanCongView> findView(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, VIEW + "WHERE pc.maPhanCong = ?", PhanCongDao::mapView, ma);
    }

    /** Mọi phân công (kể cả đã hủy) trong khoảng ngày, có thể lọc theo nhân viên. */
    public List<PhanCongView> range(Connection c, LocalDate tu, LocalDate den, String maNv) throws SQLException {
        if (maNv == null) {
            return Jdbc.list(c, VIEW + "WHERE pc.ngayLam >= ? AND pc.ngayLam <= ? ORDER BY pc.ngayLam, ca.gioBatDau, nv.chucDanh, nv.hoTen",
                    PhanCongDao::mapView, tu, den);
        }
        return Jdbc.list(c, VIEW + "WHERE pc.ngayLam >= ? AND pc.ngayLam <= ? AND pc.maNhanVien = ? ORDER BY pc.ngayLam, ca.gioBatDau",
                PhanCongDao::mapView, tu, den, maNv);
    }

    /** Phân công còn hiệu lực từ ngày cho trước trở đi (dùng cho thủ tục nghỉ việc). */
    public List<PhanCongView> openFrom(Connection c, String maNv, LocalDate tu) throws SQLException {
        return Jdbc.list(c, VIEW + "WHERE pc.maNhanVien = ? AND pc.ngayLam >= ? AND pc.trangThai IN ('CHO_HIEU_LUC','DANG_THUC_HIEN') "
                + "ORDER BY pc.ngayLam, ca.gioBatDau", PhanCongDao::mapView, maNv, tu);
    }

    /** Chuỗi lịch sử thay người: các phân công trỏ tới bản gốc này. */
    public List<PhanCongView> replacements(Connection c, String maGoc) throws SQLException {
        return Jdbc.list(c, VIEW + "WHERE pc.maPhanCongGoc = ? ORDER BY pc.maPhanCong", PhanCongDao::mapView, maGoc);
    }

    public long countActive(Connection c, String maNv, LocalDate ngay) throws SQLException {
        return Jdbc.count(c, "SELECT COUNT(*) FROM PhanCong WHERE maNhanVien = ? AND ngayLam = ? AND trangThai <> 'DA_HUY'", maNv, ngay);
    }

    public boolean existsActive(Connection c, String maNv, LocalDate ngay, String maCa) throws SQLException {
        return Jdbc.count(c, "SELECT COUNT(*) FROM PhanCong WHERE maNhanVien = ? AND ngayLam = ? AND maCa = ? AND trangThai <> 'DA_HUY'",
                maNv, ngay, maCa) > 0;
    }

    public void insert(Connection c, PhanCong p) throws SQLException {
        Jdbc.update(c, "INSERT INTO PhanCong (maPhanCong, maNhanVien, maCa, ngayLam, nhiemVu, trangThai, lyDoHuy, maPhanCongGoc) VALUES (?,?,?,?,?,?,?,?)",
                p.maPhanCong(), p.maNhanVien(), p.maCa(), p.ngayLam(), p.nhiemVu(), p.trangThai(), p.lyDoHuy(), p.maPhanCongGoc());
    }

    public int setTrangThai(Connection c, String ma, TrangThaiPhanCong cu, TrangThaiPhanCong moi) throws SQLException {
        return Jdbc.update(c, "UPDATE PhanCong SET trangThai = ? WHERE maPhanCong = ? AND trangThai = ?", moi, ma, cu);
    }

    public int cancel(Connection c, String ma, String lyDo) throws SQLException {
        return Jdbc.update(c, "UPDATE PhanCong SET trangThai = 'DA_HUY', lyDoHuy = ? WHERE maPhanCong = ? AND trangThai IN ('CHO_HIEU_LUC','DANG_THUC_HIEN')",
                lyDo, ma);
    }

    public int setNhiemVu(Connection c, String ma, String nhiemVu) throws SQLException {
        return Jdbc.update(c, "UPDATE PhanCong SET nhiemVu = ? WHERE maPhanCong = ? AND trangThai IN ('CHO_HIEU_LUC','DANG_THUC_HIEN')", nhiemVu, ma);
    }
}
