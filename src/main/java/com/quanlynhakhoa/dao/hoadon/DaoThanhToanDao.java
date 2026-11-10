package com.quanlynhakhoa.dao.hoadon;

import com.quanlynhakhoa.model.hoadon.*;
import com.quanlynhakhoa.enums.PhuongThucThanhToan;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;

/** SQL bằng connection của Service; không mở connection hay kết thúc transaction. */
public final class DaoThanhToanDao {
    private static PreparedStatement statement(Connection c,String sql)throws SQLException{PreparedStatement s=c.prepareStatement(sql);s.setQueryTimeout(15);return s;}
    public void khoaHoaDon(Connection c,String invoiceId)throws SQLException {
                try(PreparedStatement s=statement(c,"SELECT maHoaDon FROM dbo.HoaDon WITH (UPDLOCK,HOLDLOCK) WHERE maHoaDon=?")){s.setString(1,invoiceId);try(ResultSet rows=s.executeQuery()){if(!rows.next())throw new IllegalArgumentException("Không có hóa đơn.");}}
    }

    public Timestamp thoiGianHienTai(Connection c)throws SQLException {
                try(PreparedStatement s=statement(c,"SELECT CURRENT_TIMESTAMP");ResultSet rows=s.executeQuery()){rows.next();return rows.getTimestamp(1);}
    }

    public void taoDieuChinh(Connection c,String paymentId,String loai,BigDecimal soTien,Timestamp when,String account,String reason,String reference)throws SQLException {
                try(PreparedStatement s=statement(c,"INSERT INTO dbo.DieuChinhThanhToan(maThanhToan,loai,soTien,thoiGian,maTaiKhoan,lyDo,thamChieu) VALUES(?,?,?,?,?,?,?)")){
                    s.setString(1,paymentId);s.setString(2,loai);s.setBigDecimal(3,soTien);s.setTimestamp(4,when);s.setString(5,account);s.setNString(6,reason.trim());s.setNString(7,reference.trim());if(s.executeUpdate()!=1)throw new SQLException("Adjustment not saved");
                }
    }

    public void capNhatKhoanThu(Connection c,String paymentId,String trangThai,Timestamp when,String account,String reason)throws SQLException {
                try(PreparedStatement s=statement(c,"UPDATE dbo.ThanhToan SET trangThai=?,maTaiKhoanHuy=?,thoiGianHuy=?,lyDoHuy=? WHERE maThanhToan=? AND trangThai='THANH_CONG'")){
                    s.setString(1,trangThai);s.setString(2,account);s.setTimestamp(3,when);s.setNString(4,reason.trim());s.setString(5,paymentId);if(s.executeUpdate()!=1)throw new SQLException("Payment changed");
                }
    }

    public void ghiNhatKy(Connection c,String paymentId,String account,String hanhDong)throws SQLException {
                try(PreparedStatement s=statement(c,"INSERT INTO dbo.NhatKyThaoTac(loaiTacNhan,maTaiKhoan,chucNang,hanhDong,ketQua,doiTuong,maDoiTuong) VALUES('ACCOUNT',?,N'Thanh toán',?,'SUCCESS','ThanhToan',?)")){
                    s.setString(1,account);s.setNString(2,hanhDong);s.setString(3,paymentId);s.executeUpdate();
                }
    }
}
