package com.quanlynhakhoa.hethong.tongquan;

import java.sql.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import com.quanlynhakhoa.hethong.tongquan.DashboardService.*;

/** Bounded, read-only queries for the overview. */
final class DashboardDetailsDao {
    private PreparedStatement prepare(Connection c, String sql) throws SQLException {
        PreparedStatement s=c.prepareStatement(sql);s.setQueryTimeout(10);return s;
    }
    long count(Connection c, String sql, String account) throws SQLException {
        try(PreparedStatement s=prepare(c,sql)) {
            if(account!=null)s.setString(1,account);
            try(ResultSet rows=s.executeQuery()){if(!rows.next())throw new SQLException("Missing count");return rows.getLong(1);}
        }
    }
    List<Appointment> appointments(Connection c, LocalDate today) throws SQLException {
        String sql="""
            SELECT TOP (8) l.maLichHen,l.thoiGianHen,b.hoTen,n.hoTen,l.trangThai
            FROM dbo.LichHen l JOIN dbo.BenhNhan b ON b.maBenhNhan=l.maBenhNhan
            JOIN dbo.NhanVien n ON n.maNhanVien=l.maNhanVien
            WHERE l.thoiGianHen>=? AND l.thoiGianHen<?
              AND l.trangThai IN ('CHO_XAC_NHAN','DA_XAC_NHAN')
            ORDER BY l.thoiGianHen,l.maLichHen
            """;
        List<Appointment> result=new ArrayList<>();
        try(PreparedStatement s=prepare(c,sql)) {
            s.setObject(1,today.atStartOfDay());s.setObject(2,today.plusDays(1).atStartOfDay());
            try(ResultSet rows=s.executeQuery()){while(rows.next())result.add(new Appointment(rows.getString(1),rows.getTimestamp(2).toLocalDateTime(),rows.getNString(3),rows.getNString(4),rows.getString(5)));}
        }
        return List.copyOf(result);
    }
    List<StockAlert> stock(Connection c, LocalDate today) throws SQLException {
        List<StockAlert> result=new ArrayList<>();
        String low="""
            WITH items AS (
                SELECT N'Thuốc' AS loai,maThuoc AS ma,tenThuoc AS ten,dinhMucToiThieu FROM dbo.Thuoc
                UNION ALL SELECT N'Vật tư',maVatTu,tenVatTu,dinhMucToiThieu FROM dbo.VatTu
            ), stock AS (
                SELECT COALESCE(maThuoc,maVatTu) AS ma,
                       CASE WHEN maThuoc IS NOT NULL THEN N'Thuốc' ELSE N'Vật tư' END AS loai,
                       SUM(CONVERT(bigint,soLuongTon)) AS ton
                FROM dbo.LoHang WHERE hanSuDung IS NULL OR hanSuDung>=?
                GROUP BY COALESCE(maThuoc,maVatTu),CASE WHEN maThuoc IS NOT NULL THEN N'Thuốc' ELSE N'Vật tư' END
            )
            SELECT TOP (8) i.loai,i.ma,i.ten,COALESCE(s.ton,0),i.dinhMucToiThieu
            FROM items i LEFT JOIN stock s ON s.ma=i.ma AND s.loai=i.loai
            WHERE COALESCE(s.ton,0)<i.dinhMucToiThieu ORDER BY i.loai,i.ma
            """;
        try(PreparedStatement s=prepare(c,low)) {
            s.setObject(1,today);
            try(ResultSet rows=s.executeQuery()){while(rows.next())result.add(new StockAlert("Tồn thấp",rows.getString(2)+" · "+rows.getNString(3),"Còn "+rows.getLong(4)+" / định mức "+rows.getInt(5),"Vật tư".equals(rows.getNString(1))?"low-supply":"low"));}
        }
        String expiry="""
            SELECT TOP (8) l.maLo,COALESCE(t.tenThuoc,v.tenVatTu),l.hanSuDung,l.soLuongTon
            FROM dbo.LoHang l LEFT JOIN dbo.Thuoc t ON t.maThuoc=l.maThuoc
            LEFT JOIN dbo.VatTu v ON v.maVatTu=l.maVatTu
            WHERE l.soLuongTon>0 AND l.hanSuDung<=?
            ORDER BY l.hanSuDung,l.maLo
            """;
        try(PreparedStatement s=prepare(c,expiry)) {
            s.setObject(1,today.plusDays(30));
            try(ResultSet rows=s.executeQuery()){while(rows.next()){
                LocalDate expiryDate=rows.getDate(3).toLocalDate();
                result.add(new StockAlert(expiryDate.isBefore(today)?"Đã hết hạn":"Sắp hết hạn",rows.getString(1)+" · "+rows.getNString(2),expiryDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))+" · Còn "+rows.getLong(4),"lots"));
            }}
        }
        return List.copyOf(result);
    }
    Map<LocalDate,Long> visits(Connection c, LocalDate today, String doctorAccount) throws SQLException {
        String sql="""
            SELECT CAST(p.thoiGianTiepNhan AS date),COUNT_BIG(*) FROM dbo.PhieuKham p
            WHERE p.thoiGianTiepNhan>=? AND p.thoiGianTiepNhan<? AND p.trangThai<>'DA_HUY'
            """+(doctorAccount==null?"":" AND EXISTS (SELECT 1 FROM dbo.TaiKhoan a JOIN dbo.NhanVien n ON n.maNhanVien=a.maNhanVien WHERE a.maTaiKhoan=? AND a.maNhanVien=p.maNhanVien AND a.vaiTro='DOCTOR' AND a.trangThai='HOAT_DONG' AND n.chucDanh='BS' AND n.trangThai='DANG_LAM_VIEC')")+" GROUP BY CAST(p.thoiGianTiepNhan AS date)";
        Map<LocalDate,Long> values=new HashMap<>();
        try(PreparedStatement s=prepare(c,sql)) {
            s.setObject(1,today.minusDays(6).atStartOfDay());s.setObject(2,today.plusDays(1).atStartOfDay());if(doctorAccount!=null)s.setString(3,doctorAccount);
            try(ResultSet rows=s.executeQuery()){while(rows.next())values.put(rows.getDate(1).toLocalDate(),rows.getLong(2));}
        }
        return values;
    }
    Map<LocalDate,BigDecimal> collections(Connection c, LocalDate today) throws SQLException {
        Map<LocalDate,BigDecimal> values=new HashMap<>();
        try(PreparedStatement s=prepare(c,"SELECT CAST(thoiGian AS date),SUM(soTien) FROM dbo.DongTienHoaDon WHERE thoiGian>=? AND thoiGian<? GROUP BY CAST(thoiGian AS date)")) {
            s.setObject(1,today.minusDays(6).atStartOfDay());s.setObject(2,today.plusDays(1).atStartOfDay());
            try(ResultSet rows=s.executeQuery()){while(rows.next())values.put(rows.getDate(1).toLocalDate(),rows.getBigDecimal(2));}
        }
        return values;
    }
}
