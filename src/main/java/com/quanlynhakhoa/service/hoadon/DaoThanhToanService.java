package com.quanlynhakhoa.service.hoadon;

import com.quanlynhakhoa.security.RolePermissionPolicy;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.dao.hoadon.ThanhToanDao;
import com.quanlynhakhoa.dao.hoadon.HoaDonDao;
import com.quanlynhakhoa.config.ConnectionProvider;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.enums.TrangThaiThanhToan;
import com.quanlynhakhoa.enums.TrangThaiHoaDon;
import com.quanlynhakhoa.enums.LoaiDaoThanhToan;
import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import com.quanlynhakhoa.model.hoadon.ThongTinThanhToan;
import com.quanlynhakhoa.dao.hoadon.DaoThanhToanDao;
import java.sql.*;
import java.math.BigDecimal;

/** Whole-payment correction/refund; immutable adjustment row plus original transaction history. */
public final class DaoThanhToanService {

    private final DaoThanhToanDao dao=new DaoThanhToanDao();
    private final DesktopSession session;private final ConnectionProvider connections;
    private final AuthorizationService auth=new AuthorizationService(new RolePermissionPolicy());
    public DaoThanhToanService(DesktopSession session,ConnectionProvider connections){this.session=session;this.connections=connections;}
    public void reverse(String invoiceId,String paymentId,LoaiDaoThanhToan kind,String reason,String reference)throws SQLException {
        auth.require(session,Quyen.INVOICE_CANCEL_APPROVE);
        if(kind==null||reason==null||reason.isBlank()||reason.length()>300||reference==null||reference.isBlank()||reference.length()>100)throw new IllegalArgumentException("Cần lý do (tối đa 300 ký tự) và bằng chứng/tham chiếu (tối đa 100 ký tự).");
        boolean committed=false;
        try(Connection c=connections.open()){
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);c.setAutoCommit(false);
            try{
                dao.khoaHoaDon(c,invoiceId);
                if(LanThanhToanService.pending(c,invoiceId)!=null)throw new IllegalArgumentException("Còn lần thu chưa đối soát. Giải quyết lần thu trước khi điều chỉnh.");
                ThongTinThanhToan payment=new ThanhToanDao().existing(c,paymentId);
                if(payment==null||!invoiceId.equals(payment.maHoaDon())||payment.trangThai()!=TrangThaiThanhToan.THANH_CONG)throw new IllegalArgumentException("Khoản thu không thuộc hóa đơn hoặc đã được điều chỉnh. Tải lại để kiểm tra.");
                ThongTinHoaDon invoice=new HoaDonDao().getForPayment(c,invoiceId);
                if(invoice.trangThai()==TrangThaiHoaDon.DA_HUY)throw new IllegalArgumentException("Hóa đơn đã hủy; cần đối soát.");
                Timestamp when;
                when=dao.thoiGianHienTai(c);
                dao.taoDieuChinh(c,paymentId,kind.name(),payment.soTien(),when,session.accountId(),reason,reference);
                dao.capNhatKhoanThu(c,paymentId,kind==LoaiDaoThanhToan.HOAN_TIEN?"DA_HOAN_TIEN":"DA_HUY",when,session.accountId(),reason);
                BigDecimal net=invoice.daThu().subtract(payment.soTien());
                TrangThaiHoaDon status=net.compareTo(invoice.tongTien())==0?TrangThaiHoaDon.DA_THANH_TOAN:net.signum()==0?TrangThaiHoaDon.CHUA_THANH_TOAN:TrangThaiHoaDon.THANH_TOAN_MOT_PHAN;
                new ThanhToanDao().updateStatus(c,invoiceId,status);
                dao.ghiNhatKy(c,paymentId,session.accountId(),kind==LoaiDaoThanhToan.HOAN_TIEN?"Hoàn tiền":"Hủy ghi nhận thu sai");
                auth.requireCurrent(session,Quyen.INVOICE_CANCEL_APPROVE,c);auth.require(session,Quyen.INVOICE_CANCEL_APPROVE);c.commit();committed=true;
            }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}throw e;}
        }catch(SQLException e){if(committed)throw new CommittedReversalException(e);throw e;}
    }
    public static final class CommittedReversalException extends SQLException { public CommittedReversalException(SQLException cause){super("Điều chỉnh đã lưu; tải lại lịch sử để kiểm tra.",cause);} }
}
