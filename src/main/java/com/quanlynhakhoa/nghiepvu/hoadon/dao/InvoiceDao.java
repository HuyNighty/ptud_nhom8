package com.quanlynhakhoa.nghiepvu.hoadon.dao;

import com.quanlynhakhoa.nghiepvu.hoadon.model.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Read only. Aggregates never join raw detail rows to raw payment rows. */
public class InvoiceDao {
    private static final String LEDGER = """
            WITH WithDebt AS (
                SELECT h.maHoaDon,h.maNhanVien,h.maBenhNhan,
                       COALESCE(h.tenBenhNhanChot,b.hoTen) AS tenBenhNhan,
                       h.maPhieuKham,h.ngayLap,h.trangThai,
                       COALESCE(h.soDienThoaiChot,b.soDienThoai,'') AS soDienThoai,
                       COALESCE(l.tongTien,0) AS tongTien,COALESCE(p.daThu,0) AS daThu,
                       CAST(0 AS decimal(38,0)) AS congNoBenhNhan
                FROM dbo.HoaDon h JOIN dbo.BenhNhan b ON b.maBenhNhan=h.maBenhNhan
                OUTER APPLY (SELECT SUM(CAST(d.soLuong AS decimal(10,0))*d.donGiaApDung) AS tongTien
                             FROM dbo.ChiTietHoaDon d WHERE d.maHoaDon=h.maHoaDon) l
                OUTER APPLY (SELECT SUM(t.soTien) AS daThu FROM dbo.ThanhToan t
                             WHERE t.maHoaDon=h.maHoaDon AND t.trangThai='THANH_CONG') p
            )
            SELECT maHoaDon,maNhanVien,maBenhNhan,tenBenhNhan,maPhieuKham,ngayLap,
                   trangThai,tongTien,daThu,congNoBenhNhan,
                   (SELECT TOP (1) y.trangThai FROM dbo.YeuCauHuyHoaDon y
                    WHERE y.maHoaDon=WithDebt.maHoaDon
                    ORDER BY CASE WHEN y.trangThai='CHO_DUYET' THEN 0 ELSE 1 END,
                             y.thoiGianTao DESC,y.maYeuCau DESC) AS cancellationStatus
            FROM WithDebt
            """;

