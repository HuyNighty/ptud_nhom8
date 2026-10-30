package com.quanlynhakhoa.billing.dao;

import com.quanlynhakhoa.billing.model.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Read only. Aggregates never join raw detail rows to raw payment rows. */
public class HoaDonDao {
    private static final String LEDGER = """
            WITH Lines AS (
                SELECT maHoaDon, SUM(CAST(soLuong AS decimal(10,0)) * donGiaApDung) AS tongTien
                FROM dbo.ChiTietHoaDon GROUP BY maHoaDon
            ), Payments AS (
                SELECT maHoaDon, SUM(soTien) AS daThu FROM dbo.ThanhToan
                WHERE trangThai='THANH_CONG' GROUP BY maHoaDon
            ), Ledger AS (
                SELECT h.maHoaDon, h.maNhanVien, h.maBenhNhan, b.hoTen AS tenBenhNhan,
                       h.maPhieuKham, h.ngayLap, h.trangThai, b.soDienThoai,
                       COALESCE(l.tongTien, 0) AS tongTien, COALESCE(p.daThu, 0) AS daThu
                FROM dbo.HoaDon h
                JOIN dbo.BenhNhan b ON b.maBenhNhan=h.maBenhNhan
                LEFT JOIN Lines l ON l.maHoaDon=h.maHoaDon
                LEFT JOIN Payments p ON p.maHoaDon=h.maHoaDon
            ), WithDebt AS (
                SELECT maHoaDon, maNhanVien, maBenhNhan, tenBenhNhan, maPhieuKham, ngayLap,
                       trangThai, tongTien, daThu, soDienThoai,
                       SUM(CASE WHEN trangThai='DA_HUY' THEN 0 ELSE tongTien-daThu END)
                           OVER (PARTITION BY maBenhNhan) AS congNoBenhNhan
                FROM Ledger
            )
            SELECT maHoaDon, maNhanVien, maBenhNhan, tenBenhNhan, maPhieuKham, ngayLap,
                   trangThai, tongTien, daThu, congNoBenhNhan FROM WithDebt
            """;

