package com.quanlynhakhoa.billing.dao;

import com.quanlynhakhoa.billing.model.*;
import java.sql.*;
import java.util.List;

public class PaymentDao {
    public void requirePermissions(Connection c) throws SQLException {
        String[][] required={{"ThanhToan","INSERT"},{"NhatKyThaoTac","INSERT"}};
        for(String[] permission:required)try(PreparedStatement s=c.prepareStatement("SELECT HAS_PERMS_BY_NAME(?,'OBJECT',?)")) {
            s.setQueryTimeout(15);s.setString(1,"dbo."+permission[0]);s.setString(2,permission[1]);
            try(ResultSet r=s.executeQuery()){if(!r.next()||r.getInt(1)!=1)throw new SQLException("Thiếu quyền "+permission[1]+" "+permission[0],"42000",229);}
        }
    }
    public void lockInvoice(Connection c,String id)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT HAS_PERMS_BY_NAME('dbo.HoaDon','OBJECT','UPDATE','trangThai','COLUMN')")) {
            s.setQueryTimeout(15);try(ResultSet r=s.executeQuery()){if(!r.next()||r.getInt(1)!=1)throw new SQLException("Thiếu quyền UPDATE HoaDon.trangThai","42000",229);}
        }
        try(PreparedStatement s=c.prepareStatement("SELECT maHoaDon FROM dbo.HoaDon WITH (UPDLOCK,HOLDLOCK) WHERE maHoaDon=?")) {
            s.setQueryTimeout(15);s.setString(1,id);
            try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("Hóa đơn không tồn tại.");}
        }
    }
    /** Returns existing row data including cancelled rows: a cancelled ID cannot be used to collect again. */
    public ThanhToan existing(Connection c,String id)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("""
                SELECT maThanhToan,maHoaDon,thoiGian,soTien,phuongThuc,maGiaoDich,tienKhachDua,
                       trangThai,maNhanVien,maTaiKhoanHuy,thoiGianHuy,lyDoHuy
                FROM dbo.ThanhToan WITH (HOLDLOCK) WHERE maThanhToan=?
                """)) {
            s.setQueryTimeout(15);s.setString(1,id);
            try(ResultSet r=s.executeQuery()) {
                if(!r.next())return null;
                Timestamp cancelled=r.getTimestamp("thoiGianHuy");
                return new ThanhToan(r.getString("maThanhToan"),r.getString("maHoaDon"),r.getTimestamp("thoiGian").toLocalDateTime(),
                        r.getBigDecimal("soTien"),ThanhToan.PhuongThuc.valueOf(r.getString("phuongThuc")),r.getString("maGiaoDich"),r.getBigDecimal("tienKhachDua"),
                        ThanhToan.TrangThai.valueOf(r.getString("trangThai")),r.getString("maNhanVien"),r.getString("maTaiKhoanHuy"),
                        cancelled==null?null:cancelled.toLocalDateTime(),r.getNString("lyDoHuy"));
            }
        }
    }
    public void requireNewTransferReference(Connection c,String reference)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT maThanhToan FROM dbo.ThanhToan WITH (UPDLOCK,HOLDLOCK) WHERE phuongThuc='CHUYEN_KHOAN' AND maGiaoDich=? AND trangThai='THANH_CONG'")) {
            s.setQueryTimeout(15);s.setString(1,reference);
            try(ResultSet r=s.executeQuery()){if(r.next())throw new IllegalArgumentException("Mã giao dịch chuyển khoản đã được ghi nhận.");}
        }
    }
    public void requireNoPendingCancellation(Connection c,String id)throws SQLException {
        if(hasPendingCancellation(c,id))throw new IllegalArgumentException("Hóa đơn đang chờ duyệt hủy, chưa được thu tiền.");
    }
    public boolean hasPendingCancellation(Connection c,String id)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT maYeuCau FROM dbo.YeuCauHuyHoaDon WITH (HOLDLOCK) WHERE maHoaDon=? AND trangThai='CHO_DUYET'")) {
            s.setQueryTimeout(15);s.setString(1,id);
            try(ResultSet r=s.executeQuery()){return r.next();}
        }
    }
    public void insert(Connection c,String invoice,String employee,PaymentRequest.Part part)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("""
                INSERT INTO dbo.ThanhToan
                (maThanhToan,maHoaDon,thoiGian,soTien,phuongThuc,maGiaoDich,tienKhachDua,trangThai,maNhanVien)
                VALUES (?,?,CURRENT_TIMESTAMP,?,?,?,?,'THANH_CONG',?)
                """)) {
            s.setQueryTimeout(15);s.setString(1,part.id());s.setString(2,invoice);s.setBigDecimal(3,part.amount());s.setString(4,part.method().name());
            if(part.reference()==null)s.setNull(5,Types.VARCHAR);else s.setString(5,part.reference());
            if(part.customerTender()==null)s.setNull(6,Types.DECIMAL);else s.setBigDecimal(6,part.customerTender());
            s.setString(7,employee);one(s.executeUpdate());
        }
    }
    public void updateStatus(Connection c,String id,TrangThaiHoaDon status)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("UPDATE dbo.HoaDon SET trangThai=? WHERE maHoaDon=? AND trangThai<>'DA_HUY'")) {
            s.setQueryTimeout(15);s.setString(1,status.name());s.setString(2,id);one(s.executeUpdate());
        }
    }
    public void audit(Connection c,String account,String id)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("INSERT INTO dbo.NhatKyThaoTac (loaiTacNhan,maTaiKhoan,chucNang,hanhDong,ketQua,doiTuong,maDoiTuong) VALUES ('ACCOUNT',?,N'Thanh toán',N'Ghi nhận thu tiền','SUCCESS','HoaDon',?)")) {
            s.setQueryTimeout(15);s.setString(1,account);s.setString(2,id);one(s.executeUpdate());
        }
    }
    private static void one(int count)throws SQLException{if(count!=1)throw new SQLException("Cần đúng một bản ghi.");}
}
