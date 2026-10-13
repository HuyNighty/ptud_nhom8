package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.dto.LichHenView;
import vn.n08.nhakhoa.model.LichHen;
import vn.n08.nhakhoa.model.TrangThaiLichHen;
import vn.n08.nhakhoa.util.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Bảng LichHen. "Đang mở" = CHO_XAC_NHAN, DA_XAC_NHAN, DA_DEN (khớp chỉ mục UX_LichHen_KhungGio). */
public class LichHenDao {
    static final String COLS = "lh.maLichHen, lh.maBenhNhan, lh.maNhanVien, lh.thoiGianHen, lh.lyDoHen, lh.trangThai, lh.maLichHenGoc, lh.maKeHoach, "
            + "lh.lyDoHuy, lh.maTaiKhoanTao, lh.ngayTao";
    static final String OPEN = "('CHO_XAC_NHAN','DA_XAC_NHAN','DA_DEN')";
    private static final String VIEW = "SELECT " + COLS + ", bn.hoTen AS tenBn, bn.soDienThoai AS sdtBn, bs.hoTen AS tenBs FROM LichHen lh "
            + "JOIN BenhNhan bn ON bn.maBenhNhan = lh.maBenhNhan JOIN NhanVien bs ON bs.maNhanVien = lh.maNhanVien ";

    static LichHen map(ResultSet rs) throws SQLException {
        return new LichHen(rs.getString("maLichHen"), rs.getString("maBenhNhan"), rs.getString("maNhanVien"), Jdbc.dateTime(rs, "thoiGianHen"),
                rs.getString("lyDoHen"), TrangThaiLichHen.of(rs.getString("trangThai")), rs.getString("maLichHenGoc"), rs.getString("maKeHoach"),
                rs.getString("lyDoHuy"), rs.getString("maTaiKhoanTao"), Jdbc.dateTime(rs, "ngayTao"));
    }

    static LichHenView mapView(ResultSet rs) throws SQLException {
        return new LichHenView(map(rs), rs.getString("tenBn"), rs.getString("sdtBn"), rs.getString("tenBs"));
    }

    public Optional<LichHen> find(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM LichHen lh WHERE lh.maLichHen = ?", LichHenDao::map, ma);
    }

