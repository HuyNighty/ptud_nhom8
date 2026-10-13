package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.dto.PhieuKhoDong;
import vn.n08.nhakhoa.dto.PhieuKhoView;
import vn.n08.nhakhoa.dto.SoKsdbRow;
import vn.n08.nhakhoa.model.LoaiNhap;
import vn.n08.nhakhoa.model.PhieuNhapKho;
import vn.n08.nhakhoa.model.PhieuXuatKho;
import vn.n08.nhakhoa.util.Jdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Bảng PhieuNhapKho, ChiTietPhieuNhap, PhieuXuatKho, ChiTietPhieuXuat (phiếu hoàn tất, không sửa/xóa). */
public class PhieuKhoDao {
    public void insertNhap(Connection c, PhieuNhapKho p) throws SQLException {
        Jdbc.update(c, "INSERT INTO PhieuNhapKho (maPhieuNhap, ngayNhap, trangThai, loaiNhap, maNhaCungCap, maPhieuXuatGoc, maNhanVien, ghiChu) VALUES (?,?,?,?,?,?,?,?)",
                p.maPhieuNhap(), p.ngayNhap(), p.trangThai(), p.loaiNhap(), p.maNhaCungCap(), p.maPhieuXuatGoc(), p.maNhanVien(), p.ghiChu());
    }

    public void insertNhapLine(Connection c, String maPhieu, String maLo, int soLuong, BigDecimal donGia) throws SQLException {
        Jdbc.update(c, "INSERT INTO ChiTietPhieuNhap (maPhieuNhap, maLo, soLuong, donGiaNhap) VALUES (?,?,?,?)", maPhieu, maLo, soLuong, donGia);
    }

    public void insertXuat(Connection c, PhieuXuatKho p) throws SQLException {
        Jdbc.update(c, "INSERT INTO PhieuXuatKho (maPhieuXuat, ngayXuat, boPhanNhan, nguoiNhan, lyDo, trangThai, maNhanVien) VALUES (?,?,?,?,?,?,?)",
                p.maPhieuXuat(), p.ngayXuat(), p.boPhanNhan(), p.nguoiNhan(), p.lyDo(), p.trangThai(), p.maNhanVien());
    }

    public void insertXuatLine(Connection c, String maPhieu, String maLo, int soLuong) throws SQLException {
        Jdbc.update(c, "INSERT INTO ChiTietPhieuXuat (maPhieuXuat, maLo, soLuong) VALUES (?,?,?)", maPhieu, maLo, soLuong);
    }

