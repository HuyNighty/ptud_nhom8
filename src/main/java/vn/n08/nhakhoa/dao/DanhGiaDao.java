package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.dto.DanhGiaView;
import vn.n08.nhakhoa.model.ChiTietDanhGia;
import vn.n08.nhakhoa.model.DanhGiaNhanSu;
import vn.n08.nhakhoa.model.TrangThaiDanhGia;
import vn.n08.nhakhoa.util.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Bảng DanhGiaNhanSu + ChiTietDanhGia. */
public class DanhGiaDao {
    static final String COLS = "dg.maDanhGia, dg.maNhanVien, dg.maNguoiDanhGia, dg.kyDanhGia, dg.ngayDanhGia, dg.diem, dg.nhanXet, dg.trangThai";

    static DanhGiaNhanSu map(ResultSet rs) throws SQLException {
        return new DanhGiaNhanSu(rs.getString("maDanhGia"), rs.getString("maNhanVien"), rs.getString("maNguoiDanhGia"), rs.getString("kyDanhGia"),
                Jdbc.date(rs, "ngayDanhGia"), rs.getBigDecimal("diem"), rs.getString("nhanXet"), TrangThaiDanhGia.of(rs.getString("trangThai")));
    }

    /** Toàn bộ nhân viên (trừ quản trị) kèm đánh giá của kỳ nếu có. Nhân viên đã nghỉ chỉ hiện khi đã có đánh giá. */
    public List<DanhGiaView> byKy(Connection c, String ky) throws SQLException {
        return Jdbc.list(c, "SELECT " + NhanVienDao.COLS + ", dg.maDanhGia AS dgMa, dg.maNguoiDanhGia AS dgNguoi, dg.kyDanhGia AS dgKy, "
                        + "dg.ngayDanhGia AS dgNgay, dg.diem AS dgDiem, dg.nhanXet AS dgNx, dg.trangThai AS dgTt, ng.hoTen AS tenNguoi "
                        + "FROM NhanVien nv LEFT JOIN DanhGiaNhanSu dg ON dg.maNhanVien = nv.maNhanVien AND dg.kyDanhGia = ? "
                        + "LEFT JOIN NhanVien ng ON ng.maNhanVien = dg.maNguoiDanhGia "
                        + "WHERE nv.chucDanh <> 'AD' AND (nv.trangThai = 'DANG_LAM_VIEC' OR dg.maDanhGia IS NOT NULL) ORDER BY nv.chucDanh, nv.hoTen",
                rs -> {
                    DanhGiaNhanSu dg = rs.getString("dgMa") == null ? null : new DanhGiaNhanSu(rs.getString("dgMa"), rs.getString("maNhanVien"),
                            rs.getString("dgNguoi"), rs.getString("dgKy"), Jdbc.date(rs, "dgNgay"), rs.getBigDecimal("dgDiem"), rs.getString("dgNx"),
                            TrangThaiDanhGia.of(rs.getString("dgTt")));
                    return new DanhGiaView(NhanVienDao.map(rs), dg, rs.getString("tenNguoi"));
                }, ky);
    }

    public Optional<DanhGiaNhanSu> find(Connection c, String maNv, String ky) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM DanhGiaNhanSu dg WHERE dg.maNhanVien = ? AND dg.kyDanhGia = ?", DanhGiaDao::map, maNv, ky);
    }

    public List<ChiTietDanhGia> details(Connection c, String maDg) throws SQLException {
        return Jdbc.list(c, "SELECT maDanhGia, thuTu, tieuChi, diem FROM ChiTietDanhGia WHERE maDanhGia = ? ORDER BY thuTu",
                rs -> new ChiTietDanhGia(rs.getString(1), rs.getInt(2), rs.getString(3), rs.getInt(4)), maDg);
    }

    public void insert(Connection c, DanhGiaNhanSu d) throws SQLException {
        Jdbc.update(c, "INSERT INTO DanhGiaNhanSu (maDanhGia, maNhanVien, maNguoiDanhGia, kyDanhGia, ngayDanhGia, diem, nhanXet, trangThai) VALUES (?,?,?,?,?,?,?,?)",
                d.maDanhGia(), d.maNhanVien(), d.maNguoiDanhGia(), d.kyDanhGia(), d.ngayDanhGia(), d.diem(), d.nhanXet(), d.trangThai());
    }

    /** Chỉ cập nhật được bản nháp. */
    public int updateDraft(Connection c, DanhGiaNhanSu d) throws SQLException {
        return Jdbc.update(c, "UPDATE DanhGiaNhanSu SET maNguoiDanhGia = ?, ngayDanhGia = ?, diem = ?, nhanXet = ?, trangThai = ? "
                        + "WHERE maDanhGia = ? AND trangThai = 'NHAP'",
                d.maNguoiDanhGia(), d.ngayDanhGia(), d.diem(), d.nhanXet(), d.trangThai(), d.maDanhGia());
    }

    public void replaceDetails(Connection c, String maDg, List<ChiTietDanhGia> items) throws SQLException {
        Jdbc.update(c, "DELETE FROM ChiTietDanhGia WHERE maDanhGia = ?", maDg);
        for (ChiTietDanhGia i : items) {
            Jdbc.update(c, "INSERT INTO ChiTietDanhGia (maDanhGia, thuTu, tieuChi, diem) VALUES (?,?,?,?)", maDg, i.thuTu(), i.tieuChi(), i.diem());
        }
    }
}