    public Optional<LichHenView> findView(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, VIEW + "WHERE lh.maLichHen = ?", LichHenDao::mapView, ma);
    }

    public List<LichHenView> search(Connection c, LocalDateTime tu, LocalDateTime den, String maBs, TrangThaiLichHen tt, String tuKhoa) throws SQLException {
        StringBuilder sql = new StringBuilder(VIEW).append("WHERE lh.thoiGianHen >= ? AND lh.thoiGianHen < ? ");
        List<Object> p = new ArrayList<>(List.of(tu, den));
        if (maBs != null) { sql.append("AND lh.maNhanVien = ? "); p.add(maBs); }
        if (tt != null) { sql.append("AND lh.trangThai = ? "); p.add(tt); }
        if (tuKhoa != null && !tuKhoa.isBlank()) {
            sql.append("AND (LOWER(bn.hoTen) LIKE ? OR bn.soDienThoai LIKE ? OR LOWER(lh.maLichHen) LIKE ? OR LOWER(bn.maBenhNhan) LIKE ?) ");
            String l = Jdbc.like(tuKhoa);
            p.add(l); p.add(l); p.add(l); p.add(l);
        }
        sql.append("ORDER BY lh.thoiGianHen, lh.maLichHen");
        return Jdbc.list(c, sql.toString(), LichHenDao::mapView, p.toArray());
    }

    public List<LichHenView> byPatient(Connection c, String maBn) throws SQLException {
        return Jdbc.list(c, VIEW + "WHERE lh.maBenhNhan = ? ORDER BY lh.thoiGianHen DESC", LichHenDao::mapView, maBn);
    }

    /** Lịch hẹn chưa diễn ra của bác sĩ (chờ xác nhận / đã xác nhận) — điều kiện thủ tục nghỉ việc. */
    public List<LichHenView> pendingOfDoctor(Connection c, String maBs) throws SQLException {
        return Jdbc.list(c, VIEW + "WHERE lh.maNhanVien = ? AND lh.trangThai IN ('CHO_XAC_NHAN','DA_XAC_NHAN') ORDER BY lh.thoiGianHen",
                LichHenDao::mapView, maBs);
    }

    /** Các lịch đang mở của bác sĩ trong khoảng thời gian (để tính khung giờ trống). */
    public List<LichHen> openOfDoctor(Connection c, String maBs, LocalDateTime tu, LocalDateTime den) throws SQLException {
        return Jdbc.list(c, "SELECT " + COLS + " FROM LichHen lh WHERE lh.maNhanVien = ? AND lh.thoiGianHen >= ? AND lh.thoiGianHen < ? AND lh.trangThai IN " + OPEN,
                LichHenDao::map, maBs, tu, den);
    }

    public boolean doctorBusy(Connection c, String maBs, LocalDateTime t, String exceptMa) throws SQLException {
        return Jdbc.count(c, "SELECT COUNT(*) FROM LichHen WHERE maNhanVien = ? AND thoiGianHen = ? AND maLichHen <> ? AND trangThai IN " + OPEN,
                maBs, t, exceptMa == null ? "" : exceptMa) > 0;
    }

    public boolean patientBusy(Connection c, String maBn, LocalDateTime t, String exceptMa) throws SQLException {
        return Jdbc.count(c, "SELECT COUNT(*) FROM LichHen WHERE maBenhNhan = ? AND thoiGianHen = ? AND maLichHen <> ? AND trangThai IN " + OPEN,
                maBn, t, exceptMa == null ? "" : exceptMa) > 0;
    }

    /** Lịch hẹn đang mở gắn với kế hoạch điều trị (dùng cho danh sách tái khám). */
    public Optional<LichHen> openOfPlan(Connection c, String maKeHoach) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM LichHen lh WHERE lh.maKeHoach = ? AND lh.trangThai IN ('CHO_XAC_NHAN','DA_XAC_NHAN') "
                + "ORDER BY lh.thoiGianHen OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY", LichHenDao::map, maKeHoach);
    }

    public List<LichHenView> chainFrom(Connection c, String maGoc) throws SQLException {
        return Jdbc.list(c, VIEW + "WHERE lh.maLichHenGoc = ?", LichHenDao::mapView, maGoc);
    }

    public void insert(Connection c, LichHen l) throws SQLException {
        Jdbc.update(c, "INSERT INTO LichHen (maLichHen, maBenhNhan, maNhanVien, thoiGianHen, lyDoHen, trangThai, maLichHenGoc, maKeHoach, lyDoHuy, maTaiKhoanTao, ngayTao) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                l.maLichHen(), l.maBenhNhan(), l.maNhanVien(), l.thoiGianHen(), l.lyDoHen(), l.trangThai(), l.maLichHenGoc(), l.maKeHoach(),
                l.lyDoHuy(), l.maTaiKhoanTao(), l.ngayTao());
    }

    /** Chuyển trạng thái nếu trạng thái hiện tại thuộc danh sách cho phép. Trả số dòng đổi. */
    public int transition(Connection c, String ma, TrangThaiLichHen moi, String lyDoHuy, TrangThaiLichHen... tu) throws SQLException {
        StringBuilder in = new StringBuilder();
        List<Object> p = new ArrayList<>(List.of(moi));
        String sql;
        for (int i = 0; i < tu.length; i++) in.append(i == 0 ? "?" : ",?");
        if (lyDoHuy != null) {
            sql = "UPDATE LichHen SET trangThai = ?, lyDoHuy = ? WHERE maLichHen = ? AND trangThai IN (" + in + ")";
            p.add(lyDoHuy);
        } else {
            sql = "UPDATE LichHen SET trangThai = ? WHERE maLichHen = ? AND trangThai IN (" + in + ")";
        }
        p.add(ma);
        p.addAll(List.of((Object[]) tu));
        return Jdbc.update(c, sql, p.toArray());
    }

    public long countOn(Connection c, LocalDateTime tu, LocalDateTime den, TrangThaiLichHen tt) throws SQLException {
        return Jdbc.count(c, "SELECT COUNT(*) FROM LichHen WHERE thoiGianHen >= ? AND thoiGianHen < ? AND trangThai = ?", tu, den, tt);
    }
}
