package com.quanlynhakhoa.dao.hoadon;

import com.quanlynhakhoa.model.hoadon.*;
import com.quanlynhakhoa.enums.PhuongThucThanhToan;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;

/** SQL bằng connection của Service; không mở connection hay kết thúc transaction. */
public final class LanThanhToanDao {
    private static PreparedStatement statement(Connection c,String sql)throws SQLException{PreparedStatement s=c.prepareStatement(sql);s.setQueryTimeout(15);return s;}
    public ThanhToanDangCho pending(Connection c,String invoice)throws SQLException {
        try(PreparedStatement s=statement(c,"SELECT maLanThu,maHoaDon,maTaiKhoan,soTien,phuongThuc,maGiaoDich,tienKhachDua FROM dbo.LanThuHoaDon WHERE maHoaDon=? AND trangThai='CHO_XU_LY'")){
            s.setString(1,invoice);try(ResultSet r=s.executeQuery()){
                if(!r.next())return null;
                var method=PhuongThucThanhToan.valueOf(r.getString("phuongThuc"));
                var part=new PhanThanhToan(r.getString("maLanThu"),r.getBigDecimal("soTien"),method,r.getString("maGiaoDich"),r.getBigDecimal("tienKhachDua"));
                return new ThanhToanDangCho(new YeuCauThanhToan(invoice,part.amount(),List.of(part),method==PhuongThucThanhToan.CHUYEN_KHOAN),r.getString("maTaiKhoan"));
            }
        }
    }
    @FunctionalInterface public interface XuLyLanThu { void xuLy(ThongTinLanThuDaKhoa lanThu) throws SQLException; }

    /** Callback retains the original commit-before-resource-close order for an existing reservation. */
    public boolean voiLanThuDaKhoa(Connection c,String maLanThu,XuLyLanThu xuLy)throws SQLException {
        try(PreparedStatement s=statement(c,"SELECT maHoaDon,maTaiKhoan,soTien,phuongThuc,maGiaoDich,tienKhachDua,trangThai FROM dbo.LanThuHoaDon WITH (UPDLOCK,HOLDLOCK) WHERE maLanThu=?")) {
            s.setString(1,maLanThu);
            try(ResultSet r=s.executeQuery()) {
                if(!r.next())return false;
                xuLy.xuLy(new ThongTinLanThuDaKhoa(r.getString(1),r.getString(2),r.getBigDecimal(3),
                        r.getString(4),r.getString(5),r.getBigDecimal(6),r.getString(7)));
                return true;
            }
        }
    }

    public void taoLanThu(Connection c,YeuCauThanhToan request,String account,String trangThai)throws SQLException {
        var part=request.parts().getFirst();
                try(PreparedStatement s=statement(c,"INSERT INTO dbo.LanThuHoaDon(maLanThu,maHoaDon,maTaiKhoan,soTien,phuongThuc,maGiaoDich,tienKhachDua,trangThai) VALUES(?,?,?,?,?,?,?,?)")){
                    s.setString(1,part.id());s.setString(2,request.invoiceId());s.setString(3,account);s.setBigDecimal(4,part.amount());s.setString(5,part.method().name());s.setString(6,part.reference());s.setBigDecimal(7,part.customerTender());s.setString(8,trangThai);s.executeUpdate();
                }
    }

    public void kiemTraLanThuConHieuLuc(Connection c,YeuCauThanhToan request,String account)throws SQLException {
        try(PreparedStatement s=statement(c,"SELECT maLanThu FROM dbo.LanThuHoaDon WITH (UPDLOCK,HOLDLOCK) WHERE maLanThu=? AND maHoaDon=? AND maTaiKhoan=? AND trangThai IN ('CHO_XU_LY','THANH_CONG')")){
            s.setString(1,request.parts().getFirst().id());s.setString(2,request.invoiceId());s.setString(3,account);
            try(ResultSet rows=s.executeQuery()){if(!rows.next())throw new IllegalArgumentException("Lần thu đã bị giải quyết; không được gửi lại.");}
        }
    }

    public void xacNhanHoanTat(Connection c,YeuCauThanhToan request,String account)throws SQLException {
                try(PreparedStatement s=statement(c,"UPDATE dbo.LanThuHoaDon SET trangThai='THANH_CONG' WHERE maLanThu=? AND maHoaDon=? AND maTaiKhoan=? AND trangThai IN ('CHO_XU_LY','THANH_CONG')")){
                    s.setString(1,request.parts().getFirst().id());s.setString(2,request.invoiceId());s.setString(3,account);
                    if(s.executeUpdate()!=1)throw new IllegalArgumentException("Lần thu không thuộc tài khoản này hoặc đã giải quyết.");
                }
    }

    public void khoaHoaDon(Connection c,String invoice)throws SQLException {
                try(PreparedStatement lock=statement(c,"SELECT maHoaDon FROM dbo.HoaDon WITH (UPDLOCK,HOLDLOCK) WHERE maHoaDon=?")){lock.setString(1,invoice);try(ResultSet r=lock.executeQuery()){if(!r.next())throw new IllegalArgumentException("Không có hóa đơn.");}}
    }

    public void ketThucDoiSoat(Connection c,String invoice,String attempt,String reason,String account,String trangThai)throws SQLException {
                try(PreparedStatement s=statement(c,"UPDATE dbo.LanThuHoaDon SET trangThai=?,maTaiKhoanXuLy=?,lyDoXuLy=? WHERE maLanThu=? AND maHoaDon=? AND trangThai='CHO_XU_LY'")){
                    s.setString(1,trangThai);s.setString(2,account);s.setNString(3,reason.trim());s.setString(4,attempt);s.setString(5,invoice);if(s.executeUpdate()!=1)throw new IllegalArgumentException("Lần thu đã được xử lý.");
                }
    }

    public void ghiNhatKyDoiSoat(Connection c,String account,String attempt)throws SQLException {
                try(PreparedStatement s=statement(c,"INSERT INTO dbo.NhatKyThaoTac(loaiTacNhan,maTaiKhoan,chucNang,hanhDong,ketQua,doiTuong,maDoiTuong) VALUES('ACCOUNT',?,N'Thanh toán',N'Đối soát lần thu','SUCCESS','LanThuHoaDon',?)")){
                    s.setString(1,account);s.setString(2,attempt);s.executeUpdate();
                }
    }
}
