package com.quanlynhakhoa.nghiepvu.hoadon.service;

import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import com.quanlynhakhoa.model.hoadon.ThongTinThanhToan;
import com.quanlynhakhoa.nghiepvu.hoadon.dao.*;
import com.quanlynhakhoa.nghiepvu.hoadon.entity.*;
import com.quanlynhakhoa.hethong.phanquyen.*;
import java.sql.*;
import java.math.BigDecimal;

/** Whole-payment correction/refund; immutable adjustment row plus original transaction history. */
public final class PaymentReversalService {
    public enum Kind { HUY_GHI_NHAN, HOAN_TIEN }
    private final DesktopSession session;private final ConnectionProvider connections;
    private final AuthorizationService auth=new AuthorizationService(new RolePermissionPolicy());
    public PaymentReversalService(DesktopSession session,ConnectionProvider connections){this.session=session;this.connections=connections;}
    private PreparedStatement statement(Connection c,String sql)throws SQLException{PreparedStatement s=c.prepareStatement(sql);s.setQueryTimeout(15);return s;}
    public void reverse(String invoiceId,String paymentId,Kind kind,String reason,String reference)throws SQLException {
        auth.require(session,Permission.INVOICE_CANCEL_APPROVE);
        if(kind==null||reason==null||reason.isBlank()||reason.length()>300||reference==null||reference.isBlank()||reference.length()>100)throw new IllegalArgumentException("Cần lý do (tối đa 300 ký tự) và bằng chứng/tham chiếu (tối đa 100 ký tự).");
        boolean committed=false;
        try(Connection c=connections.open()){
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);c.setAutoCommit(false);
            try{
                try(PreparedStatement s=statement(c,"SELECT maHoaDon FROM dbo.HoaDon WITH (UPDLOCK,HOLDLOCK) WHERE maHoaDon=?")){s.setString(1,invoiceId);try(ResultSet rows=s.executeQuery()){if(!rows.next())throw new IllegalArgumentException("Không có hóa đơn.");}}
                if(PaymentAttemptService.pending(c,invoiceId)!=null)throw new IllegalArgumentException("Còn lần thu chưa đối soát. Giải quyết lần thu trước khi điều chỉnh.");
                ThongTinThanhToan payment=new PaymentDao().existing(c,paymentId);
                if(payment==null||!invoiceId.equals(payment.maHoaDon())||payment.trangThai()!=ThongTinThanhToan.TrangThai.THANH_CONG)throw new IllegalArgumentException("Khoản thu không thuộc hóa đơn hoặc đã được điều chỉnh. Tải lại để kiểm tra.");
                ThongTinHoaDon invoice=new InvoiceDao().getForPayment(c,invoiceId);
                if(invoice.trangThai()==InvoiceStatus.DA_HUY)throw new IllegalArgumentException("Hóa đơn đã hủy; cần đối soát.");
                Timestamp when;
                try(PreparedStatement s=statement(c,"SELECT CURRENT_TIMESTAMP");ResultSet rows=s.executeQuery()){rows.next();when=rows.getTimestamp(1);}
                try(PreparedStatement s=statement(c,"INSERT INTO dbo.DieuChinhThanhToan(maThanhToan,loai,soTien,thoiGian,maTaiKhoan,lyDo,thamChieu) VALUES(?,?,?,?,?,?,?)")){
                    s.setString(1,paymentId);s.setString(2,kind.name());s.setBigDecimal(3,payment.soTien());s.setTimestamp(4,when);s.setString(5,session.accountId());s.setNString(6,reason.trim());s.setNString(7,reference.trim());if(s.executeUpdate()!=1)throw new SQLException("Adjustment not saved");
                }
                try(PreparedStatement s=statement(c,"UPDATE dbo.ThanhToan SET trangThai=?,maTaiKhoanHuy=?,thoiGianHuy=?,lyDoHuy=? WHERE maThanhToan=? AND trangThai='THANH_CONG'")){
                    s.setString(1,kind==Kind.HOAN_TIEN?"DA_HOAN_TIEN":"DA_HUY");s.setString(2,session.accountId());s.setTimestamp(3,when);s.setNString(4,reason.trim());s.setString(5,paymentId);if(s.executeUpdate()!=1)throw new SQLException("Payment changed");
                }
                BigDecimal net=invoice.daThu().subtract(payment.soTien());
                InvoiceStatus status=net.compareTo(invoice.tongTien())==0?InvoiceStatus.DA_THANH_TOAN:net.signum()==0?InvoiceStatus.CHUA_THANH_TOAN:InvoiceStatus.THANH_TOAN_MOT_PHAN;
                new PaymentDao().updateStatus(c,invoiceId,status);
                try(PreparedStatement s=statement(c,"INSERT INTO dbo.NhatKyThaoTac(loaiTacNhan,maTaiKhoan,chucNang,hanhDong,ketQua,doiTuong,maDoiTuong) VALUES('ACCOUNT',?,N'Thanh toán',?,'SUCCESS','ThanhToan',?)")){
                    s.setString(1,session.accountId());s.setNString(2,kind==Kind.HOAN_TIEN?"Hoàn tiền":"Hủy ghi nhận thu sai");s.setString(3,paymentId);s.executeUpdate();
                }
                auth.requireCurrent(session,Permission.INVOICE_CANCEL_APPROVE,c);auth.require(session,Permission.INVOICE_CANCEL_APPROVE);c.commit();committed=true;
            }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}throw e;}
        }catch(SQLException e){if(committed)throw new CommittedReversalException(e);throw e;}
    }
    public static final class CommittedReversalException extends SQLException { public CommittedReversalException(SQLException cause){super("Điều chỉnh đã lưu; tải lại lịch sử để kiểm tra.",cause);} }
}
