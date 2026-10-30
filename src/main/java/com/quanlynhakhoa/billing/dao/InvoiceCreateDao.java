package com.quanlynhakhoa.billing.dao;

import com.quanlynhakhoa.billing.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** All resources except the caller's connection are owned here. */
public class InvoiceCreateDao {
    public List<BillingChoice> patients(Connection c, String text) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT TOP (100) maBenhNhan, hoTen FROM dbo.BenhNhan WHERE CHARINDEX(?,maBenhNhan)>0 OR CHARINDEX(?,hoTen)>0 OR CHARINDEX(?,soDienThoai)>0 ORDER BY maBenhNhan")) {
            s.setQueryTimeout(15); s.setNString(1,text); s.setNString(2,text);s.setNString(3,text);
            List<BillingChoice> result = new ArrayList<>();
            try (ResultSet r=s.executeQuery()) { while(r.next()) result.add(new BillingChoice(r.getString(1),r.getNString(2))); }
            return List.copyOf(result);
        }
    }
    /** These are candidates only. Clinical eligibility is determined by the approved source. */
    public List<BillingChoice> visits(Connection c, String patient) throws SQLException {
        try (PreparedStatement s=c.prepareStatement("SELECT p.maPhieuKham,p.trangThai,p.thoiGianTiepNhan FROM dbo.PhieuKham p WHERE p.maBenhNhan=? AND p.trangThai<>'DA_HUY' AND NOT EXISTS (SELECT 1 FROM dbo.HoaDon h WHERE h.maPhieuKham=p.maPhieuKham AND h.trangThai<>'DA_HUY') ORDER BY p.thoiGianTiepNhan DESC")) {
            s.setQueryTimeout(15);s.setString(1,patient);
            List<BillingChoice> result=new ArrayList<>();
            try(ResultSet r=s.executeQuery()){while(r.next()){
                Timestamp time=r.getTimestamp(3);String state=r.getString(2);
                String caption=switch(state==null?"":state){case "DA_KET_LUAN" -> "Đã kết luận";case "DANG_KHAM" -> "Đang khám";case "CHO_KHAM" -> "Chờ khám";default -> "Cần kiểm tra dịch vụ tính phí";};
                result.add(new BillingChoice(r.getString(1),(time==null?"":time.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm"))+" · ")+caption));
            }}
            return List.copyOf(result);
        }
    }
    public void requireWritePermissions(Connection c) throws SQLException {
        for(String table:List.of("HoaDon","ChiTietHoaDon","NhatKyThaoTac")) {
            try(PreparedStatement s=c.prepareStatement("SELECT HAS_PERMS_BY_NAME(?, 'OBJECT', 'INSERT')")) {
                s.setQueryTimeout(15);s.setString(1,"dbo."+table);
                try(ResultSet r=s.executeQuery()) { if(!r.next() || r.getInt(1)!=1) throw new SQLException("Chưa được duyệt quyền INSERT " + table,"42000",229); }
            }
        }
    }
    public String employee(Connection c,String account) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT n.maNhanVien FROM dbo.TaiKhoan t WITH (HOLDLOCK) JOIN dbo.NhanVien n WITH (HOLDLOCK) ON n.maNhanVien=t.maNhanVien WHERE t.maTaiKhoan=? AND t.trangThai='HOAT_DONG' AND n.trangThai='DANG_LAM_VIEC'")) {
            s.setQueryTimeout(15);s.setString(1,account);
            try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("Tài khoản chưa gắn nhân viên đang làm việc.");return r.getString(1);}
        }
    }
    public void validateVisit(Connection c,String patient,String visit) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT p.trangThai FROM dbo.PhieuKham p WITH (UPDLOCK,HOLDLOCK) JOIN dbo.BenhNhan b WITH (HOLDLOCK) ON b.maBenhNhan=p.maBenhNhan WHERE p.maPhieuKham=? AND b.maBenhNhan=?")) {
            s.setQueryTimeout(15);s.setString(1,visit);s.setString(2,patient);
            try(ResultSet r=s.executeQuery()){if(!r.next() || "DA_HUY".equals(r.getString(1)))throw new IllegalArgumentException("Bệnh nhân/lượt khám không hợp lệ hoặc không thuộc nhau.");}
        }
        try(PreparedStatement s=c.prepareStatement("SELECT maHoaDon FROM dbo.HoaDon WITH (UPDLOCK,HOLDLOCK) WHERE maPhieuKham=? AND trangThai<>'DA_HUY'")) {
            s.setQueryTimeout(15);s.setString(1,visit);
            try(ResultSet r=s.executeQuery()){if(r.next())throw new IllegalArgumentException("Lượt khám đã có hóa đơn chưa hủy.");}
        }
    }
    public void insertHeader(Connection c,String id,String patient,String visit,String employee) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("INSERT INTO dbo.HoaDon (maHoaDon,maNhanVien,maBenhNhan,maPhieuKham,ngayLap,trangThai) VALUES (?,?,?,?,CURRENT_TIMESTAMP,'CHUA_THANH_TOAN')")) {
            s.setQueryTimeout(15);s.setString(1,id);s.setString(2,employee);s.setString(3,patient);s.setString(4,visit);one(s.executeUpdate());
        }
    }
    public boolean serviceActive(Connection c,String id) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT dangApDung FROM dbo.DichVu WITH (HOLDLOCK) WHERE maDichVu=?")) {
            s.setQueryTimeout(15);s.setString(1,id);
            try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("Dịch vụ tính phí không tồn tại.");return r.getBoolean(1);}
        }
    }
    public void insertLine(Connection c,String id,InvoiceDraft.Line line) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("INSERT INTO dbo.ChiTietHoaDon (maHoaDon,maDichVu,soLuong,donGiaApDung) VALUES (?,?,?,?)")) {
            s.setQueryTimeout(15);s.setString(1,id);s.setString(2,line.serviceId());s.setInt(3,line.quantity());s.setBigDecimal(4,line.unitPrice());one(s.executeUpdate());
        }
    }
    public void audit(Connection c,String account,String id) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("INSERT INTO dbo.NhatKyThaoTac (loaiTacNhan,maTaiKhoan,chucNang,hanhDong,ketQua,doiTuong,maDoiTuong) VALUES ('ACCOUNT',?,N'Hóa đơn',N'Tạo hóa đơn','SUCCESS','HoaDon',?)")) {
            s.setQueryTimeout(15);s.setString(1,account);s.setString(2,id);one(s.executeUpdate());
        }
    }
    private static void one(int count) throws SQLException {if(count!=1)throw new SQLException("Cần đúng một bản ghi.");}
}