    public Optional<PhieuXuatKho> findXuat(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, "SELECT maPhieuXuat, ngayXuat, boPhanNhan, nguoiNhan, lyDo, trangThai, maNhanVien FROM PhieuXuatKho WHERE maPhieuXuat = ?",
                rs -> new PhieuXuatKho(rs.getString(1), Jdbc.dateTime(rs, "ngayXuat"), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6),
                        rs.getString(7)), ma);
    }

    public Optional<PhieuNhapKho> findNhap(Connection c, String ma) throws SQLException {
        return Jdbc.one(c, "SELECT maPhieuNhap, ngayNhap, trangThai, loaiNhap, maNhaCungCap, maPhieuXuatGoc, maNhanVien, ghiChu FROM PhieuNhapKho WHERE maPhieuNhap = ?",
                rs -> new PhieuNhapKho(rs.getString(1), Jdbc.dateTime(rs, "ngayNhap"), rs.getString(3), LoaiNhap.of(rs.getString(4)), rs.getString(5),
                        rs.getString(6), rs.getString(7), rs.getString(8)), ma);
    }

    /** Danh sách phiếu nhập, hoàn trả và xuất trong khoảng thời gian, mới nhất trước. */
    public List<PhieuKhoView> list(Connection c, LocalDateTime tu, LocalDateTime den) throws SQLException {
        List<PhieuKhoView> out = new ArrayList<>(Jdbc.list(c, "SELECT pn.maPhieuNhap, pn.loaiNhap, pn.ngayNhap, ncc.tenNhaCungCap, nv.hoTen, pn.ghiChu, pn.maPhieuXuatGoc, "
                        + "(SELECT COUNT(*) FROM ChiTietPhieuNhap c1 WHERE c1.maPhieuNhap = pn.maPhieuNhap) AS soDong, "
                        + "(SELECT COALESCE(SUM(c2.soLuong), 0) FROM ChiTietPhieuNhap c2 WHERE c2.maPhieuNhap = pn.maPhieuNhap) AS tongSl "
                        + "FROM PhieuNhapKho pn LEFT JOIN NhaCungCap ncc ON ncc.maNhaCungCap = pn.maNhaCungCap JOIN NhanVien nv ON nv.maNhanVien = pn.maNhanVien "
                        + "WHERE pn.ngayNhap >= ? AND pn.ngayNhap < ?",
                rs -> new PhieuKhoView(rs.getString("maPhieuNhap"), rs.getString("loaiNhap"), Jdbc.dateTime(rs, "ngayNhap"),
                        rs.getString("tenNhaCungCap") != null ? rs.getString("tenNhaCungCap") : "Hoàn trả từ " + rs.getString("maPhieuXuatGoc"),
                        rs.getString("hoTen"), rs.getInt("soDong"), rs.getInt("tongSl"), rs.getString("ghiChu"), rs.getString("maPhieuXuatGoc")), tu, den));
        out.addAll(Jdbc.list(c, "SELECT px.maPhieuXuat, px.ngayXuat, px.boPhanNhan, px.nguoiNhan, px.lyDo, nv.hoTen, "
                        + "(SELECT COUNT(*) FROM ChiTietPhieuXuat c1 WHERE c1.maPhieuXuat = px.maPhieuXuat) AS soDong, "
                        + "(SELECT COALESCE(SUM(c2.soLuong), 0) FROM ChiTietPhieuXuat c2 WHERE c2.maPhieuXuat = px.maPhieuXuat) AS tongSl "
                        + "FROM PhieuXuatKho px JOIN NhanVien nv ON nv.maNhanVien = px.maNhanVien WHERE px.ngayXuat >= ? AND px.ngayXuat < ?",
                rs -> new PhieuKhoView(rs.getString("maPhieuXuat"), "XUAT", Jdbc.dateTime(rs, "ngayXuat"),
                        rs.getString("boPhanNhan") + (rs.getString("nguoiNhan") == null ? "" : " · " + rs.getString("nguoiNhan")),
                        rs.getString("hoTen"), rs.getInt("soDong"), rs.getInt("tongSl"), rs.getString("lyDo"), null), tu, den));
        out.sort(Comparator.comparing(PhieuKhoView::ngay).reversed().thenComparing(PhieuKhoView::ma, Comparator.reverseOrder()));
        return out;
    }

    private static final String LINE_TAIL = "JOIN LoHang lo ON lo.maLo = ct.maLo LEFT JOIN Thuoc t ON t.maThuoc = lo.maThuoc LEFT JOIN VatTu v ON v.maVatTu = lo.maVatTu ";
    private static final String LINE_COLS = "ct.maLo, lo.soLo, COALESCE(lo.maThuoc, lo.maVatTu) AS maHang, COALESCE(t.tenThuoc, v.tenVatTu) AS tenHang, "
            + "COALESCE(t.donViTinh, v.donViTinh) AS dvt, lo.hanSuDung, ct.soLuong, COALESCE(t.kiemSoatDacBiet, 0) AS ksdb";

    private static PhieuKhoDong mapLine(ResultSet rs, BigDecimal donGia) throws SQLException {
        return new PhieuKhoDong(rs.getString("maLo"), rs.getString("soLo"), rs.getString("maHang"), rs.getString("tenHang"), rs.getString("dvt"),
                Jdbc.date(rs, "hanSuDung"), rs.getInt("soLuong"), donGia, rs.getInt("ksdb") == 1);
    }

    public List<PhieuKhoDong> linesNhap(Connection c, String ma) throws SQLException {
        return Jdbc.list(c, "SELECT " + LINE_COLS + ", ct.donGiaNhap FROM ChiTietPhieuNhap ct " + LINE_TAIL + "WHERE ct.maPhieuNhap = ? ORDER BY ct.maLo",
                rs -> mapLine(rs, Jdbc.money(rs, "donGiaNhap")), ma);
    }

    public List<PhieuKhoDong> linesXuat(Connection c, String ma) throws SQLException {
        return Jdbc.list(c, "SELECT " + LINE_COLS + " FROM ChiTietPhieuXuat ct " + LINE_TAIL + "WHERE ct.maPhieuXuat = ? ORDER BY ct.maLo",
                rs -> mapLine(rs, null), ma);
    }

    public int qtyXuat(Connection c, String maPx, String maLo) throws SQLException {
        return (int) Jdbc.count(c, "SELECT COALESCE(SUM(soLuong), 0) FROM ChiTietPhieuXuat WHERE maPhieuXuat = ? AND maLo = ?", maPx, maLo);
    }

    public int qtyDaHoanTra(Connection c, String maPx, String maLo) throws SQLException {
        return (int) Jdbc.count(c, "SELECT COALESCE(SUM(ct.soLuong), 0) FROM ChiTietPhieuNhap ct JOIN PhieuNhapKho pn ON pn.maPhieuNhap = ct.maPhieuNhap "
                + "WHERE pn.loaiNhap = 'HOAN_TRA' AND pn.maPhieuXuatGoc = ? AND ct.maLo = ?", maPx, maLo);
    }

    /** Sổ theo dõi thuốc kiểm soát đặc biệt: nhập, hoàn trả, xuất, điều chỉnh đã duyệt, kê đơn. */
    public List<SoKsdbRow> soKsdb(Connection c, LocalDateTime tu, LocalDateTime den) throws SQLException {
        List<SoKsdbRow> out = new ArrayList<>();
        out.addAll(Jdbc.list(c, "SELECT pn.ngayNhap AS tg, pn.loaiNhap, pn.maPhieuNhap AS ct, t.maThuoc, t.tenThuoc, lo.soLo, d.soLuong, nv.hoTen, pn.ghiChu, ncc.tenNhaCungCap "
                        + "FROM ChiTietPhieuNhap d JOIN PhieuNhapKho pn ON pn.maPhieuNhap = d.maPhieuNhap JOIN LoHang lo ON lo.maLo = d.maLo "
                        + "JOIN Thuoc t ON t.maThuoc = lo.maThuoc JOIN NhanVien nv ON nv.maNhanVien = pn.maNhanVien "
                        + "LEFT JOIN NhaCungCap ncc ON ncc.maNhaCungCap = pn.maNhaCungCap WHERE t.kiemSoatDacBiet = 1 AND pn.ngayNhap >= ? AND pn.ngayNhap < ?",
                rs -> new SoKsdbRow(Jdbc.dateTime(rs, "tg"), "HOAN_TRA".equals(rs.getString("loaiNhap")) ? "Hoàn trả" : "Nhập", rs.getString("ct"),
                        rs.getString("maThuoc"), rs.getString("tenThuoc"), rs.getString("soLo"), rs.getInt("soLuong"), rs.getString("hoTen"),
                        rs.getString("tenNhaCungCap") != null ? "NCC: " + rs.getString("tenNhaCungCap") : rs.getString("ghiChu")), tu, den));
        out.addAll(Jdbc.list(c, "SELECT px.ngayXuat AS tg, px.maPhieuXuat AS ct, t.maThuoc, t.tenThuoc, lo.soLo, d.soLuong, nv.hoTen, px.boPhanNhan, px.nguoiNhan, "
                        + "(SELECT MAX(bs.hoTen) FROM NhatKyThaoTac nk JOIN NhanVien bs ON bs.maNhanVien = nk.maNguoiChiuTrachNhiem WHERE nk.doiTuong = px.maPhieuXuat) AS bsTn "
                        + "FROM ChiTietPhieuXuat d JOIN PhieuXuatKho px ON px.maPhieuXuat = d.maPhieuXuat JOIN LoHang lo ON lo.maLo = d.maLo "
                        + "JOIN Thuoc t ON t.maThuoc = lo.maThuoc JOIN NhanVien nv ON nv.maNhanVien = px.maNhanVien "
                        + "WHERE t.kiemSoatDacBiet = 1 AND px.ngayXuat >= ? AND px.ngayXuat < ?",
                rs -> new SoKsdbRow(Jdbc.dateTime(rs, "tg"), "Xuất", rs.getString("ct"), rs.getString("maThuoc"), rs.getString("tenThuoc"),
                        rs.getString("soLo"), -rs.getInt("soLuong"), rs.getString("hoTen"),
                        "Nhận: " + rs.getString("boPhanNhan") + (rs.getString("nguoiNhan") == null ? "" : " — " + rs.getString("nguoiNhan"))
                                + (rs.getString("bsTn") == null ? "" : " · BS chịu trách nhiệm: " + rs.getString("bsTn"))), tu, den));
        out.addAll(Jdbc.list(c, "SELECT p.thoiGianDuyet AS tg, p.maPhieu AS ct, p.loai, t.maThuoc, t.tenThuoc, lo.soLo, d.soLuongHeThong, d.soLuongThucTe, n.hoTen, p.lyDo "
                        + "FROM ChiTietDieuChinhKho d JOIN PhieuDieuChinhKho p ON p.maPhieu = d.maPhieu JOIN LoHang lo ON lo.maLo = d.maLo "
                        + "JOIN Thuoc t ON t.maThuoc = lo.maThuoc JOIN TaiKhoan tk ON tk.maTaiKhoan = p.maTaiKhoanDuyet JOIN NhanVien n ON n.maNhanVien = tk.maNhanVien "
                        + "WHERE t.kiemSoatDacBiet = 1 AND p.trangThai = 'DA_DUYET' AND p.thoiGianDuyet >= ? AND p.thoiGianDuyet < ?",
                rs -> new SoKsdbRow(Jdbc.dateTime(rs, "tg"), "Điều chỉnh", rs.getString("ct"), rs.getString("maThuoc"), rs.getString("tenThuoc"),
                        rs.getString("soLo"), rs.getInt("soLuongThucTe") - rs.getInt("soLuongHeThong"), rs.getString("hoTen") + " (duyệt)",
                        rs.getString("lyDo")), tu, den));
        out.addAll(Jdbc.list(c, "SELECT pk.thoiGianTiepNhan AS tg, dt.maDonThuoc AS ct, t.maThuoc, t.tenThuoc, d.soLuong, bs.hoTen, bn.hoTen AS tenBn "
                        + "FROM ChiTietDonThuoc d JOIN DonThuoc dt ON dt.maDonThuoc = d.maDonThuoc JOIN PhieuKham pk ON pk.maPhieuKham = dt.maPhieuKham "
                        + "JOIN Thuoc t ON t.maThuoc = d.maThuoc JOIN NhanVien bs ON bs.maNhanVien = dt.maNhanVien JOIN BenhNhan bn ON bn.maBenhNhan = dt.maBenhNhan "
                        + "WHERE t.kiemSoatDacBiet = 1 AND pk.thoiGianTiepNhan >= ? AND pk.thoiGianTiepNhan < ?",
                rs -> new SoKsdbRow(Jdbc.dateTime(rs, "tg"), "Kê đơn", rs.getString("ct"), rs.getString("maThuoc"), rs.getString("tenThuoc"), "—",
                        rs.getInt("soLuong"), rs.getString("hoTen"), "Bệnh nhân: " + rs.getString("tenBn") + " (kê đơn, không trừ kho)"), tu, den));
        out.sort(Comparator.comparing(SoKsdbRow::thoiGian, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return out;
    }
}