    public List<Invoice> find(Connection connection, String keyword, LocalDate from, LocalDate to,
                             InvoiceStatus status) throws SQLException {
        return findPage(connection,keyword,from,to,status,0,0);
    }
    public List<Invoice> findPage(Connection connection,String keyword,LocalDate from,LocalDate to,
                                  InvoiceStatus status,int offset,int limit)throws SQLException {
        return findPage(connection,keyword,from,to,status,offset,limit,false);
    }
    public List<Invoice> findPage(Connection connection,String keyword,LocalDate from,LocalDate to,
                                  InvoiceStatus status,int offset,int limit,boolean outstandingOnly)throws SQLException {
        if(offset<0||limit<0||limit>201)throw new IllegalArgumentException("Invalid page");
        String sql = LEDGER + """
                WHERE (? = N'' OR CHARINDEX(?,maHoaDon)>0 OR CHARINDEX(?,maBenhNhan)>0 OR CHARINDEX(?,tenBenhNhan + N' ' + soDienThoai)>0)
                AND (? IS NULL OR ngayLap>=?) AND (? IS NULL OR ngayLap<?)
                AND (? IS NULL OR trangThai=?) ORDER BY ngayLap DESC, maHoaDon
                """;
        if(outstandingOnly)sql=sql.replace("ORDER BY ngayLap DESC, maHoaDon","AND trangThai<>'DA_HUY' AND tongTien>daThu ORDER BY ngayLap DESC, maHoaDon");
        if(limit>0)sql+=" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(15);
            for (int i = 1; i <= 4; i++) statement.setNString(i, keyword);
            LocalDateTime start = from == null ? null : from.atStartOfDay();
            LocalDateTime end = to == null ? null : to.plusDays(1).atStartOfDay();
            dateTime(statement, 5, start); dateTime(statement, 6, start);
            dateTime(statement, 7, end); dateTime(statement, 8, end);
            statement.setString(9, status == null ? null : status.name());
            statement.setString(10, status == null ? null : status.name());
            if(limit>0){statement.setInt(11,offset);statement.setInt(12,limit);}
            List<Invoice> result = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) { while (rows.next()) result.add(map(rows)); }
            return List.copyOf(result);
        }
    }

    public Invoice get(Connection connection, String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(LEDGER + " WHERE maHoaDon=?")) {
            statement.setQueryTimeout(15);
            statement.setString(1, id);
            try (ResultSet rows = statement.executeQuery()) { if(!rows.next())return null;
                Invoice result=map(rows);
                java.math.BigDecimal debt=patientDebt(connection,result.maBenhNhan());
                return new Invoice(result.maHoaDon(),result.maNhanVien(),result.maBenhNhan(),result.tenBenhNhan(),result.maPhieuKham(),result.ngayLap(),result.trangThai(),result.tongTien(),result.daThu(),debt,result.cancellationStatus()); }
        }
    }
    /** Targeted money snapshot under the caller's invoice lock; avoids scanning unrelated invoices while collecting. */
    public Invoice getForPayment(Connection connection,String id)throws SQLException {
        try(PreparedStatement s=connection.prepareStatement("""
                SELECT h.maHoaDon,h.maNhanVien,h.maBenhNhan,COALESCE(h.tenBenhNhanChot,b.hoTen) AS tenBenhNhan,h.maPhieuKham,h.ngayLap,h.trangThai,
                    COALESCE((SELECT SUM(CAST(d.soLuong AS decimal(10,0))*d.donGiaApDung)
                              FROM dbo.ChiTietHoaDon d WHERE d.maHoaDon=h.maHoaDon),0) AS tongTien,
                    COALESCE((SELECT SUM(t.soTien) FROM dbo.ThanhToan t
                              WHERE t.maHoaDon=h.maHoaDon AND t.trangThai='THANH_CONG'),0) AS daThu,
                    CAST(0 AS decimal(38,0)) AS congNoBenhNhan, CAST(NULL AS varchar(10)) AS cancellationStatus
                FROM dbo.HoaDon h JOIN dbo.BenhNhan b ON b.maBenhNhan=h.maBenhNhan WHERE h.maHoaDon=?
                """)) {
            s.setQueryTimeout(15);s.setString(1,id);try(ResultSet r=s.executeQuery()){return r.next()?map(r):null;}
        }
    }

    private java.math.BigDecimal patientDebt(Connection c,String patient)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("""
            SELECT COALESCE(SUM(COALESCE(d.total,0)-COALESCE(p.paid,0)),0)
            FROM dbo.HoaDon h
            OUTER APPLY(SELECT SUM(CAST(x.soLuong AS decimal(10,0))*x.donGiaApDung) total FROM dbo.ChiTietHoaDon x WHERE x.maHoaDon=h.maHoaDon) d
            OUTER APPLY(SELECT SUM(t.soTien) paid FROM dbo.ThanhToan t WHERE t.maHoaDon=h.maHoaDon AND t.trangThai='THANH_CONG') p
            WHERE h.maBenhNhan=? AND h.trangThai<>'DA_HUY'
            """)){
            s.setQueryTimeout(15);s.setString(1,patient);try(ResultSet rows=s.executeQuery()){if(!rows.next())throw new SQLException("Missing patient debt");return rows.getBigDecimal(1);}
        }
    }

    public List<InvoiceLine> lines(Connection connection, String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT ct.maChiTiet, ct.maHoaDon, ct.maDichVu, ct.maThuoc, ct.maVatTu, ct.soLuong, ct.donGiaApDung,
                       COALESCE(ct.tenHangChot,d.tenDichVu,t.tenThuoc,v.tenVatTu) AS tenHang
                FROM dbo.ChiTietHoaDon ct
                LEFT JOIN dbo.DichVu d ON d.maDichVu=ct.maDichVu
                LEFT JOIN dbo.Thuoc t ON t.maThuoc=ct.maThuoc
                LEFT JOIN dbo.VatTu v ON v.maVatTu=ct.maVatTu
                WHERE ct.maHoaDon=? ORDER BY ct.maChiTiet
                """)) {
            statement.setQueryTimeout(15); statement.setString(1, id);
            List<InvoiceLine> result = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(new InvoiceLine(rows.getInt("maChiTiet"), rows.getString("maHoaDon"),
                        rows.getString("maDichVu"), rows.getString("maThuoc"), rows.getString("maVatTu"),
                        rows.getInt("soLuong"), rows.getBigDecimal("donGiaApDung"),rows.getNString("tenHang")));
            }
            return List.copyOf(result);
        }
    }

    public List<PrescribedMedicine> prescribedMedicines(Connection connection, String invoiceId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT t.maThuoc,COALESCE(b.tenHangChot,t.tenThuoc) AS tenThuoc,t.donViTinh,
                       COALESCE(b.soLuong,ct.soLuong) AS soLuong,b.donGiaApDung
                FROM dbo.HoaDon h
                JOIN dbo.DonThuoc d ON d.maPhieuKham=h.maPhieuKham AND d.maBenhNhan=h.maBenhNhan
                JOIN dbo.ChiTietDonThuoc ct ON ct.maDonThuoc=d.maDonThuoc
                JOIN dbo.Thuoc t ON t.maThuoc=ct.maThuoc
                LEFT JOIN dbo.ChiTietHoaDon b ON b.maHoaDon=h.maHoaDon AND b.maThuoc=ct.maThuoc
                WHERE h.maHoaDon=? ORDER BY t.tenThuoc,t.maThuoc,b.maChiTiet
                """)) {
            statement.setQueryTimeout(15); statement.setString(1, invoiceId);
            List<PrescribedMedicine> result = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(new PrescribedMedicine(rows.getString("maThuoc"),
                        rows.getNString("tenThuoc"), rows.getNString("donViTinh"), rows.getInt("soLuong"),
                        rows.getBigDecimal("donGiaApDung")));
            }
            return List.copyOf(result);
        }
    }
    public List<Payment> payments(Connection connection, String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT maThanhToan, maHoaDon, thoiGian, soTien, phuongThuc, maGiaoDich, tienKhachDua,
                       trangThai, maNhanVien, maTaiKhoanHuy, thoiGianHuy, lyDoHuy
                FROM dbo.ThanhToan WHERE maHoaDon=? ORDER BY thoiGian, maThanhToan
                """)) {
            statement.setQueryTimeout(15); statement.setString(1, id);
            List<Payment> result = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(new Payment(rows.getString("maThanhToan"), rows.getString("maHoaDon"),
                        time(rows, "thoiGian"), rows.getBigDecimal("soTien"),
                        Payment.PhuongThuc.valueOf(rows.getString("phuongThuc")), rows.getString("maGiaoDich"),
                        rows.getBigDecimal("tienKhachDua"), Payment.TrangThai.valueOf(rows.getString("trangThai")),
                        rows.getString("maNhanVien"), rows.getString("maTaiKhoanHuy"), time(rows, "thoiGianHuy"),
                        rows.getNString("lyDoHuy")));
            }
            return List.copyOf(result);
        }
    }

    private static Invoice map(ResultSet rows) throws SQLException {
        return new Invoice(rows.getString("maHoaDon"), rows.getString("maNhanVien"), rows.getString("maBenhNhan"),
                rows.getNString("tenBenhNhan"), rows.getString("maPhieuKham"), time(rows, "ngayLap"),
                InvoiceStatus.valueOf(rows.getString("trangThai")), rows.getBigDecimal("tongTien"),
                rows.getBigDecimal("daThu"), rows.getBigDecimal("congNoBenhNhan"),
                rows.getString("cancellationStatus") == null ? null
                        : InvoiceCancellationRequest.TrangThai.valueOf(rows.getString("cancellationStatus")));
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
