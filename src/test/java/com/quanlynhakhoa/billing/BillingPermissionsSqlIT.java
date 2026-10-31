package com.quanlynhakhoa.billing;

import com.quanlynhakhoa.billing.dao.*;
import com.quanlynhakhoa.dev.TeamDevConnection;
import java.sql.*;
import java.util.List;

/** Opt-in real application-login permission checks; zero-row writes are always rolled back. */
public final class BillingPermissionsSqlIT {
    private static int checks;
    private static void permission(Connection c,String table,String action,String column,int expected)throws SQLException {
        String sql=column==null?"SELECT HAS_PERMS_BY_NAME(?,'OBJECT',?)":"SELECT HAS_PERMS_BY_NAME(?,'OBJECT',?,?,'COLUMN')";
        try(PreparedStatement s=c.prepareStatement(sql)){
            s.setQueryTimeout(10);s.setString(1,"dbo."+table);s.setString(2,action);if(column!=null)s.setString(3,column);
            try(ResultSet r=s.executeQuery()){
                if(!r.next()||r.getInt(1)!=expected||r.wasNull())throw new SQLException("Unexpected permission: "+table+(column==null?"":"."+column)+" / "+action);
                checks++;
            }
        }
    }
    private static void emptyWrite(Connection c,String sql)throws SQLException {
        try(PreparedStatement s=c.prepareStatement(sql)){s.setQueryTimeout(10);if(s.executeUpdate()!=0)throw new SQLException("Permission probe must not affect business rows");checks++;}
    }
    private static void emptyLock(Connection c,String sql)throws SQLException {
        try(PreparedStatement s=c.prepareStatement(sql)){s.setQueryTimeout(10);try(ResultSet r=s.executeQuery()){if(r.next())throw new SQLException("Permission probe must not return business rows");checks++;}}
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=1||!args[0].equals("--verify")||!TeamDevConnection.hasLocalConfig())throw new IllegalArgumentException("Requires --verify and approved local dev configuration");
        try(Connection c=TeamDevConnection.open()){
            for(String table:List.of("HoaDon","ChiTietHoaDon","ThanhToan","YeuCauHuyHoaDon","ChiTietDieuTri","ChiDinhCanLamSang","PhieuKham","HoSoBenhAn","KeHoachDieuTri","DichVu","Thuoc","VatTu","BenhNhan","TaiKhoan","NhanVien"))permission(c,table,"SELECT",null,1);
            for(String table:List.of("HoaDon","ChiTietHoaDon","ThanhToan","YeuCauHuyHoaDon","NhatKyThaoTac"))permission(c,table,"INSERT",null,1);
            permission(c,"HoaDon","UPDATE","trangThai",1);
            for(String column:List.of("trangThai","maTaiKhoanDuyet","thoiGianDuyet","lyDoDuyet"))permission(c,"YeuCauHuyHoaDon","UPDATE",column,1);
            for(String table:List.of("HoaDon","ChiTietHoaDon","ThanhToan","YeuCauHuyHoaDon","ChiTietDieuTri","ChiDinhCanLamSang")){
                permission(c,table,"DELETE",null,0);permission(c,table,"UPDATE",null,0);permission(c,table,"ALTER",null,0);
            }
            for(String column:List.of("maBenhNhan","ngayLap","maNhanVien"))permission(c,"HoaDon","UPDATE",column,0);
            for(String column:List.of("maHoaDon","lyDo","maTaiKhoanTao"))permission(c,"YeuCauHuyHoaDon","UPDATE",column,0);
            permission(c,"ThanhToan","UPDATE","soTien",0);
            new InvoiceCreateDao().requireWritePermissions(c);new PaymentDao().requirePermissions(c);
            int requests=new CancellationDao().find(c,"",null,null,null).size();
            System.out.println("PASS: real application login can read cancellation screen query; rows="+requests+" (no record details printed)");
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);c.setAutoCommit(false);
            try{
                emptyLock(c,"SELECT maHoaDon FROM dbo.HoaDon WITH (UPDLOCK,HOLDLOCK) WHERE 1=0");
                emptyLock(c,"SELECT maYeuCau FROM dbo.YeuCauHuyHoaDon WITH (UPDLOCK,HOLDLOCK) WHERE 1=0");
                emptyLock(c,"SELECT maChiTiet FROM dbo.ChiTietDieuTri WITH (HOLDLOCK) WHERE 1=0");
                emptyLock(c,"SELECT maChiDinh FROM dbo.ChiDinhCanLamSang WITH (HOLDLOCK) WHERE 1=0");
                emptyWrite(c,"INSERT INTO dbo.HoaDon (maHoaDon,maNhanVien,maBenhNhan,maPhieuKham,ngayLap,trangThai) SELECT maHoaDon,maNhanVien,maBenhNhan,maPhieuKham,ngayLap,trangThai FROM dbo.HoaDon WHERE 1=0");
                emptyWrite(c,"INSERT INTO dbo.ChiTietHoaDon (maHoaDon,maDichVu,soLuong,donGiaApDung) SELECT maHoaDon,maDichVu,soLuong,donGiaApDung FROM dbo.ChiTietHoaDon WHERE 1=0");
                emptyWrite(c,"INSERT INTO dbo.ThanhToan (maThanhToan,maHoaDon,thoiGian,soTien,phuongThuc,maGiaoDich,tienKhachDua,trangThai,maNhanVien) SELECT maThanhToan,maHoaDon,thoiGian,soTien,phuongThuc,maGiaoDich,tienKhachDua,trangThai,maNhanVien FROM dbo.ThanhToan WHERE 1=0");
                emptyWrite(c,"INSERT INTO dbo.YeuCauHuyHoaDon (maYeuCau,maHoaDon,maTaiKhoanTao,thoiGianTao,lyDo,trangThai) SELECT maYeuCau,maHoaDon,maTaiKhoanTao,thoiGianTao,lyDo,trangThai FROM dbo.YeuCauHuyHoaDon WHERE 1=0");
                emptyWrite(c,"UPDATE dbo.HoaDon SET trangThai=trangThai WHERE 1=0");
                emptyWrite(c,"UPDATE dbo.YeuCauHuyHoaDon SET trangThai=trangThai,maTaiKhoanDuyet=maTaiKhoanDuyet,thoiGianDuyet=thoiGianDuyet,lyDoDuyet=lyDoDuyet WHERE 1=0");
            }finally{c.rollback();}
        }
        System.out.println("PASS: BillingPermissionsSqlIT "+checks+" real SQL permission/lock/zero-row-write checks; probes rolled back, no business records created or changed.");
    }
}
