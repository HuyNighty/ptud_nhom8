package com.quanlynhakhoa.nghiepvu.hoadon.service;

import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.enums.TrangThaiThanhToan;
import com.quanlynhakhoa.enums.TrangThaiHoaDon;
import com.quanlynhakhoa.enums.PhuongThucThanhToan;
import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import com.quanlynhakhoa.model.hoadon.YeuCauThanhToan;
import com.quanlynhakhoa.model.hoadon.ThongTinThanhToan;
import com.quanlynhakhoa.model.hoadon.ThanhToanDangCho;
import com.quanlynhakhoa.model.hoadon.PhanThanhToan;
import com.quanlynhakhoa.nghiepvu.hoadon.dao.*;
import com.quanlynhakhoa.hethong.phanquyen.*;
import java.sql.*;
import java.util.*;

/** A durable, server-side reservation survives a lost response and is visible on every workstation. */
public final class PaymentAttemptService {

    private final DesktopSession session;private final ConnectionProvider connections;
    private final AuthorizationService auth=new AuthorizationService(new RolePermissionPolicy());
    public PaymentAttemptService(DesktopSession session,ConnectionProvider connections){this.session=session;this.connections=connections;}
    private static PreparedStatement statement(Connection c,String sql)throws SQLException{PreparedStatement s=c.prepareStatement(sql);s.setQueryTimeout(15);return s;}
    public ThanhToanDangCho pending(String invoice)throws SQLException {
        auth.require(session,Quyen.BILLING_READ);
        try(Connection c=connections.open()){return pending(c,invoice);}
    }
    static ThanhToanDangCho pending(Connection c,String invoice)throws SQLException {
        try(PreparedStatement s=statement(c,"SELECT maLanThu,maHoaDon,maTaiKhoan,soTien,phuongThuc,maGiaoDich,tienKhachDua FROM dbo.LanThuHoaDon WHERE maHoaDon=? AND trangThai='CHO_XU_LY'")){
            s.setString(1,invoice);try(ResultSet r=s.executeQuery()){
                if(!r.next())return null;
                var method=PhuongThucThanhToan.valueOf(r.getString("phuongThuc"));
                var part=new PhanThanhToan(r.getString("maLanThu"),r.getBigDecimal("soTien"),method,r.getString("maGiaoDich"),r.getBigDecimal("tienKhachDua"));
                return new ThanhToanDangCho(new YeuCauThanhToan(invoice,part.amount(),List.of(part),method==PhuongThucThanhToan.CHUYEN_KHOAN),r.getString("maTaiKhoan"));
            }
        }
    }
    public void reserve(YeuCauThanhToan request)throws SQLException {
        auth.require(session,Quyen.BILLING_MANAGE);PaymentService.validate(request);
        try(Connection c=connections.open()){
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);c.setAutoCommit(false);
            try{
                new PaymentDao().lockInvoice(c,request.invoiceId());
                try(PreparedStatement s=statement(c,"SELECT maHoaDon,maTaiKhoan,soTien,phuongThuc,maGiaoDich,tienKhachDua,trangThai FROM dbo.LanThuHoaDon WITH (UPDLOCK,HOLDLOCK) WHERE maLanThu=?")){
                    var part=request.parts().getFirst();s.setString(1,part.id());try(ResultSet r=s.executeQuery()){
                        if(r.next()){
                            if(!request.invoiceId().equals(r.getString(1))||!session.accountId().equals(r.getString(2))||part.amount().compareTo(r.getBigDecimal(3))!=0||!part.method().name().equals(r.getString(4))||!Objects.equals(part.reference(),r.getString(5))||!equal(part.customerTender(),r.getBigDecimal(6))||"DA_HUY".equals(r.getString(7)))
                                throw new IllegalArgumentException("Lần thu thuộc tài khoản khác, đã được giải quyết hoặc có dữ liệu khác. Kiểm tra kết quả trước khi thu tiếp.");
                            auth.requireCurrent(session,Quyen.BILLING_MANAGE,c);c.commit();return;
                        }
                    }
                }
                var prior=new PaymentDao().existing(c,request.parts().getFirst().id());
                boolean recorded=prior!=null;
                if(recorded&&!PaymentService.matches(prior,request.invoiceId(),new InvoiceCreateDao().employee(c,session.accountId()),request.parts().getFirst()))
                    throw new IllegalArgumentException("Mã lần thu đã có giao dịch khác hoặc đã điều chỉnh.");
                if(!recorded&&pending(c,request.invoiceId())!=null)throw new IllegalArgumentException("Hóa đơn đang có lần thu chờ đối soát trên máy khác. Bấm Kiểm tra để tải lần thu đó.");
                ThongTinHoaDon invoice=new InvoiceDao().getForPayment(c,request.invoiceId());
                if(!recorded&&(invoice==null||invoice.trangThai()==TrangThaiHoaDon.DA_HUY||!invoice.canhBao().isEmpty()||request.amount().compareTo(invoice.conNo())>0))throw new IllegalArgumentException("Hóa đơn không còn đủ số nợ cho lần thu này.");
                if(!recorded)new PaymentDao().requireNoPendingCancellation(c,request.invoiceId());
                var part=request.parts().getFirst();
                try(PreparedStatement s=statement(c,"INSERT INTO dbo.LanThuHoaDon(maLanThu,maHoaDon,maTaiKhoan,soTien,phuongThuc,maGiaoDich,tienKhachDua,trangThai) VALUES(?,?,?,?,?,?,?,?)")){
                    s.setString(1,part.id());s.setString(2,request.invoiceId());s.setString(3,session.accountId());s.setBigDecimal(4,part.amount());s.setString(5,part.method().name());s.setString(6,part.reference());s.setBigDecimal(7,part.customerTender());s.setString(8,recorded?"THANH_CONG":"CHO_XU_LY");s.executeUpdate();
                }
                auth.requireCurrent(session,Quyen.BILLING_MANAGE,c);c.commit();
            }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}throw e;}
        }
    }
    static void requireAndComplete(Connection c,YeuCauThanhToan request,String account)throws SQLException {
        // Keep the reservation until the client has received the commit response.
        try(PreparedStatement s=statement(c,"SELECT maLanThu FROM dbo.LanThuHoaDon WITH (UPDLOCK,HOLDLOCK) WHERE maLanThu=? AND maHoaDon=? AND maTaiKhoan=? AND trangThai IN ('CHO_XU_LY','THANH_CONG')")){
            s.setString(1,request.parts().getFirst().id());s.setString(2,request.invoiceId());s.setString(3,account);
            try(ResultSet rows=s.executeQuery()){if(!rows.next())throw new IllegalArgumentException("Lần thu đã bị giải quyết; không được gửi lại.");}
        }
    }
    public void acknowledge(YeuCauThanhToan request)throws SQLException {
        auth.require(session,Quyen.BILLING_MANAGE);
        try(Connection c=connections.open()){
            c.setAutoCommit(false);
            try{
                ThongTinThanhToan payment=new PaymentDao().existing(c,request.parts().getFirst().id());
                if(payment==null||!PaymentService.matches(payment,request.invoiceId(),new InvoiceCreateDao().employee(c,session.accountId()),request.parts().getFirst()))throw new IllegalArgumentException("Chưa có giao dịch khớp để kết thúc đối soát.");
                try(PreparedStatement s=statement(c,"UPDATE dbo.LanThuHoaDon SET trangThai='THANH_CONG' WHERE maLanThu=? AND maHoaDon=? AND maTaiKhoan=? AND trangThai IN ('CHO_XU_LY','THANH_CONG')")){
                    s.setString(1,request.parts().getFirst().id());s.setString(2,request.invoiceId());s.setString(3,session.accountId());
                    if(s.executeUpdate()!=1)throw new IllegalArgumentException("Lần thu không thuộc tài khoản này hoặc đã giải quyết.");
                }
                auth.requireCurrent(session,Quyen.BILLING_MANAGE,c);c.commit();
            }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}throw e;}
        }
    }
    public void release(String invoice,String attempt,String reason)throws SQLException {
        auth.require(session,Quyen.INVOICE_CANCEL_APPROVE);
        if(reason==null||reason.trim().isEmpty()||reason.length()>300)throw new IllegalArgumentException("Nhập lý do đối soát tối đa 300 ký tự.");
        try(Connection c=connections.open()){
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);c.setAutoCommit(false);
            try{
                // Managers may lack payment write permissions, so take the same invoice lock directly.
                try(PreparedStatement lock=statement(c,"SELECT maHoaDon FROM dbo.HoaDon WITH (UPDLOCK,HOLDLOCK) WHERE maHoaDon=?")){lock.setString(1,invoice);try(ResultSet r=lock.executeQuery()){if(!r.next())throw new IllegalArgumentException("Không có hóa đơn.");}}
                ThongTinThanhToan recorded=new PaymentDao().existing(c,attempt);
                ThanhToanDangCho pending=pending(c,invoice);
                if(pending==null||!pending.request().parts().getFirst().id().equals(attempt))throw new IllegalArgumentException("Lần thu đã được xử lý.");
                if(recorded!=null){
                    var part=pending.request().parts().getFirst();
                    if(!invoice.equals(recorded.maHoaDon())||recorded.trangThai()!=TrangThaiThanhToan.THANH_CONG||recorded.soTien().compareTo(part.amount())!=0||recorded.phuongThuc()!=part.method()||!Objects.equals(recorded.maGiaoDich(),part.reference())||!equal(recorded.tienKhachDua(),part.customerTender()))throw new IllegalArgumentException("Giao dịch không khớp lần thu; cần kiểm tra thủ công.");
                }
                try(PreparedStatement s=statement(c,"UPDATE dbo.LanThuHoaDon SET trangThai=?,maTaiKhoanXuLy=?,lyDoXuLy=? WHERE maLanThu=? AND maHoaDon=? AND trangThai='CHO_XU_LY'")){
                    s.setString(1,recorded==null?"DA_HUY":"THANH_CONG");s.setString(2,session.accountId());s.setNString(3,reason.trim());s.setString(4,attempt);s.setString(5,invoice);if(s.executeUpdate()!=1)throw new IllegalArgumentException("Lần thu đã được xử lý.");
                }
                try(PreparedStatement s=statement(c,"INSERT INTO dbo.NhatKyThaoTac(loaiTacNhan,maTaiKhoan,chucNang,hanhDong,ketQua,doiTuong,maDoiTuong) VALUES('ACCOUNT',?,N'Thanh toán',N'Đối soát lần thu','SUCCESS','LanThuHoaDon',?)")){
                    s.setString(1,session.accountId());s.setString(2,attempt);s.executeUpdate();
                }
                auth.requireCurrent(session,Quyen.INVOICE_CANCEL_APPROVE,c);c.commit();
            }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}throw e;}
        }
    }
    private static boolean equal(java.math.BigDecimal a,java.math.BigDecimal b){return a==null?b==null:b!=null&&a.compareTo(b)==0;}
}