    public List<HoaDon> find(Connection connection, String keyword, LocalDate from, LocalDate to,
                             TrangThaiHoaDon status) throws SQLException {
        String sql = LEDGER + """
                WHERE (CHARINDEX(?,maHoaDon)>0 OR CHARINDEX(?,maBenhNhan)>0 OR CHARINDEX(?,tenBenhNhan + N' ' + soDienThoai)>0)
                AND (? IS NULL OR ngayLap>=?) AND (? IS NULL OR ngayLap<?)
                AND (? IS NULL OR trangThai=?) ORDER BY ngayLap DESC, maHoaDon
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(15);
            for (int i = 1; i <= 3; i++) statement.setNString(i, keyword);
            LocalDateTime start = from == null ? null : from.atStartOfDay();
            LocalDateTime end = to == null ? null : to.plusDays(1).atStartOfDay();
            dateTime(statement, 4, start); dateTime(statement, 5, start);
            dateTime(statement, 6, end); dateTime(statement, 7, end);
            statement.setString(8, status == null ? null : status.name());
            statement.setString(9, status == null ? null : status.name());
            List<HoaDon> result = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) { while (rows.next()) result.add(map(rows)); }
            return List.copyOf(result);
        }
    }

    public HoaDon get(Connection connection, String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(LEDGER + " WHERE maHoaDon=?")) {
            statement.setQueryTimeout(15);
            statement.setString(1, id);
            try (ResultSet rows = statement.executeQuery()) { return rows.next() ? map(rows) : null; }
        }
    }
    /** Targeted money snapshot under the caller's invoice lock; avoids scanning unrelated invoices while collecting. */
    public HoaDon getForPayment(Connection connection,String id)throws SQLException {
        try(PreparedStatement s=connection.prepareStatement("""
                SELECT h.maHoaDon,h.maNhanVien,h.maBenhNhan,b.hoTen AS tenBenhNhan,h.maPhieuKham,h.ngayLap,h.trangThai,
                    COALESCE((SELECT SUM(CAST(d.soLuong AS decimal(10,0))*d.donGiaApDung)
                              FROM dbo.ChiTietHoaDon d WHERE d.maHoaDon=h.maHoaDon),0) AS tongTien,
                    COALESCE((SELECT SUM(t.soTien) FROM dbo.ThanhToan t
                              WHERE t.maHoaDon=h.maHoaDon AND t.trangThai='THANH_CONG'),0) AS daThu,
                    CAST(0 AS decimal(38,0)) AS congNoBenhNhan
                FROM dbo.HoaDon h JOIN dbo.BenhNhan b ON b.maBenhNhan=h.maBenhNhan WHERE h.maHoaDon=?
                """)) {
            s.setQueryTimeout(15);s.setString(1,id);try(ResultSet r=s.executeQuery()){return r.next()?map(r):null;}
        }
    }

    public List<ChiTietHoaDon> lines(Connection connection, String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT maChiTiet, maHoaDon, maDichVu, maThuoc, maVatTu, soLuong, donGiaApDung
                FROM dbo.ChiTietHoaDon WHERE maHoaDon=? ORDER BY maChiTiet
                """)) {
            statement.setQueryTimeout(15); statement.setString(1, id);
            List<ChiTietHoaDon> result = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(new ChiTietHoaDon(rows.getInt("maChiTiet"), rows.getString("maHoaDon"),
                        rows.getString("maDichVu"), rows.getString("maThuoc"), rows.getString("maVatTu"),
                        rows.getInt("soLuong"), rows.getBigDecimal("donGiaApDung")));
            }
            return List.copyOf(result);
        }
    }

    public List<ThanhToan> payments(Connection connection, String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT maThanhToan, maHoaDon, thoiGian, soTien, phuongThuc, maGiaoDich, tienKhachDua,
                       trangThai, maNhanVien, maTaiKhoanHuy, thoiGianHuy, lyDoHuy
                FROM dbo.ThanhToan WHERE maHoaDon=? ORDER BY thoiGian, maThanhToan
                """)) {
            statement.setQueryTimeout(15); statement.setString(1, id);
            List<ThanhToan> result = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(new ThanhToan(rows.getString("maThanhToan"), rows.getString("maHoaDon"),
                        time(rows, "thoiGian"), rows.getBigDecimal("soTien"),
                        ThanhToan.PhuongThuc.valueOf(rows.getString("phuongThuc")), rows.getString("maGiaoDich"),
                        rows.getBigDecimal("tienKhachDua"), ThanhToan.TrangThai.valueOf(rows.getString("trangThai")),
                        rows.getString("maNhanVien"), rows.getString("maTaiKhoanHuy"), time(rows, "thoiGianHuy"),
                        rows.getNString("lyDoHuy")));
            }
            return List.copyOf(result);
        }
    }

    private static HoaDon map(ResultSet rows) throws SQLException {
        return new HoaDon(rows.getString("maHoaDon"), rows.getString("maNhanVien"), rows.getString("maBenhNhan"),
                rows.getNString("tenBenhNhan"), rows.getString("maPhieuKham"), time(rows, "ngayLap"),
                TrangThaiHoaDon.valueOf(rows.getString("trangThai")), rows.getBigDecimal("tongTien"),
                rows.getBigDecimal("daThu"), rows.getBigDecimal("congNoBenhNhan"));
    }
    private static LocalDateTime time(ResultSet rows, String column) throws SQLException {
        Timestamp value = rows.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }
    private static void dateTime(PreparedStatement statement, int index, LocalDateTime value) throws SQLException {
        if (value == null) statement.setNull(index, Types.TIMESTAMP);
        else statement.setTimestamp(index, Timestamp.valueOf(value));
    }
}
