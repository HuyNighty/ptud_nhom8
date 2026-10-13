package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.dto.DonThuocView;
import vn.n08.nhakhoa.model.ChiTietDonThuoc;
import vn.n08.nhakhoa.model.DonThuoc;
import vn.n08.nhakhoa.util.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Bảng DonThuoc + ChiTietDonThuoc. */
public class DonThuocDao {
    static DonThuoc map(ResultSet rs) throws SQLException {
        return new DonThuoc(rs.getString("maDonThuoc"), rs.getString("maPhieuKham"), rs.getString("maBenhNhan"), rs.getString("maNhanVien"),
                Jdbc.date(rs, "ngayKe"), rs.getString("loiDan"));
    }

    public Optional<DonThuoc> byPhieuKham(Connection c, String maPk) throws SQLException {
        return Jdbc.one(c, "SELECT maDonThuoc, maPhieuKham, maBenhNhan, maNhanVien, ngayKe, loiDan FROM DonThuoc WHERE maPhieuKham = ?", DonThuocDao::map, maPk);
    }

    public Optional<DonThuocView> viewByPhieuKham(Connection c, String maPk) throws SQLException {
        Optional<DonThuoc> d = byPhieuKham(c, maPk);
        if (d.isEmpty()) return Optional.empty();
        String tenBs = Jdbc.one(c, "SELECT hoTen FROM NhanVien WHERE maNhanVien = ?", rs -> rs.getString(1), d.get().maNhanVien()).orElse("");
        List<DonThuocView.Dong> dong = Jdbc.list(c, "SELECT ct.maDonThuoc, ct.maThuoc, ct.soLuong, ct.lieuDung, ct.cachDung, ct.soNgayDung, "
                        + "t.tenThuoc, t.hamLuong, t.donViTinh, t.kiemSoatDacBiet FROM ChiTietDonThuoc ct JOIN Thuoc t ON t.maThuoc = ct.maThuoc "
                        + "WHERE ct.maDonThuoc = ? ORDER BY t.tenThuoc",
                rs -> new DonThuocView.Dong(new ChiTietDonThuoc(rs.getString("maDonThuoc"), rs.getString("maThuoc"), rs.getInt("soLuong"),
                        rs.getString("lieuDung"), rs.getString("cachDung"), rs.getInt("soNgayDung")),
                        rs.getString("tenThuoc") + (rs.getString("hamLuong") == null ? "" : " " + rs.getString("hamLuong")),
                        rs.getString("donViTinh"), rs.getBoolean("kiemSoatDacBiet")), d.get().maDonThuoc());
        return Optional.of(new DonThuocView(d.get(), tenBs, dong));
    }

    public void insert(Connection c, DonThuoc d) throws SQLException {
        Jdbc.update(c, "INSERT INTO DonThuoc (maDonThuoc, maPhieuKham, maBenhNhan, maNhanVien, ngayKe, loiDan) VALUES (?,?,?,?,?,?)",
                d.maDonThuoc(), d.maPhieuKham(), d.maBenhNhan(), d.maNhanVien(), d.ngayKe(), d.loiDan());
    }

    public int updateHeader(Connection c, String ma, String loiDan, String maBs, java.time.LocalDate ngayKe) throws SQLException {
        return Jdbc.update(c, "UPDATE DonThuoc SET loiDan = ?, maNhanVien = ?, ngayKe = ? WHERE maDonThuoc = ?", loiDan, maBs, ngayKe, ma);
    }

    public void replaceLines(Connection c, String maDon, List<ChiTietDonThuoc> lines) throws SQLException {
        Jdbc.update(c, "DELETE FROM ChiTietDonThuoc WHERE maDonThuoc = ?", maDon);
        for (ChiTietDonThuoc l : lines) {
            Jdbc.update(c, "INSERT INTO ChiTietDonThuoc (maDonThuoc, maThuoc, soLuong, lieuDung, cachDung, soNgayDung) VALUES (?,?,?,?,?,?)",
                    maDon, l.maThuoc(), l.soLuong(), l.lieuDung(), l.cachDung(), l.soNgayDung());
        }
    }
}
