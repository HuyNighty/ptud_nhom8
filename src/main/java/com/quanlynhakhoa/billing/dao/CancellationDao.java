package com.quanlynhakhoa.billing.dao;

import com.quanlynhakhoa.billing.model.YeuCauHuyHoaDon;
import java.sql.*;
import java.time.*;
import java.util.*;

public class CancellationDao {
    private static final String QUERY="""
            SELECT y.maYeuCau,y.maHoaDon,h.maBenhNhan,b.hoTen,
              COALESCE((SELECT SUM(CAST(d.soLuong AS decimal(10,0))*d.donGiaApDung)
                        FROM dbo.ChiTietHoaDon d WHERE d.maHoaDon=y.maHoaDon),0) AS tongTien,
              COALESCE((SELECT SUM(t.soTien) FROM dbo.ThanhToan t
                        WHERE t.maHoaDon=y.maHoaDon AND t.trangThai='THANH_CONG'),0) AS daThu,
              y.maTaiKhoanTao,y.thoiGianTao,y.lyDo,y.trangThai,y.maTaiKhoanDuyet,y.thoiGianDuyet,y.lyDoDuyet
            FROM dbo.YeuCauHuyHoaDon y
            JOIN dbo.HoaDon h ON h.maHoaDon=y.maHoaDon JOIN dbo.BenhNhan b ON b.maBenhNhan=h.maBenhNhan
            """;
    public List<YeuCauHuyHoaDon> find(Connection c,String text,LocalDate from,LocalDate to,YeuCauHuyHoaDon.TrangThai status)throws SQLException {
        try(PreparedStatement s=c.prepareStatement(QUERY+"""
                WHERE (CHARINDEX(?,y.maYeuCau)>0 OR CHARINDEX(?,y.maHoaDon)>0
                   OR CHARINDEX(?,h.maBenhNhan)>0 OR CHARINDEX(?,b.hoTen)>0 OR CHARINDEX(?,y.maTaiKhoanTao)>0)
                AND (? IS NULL OR y.thoiGianTao>=?) AND (? IS NULL OR y.thoiGianTao<?)
                AND (? IS NULL OR y.trangThai=?) ORDER BY y.thoiGianTao DESC,y.maYeuCau
                """)) {
            s.setQueryTimeout(15);for(int i=1;i<=5;i++)s.setNString(i,text);
            time(s,6,from==null?null:from.atStartOfDay());time(s,7,from==null?null:from.atStartOfDay());
            time(s,8,to==null?null:to.plusDays(1).atStartOfDay());time(s,9,to==null?null:to.plusDays(1).atStartOfDay());
            s.setString(10,status==null?null:status.name());s.setString(11,status==null?null:status.name());
            List<YeuCauHuyHoaDon> result=new ArrayList<>();try(ResultSet r=s.executeQuery()){while(r.next())result.add(map(r));}return List.copyOf(result);
        }
    }
    public YeuCauHuyHoaDon get(Connection c,String id)throws SQLException {
        try(PreparedStatement s=c.prepareStatement(QUERY+" WHERE y.maYeuCau=?")) {
            s.setQueryTimeout(15);s.setString(1,id);try(ResultSet r=s.executeQuery()){return r.next()?map(r):null;}
        }
    }
    /** Must finish in auto-commit before acquiring the invoice lock; decisions always lock invoice first. */
    public String invoiceId(Connection c,String id)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT maHoaDon FROM dbo.YeuCauHuyHoaDon WHERE maYeuCau=?")) {
            s.setQueryTimeout(15);s.setString(1,id);try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("Yêu cầu không tồn tại.");return r.getString(1);}
        }
    }
    public void lockInvoice(Connection c,String id)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT maHoaDon FROM dbo.HoaDon WITH (UPDLOCK,HOLDLOCK) WHERE maHoaDon=?")) {
            s.setQueryTimeout(15);s.setString(1,id);try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("Hóa đơn không tồn tại.");}
        }
    }
    public void lockRequest(Connection c,String id)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT maYeuCau FROM dbo.YeuCauHuyHoaDon WITH (UPDLOCK,HOLDLOCK) WHERE maYeuCau=?")) {
            s.setQueryTimeout(15);s.setString(1,id);try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("Yêu cầu không tồn tại.");}
        }
    }
    public boolean pending(Connection c,String invoice)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT maYeuCau FROM dbo.YeuCauHuyHoaDon WITH (HOLDLOCK) WHERE maHoaDon=? AND trangThai='CHO_DUYET'")) {
            s.setQueryTimeout(15);s.setString(1,invoice);try(ResultSet r=s.executeQuery()){return r.next();}
        }
    }
    public void insert(Connection c,String id,String invoice,String account,String reason)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("INSERT INTO dbo.YeuCauHuyHoaDon (maYeuCau,maHoaDon,maTaiKhoanTao,thoiGianTao,lyDo,trangThai) VALUES (?,?,?,CURRENT_TIMESTAMP,?,'CHO_DUYET')")) {
            s.setQueryTimeout(15);s.setString(1,id);s.setString(2,invoice);s.setString(3,account);s.setNString(4,reason);one(s.executeUpdate());
        }
    }
    public void decide(Connection c,String id,String account,String reason,boolean approve)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("UPDATE dbo.YeuCauHuyHoaDon SET trangThai=?,maTaiKhoanDuyet=?,thoiGianDuyet=CURRENT_TIMESTAMP,lyDoDuyet=? WHERE maYeuCau=? AND trangThai='CHO_DUYET' AND maTaiKhoanTao<>?")) {
            s.setQueryTimeout(15);s.setString(1,approve?"DA_DUYET":"TU_CHOI");s.setString(2,account);s.setNString(3,reason);s.setString(4,id);s.setString(5,account);one(s.executeUpdate());
        }
    }
    public void cancelInvoice(Connection c,String id)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("UPDATE dbo.HoaDon SET trangThai='DA_HUY' WHERE maHoaDon=? AND trangThai<>'DA_HUY'")) {
            s.setQueryTimeout(15);s.setString(1,id);one(s.executeUpdate());
        }
    }
    public void audit(Connection c,String account,String id,String action)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("INSERT INTO dbo.NhatKyThaoTac (loaiTacNhan,maTaiKhoan,chucNang,hanhDong,ketQua,doiTuong,maDoiTuong) VALUES ('ACCOUNT',?,N'Hủy hóa đơn',?,'SUCCESS','YeuCauHuyHoaDon',?)")) {
            s.setQueryTimeout(15);s.setString(1,account);s.setNString(2,action);s.setString(3,id);one(s.executeUpdate());
        }
    }
    private static YeuCauHuyHoaDon map(ResultSet r)throws SQLException {
        return new YeuCauHuyHoaDon(r.getString("maYeuCau"),r.getString("maHoaDon"),r.getString("maBenhNhan"),r.getNString("hoTen"),
                r.getBigDecimal("tongTien"),r.getBigDecimal("daThu"),r.getString("maTaiKhoanTao"),date(r,"thoiGianTao"),r.getNString("lyDo"),
                YeuCauHuyHoaDon.TrangThai.valueOf(r.getString("trangThai")),r.getString("maTaiKhoanDuyet"),date(r,"thoiGianDuyet"),r.getNString("lyDoDuyet"));
    }
    private static LocalDateTime date(ResultSet r,String column)throws SQLException{Timestamp value=r.getTimestamp(column);return value==null?null:value.toLocalDateTime();}
    private static void time(PreparedStatement s,int i,LocalDateTime value)throws SQLException{if(value==null)s.setNull(i,Types.TIMESTAMP);else s.setTimestamp(i,Timestamp.valueOf(value));}
    private static void one(int count)throws SQLException{if(count!=1)throw new IllegalArgumentException("Yêu cầu đã thay đổi/được xử lý; tải lại trước khi thao tác.");}
}
