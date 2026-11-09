package com.quanlynhakhoa.service.hoadon;

import com.quanlynhakhoa.security.RolePermissionPolicy;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.dao.hoadon.ThanhToanDao;
import com.quanlynhakhoa.dao.hoadon.TaoHoaDonDao;
import com.quanlynhakhoa.dao.hoadon.HoaDonDao;
import com.quanlynhakhoa.config.ConnectionProvider;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.enums.TrangThaiThanhToan;
import com.quanlynhakhoa.enums.TrangThaiHoaDon;
import com.quanlynhakhoa.enums.PhuongThucThanhToan;
import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import com.quanlynhakhoa.model.hoadon.YeuCauThanhToan;
import com.quanlynhakhoa.model.hoadon.ThongTinThanhToan;
import com.quanlynhakhoa.model.hoadon.ThanhToanDangCho;
import com.quanlynhakhoa.model.hoadon.PhanThanhToan;
import com.quanlynhakhoa.dao.hoadon.LanThanhToanDao;
import com.quanlynhakhoa.model.hoadon.ThongTinLanThuDaKhoa;
import java.sql.*;
import java.util.*;

/** A durable, server-side reservation survives a lost response and is visible on every workstation. */
public final class LanThanhToanService {

    private final LanThanhToanDao dao=new LanThanhToanDao();
    private final DesktopSession session;private final ConnectionProvider connections;
    private final AuthorizationService auth=new AuthorizationService(new RolePermissionPolicy());
    public LanThanhToanService(DesktopSession session,ConnectionProvider connections){this.session=session;this.connections=connections;}
    public ThanhToanDangCho pending(String invoice)throws SQLException {
        auth.require(session,Quyen.BILLING_READ);
        try(Connection c=connections.open()){return pending(c,invoice);}
    }
    static ThanhToanDangCho pending(Connection c,String invoice)throws SQLException {
        return new LanThanhToanDao().pending(c,invoice);
    }
    public void reserve(YeuCauThanhToan request)throws SQLException {
        auth.require(session,Quyen.BILLING_MANAGE);ThanhToanService.validate(request);
        try(Connection c=connections.open()){
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);c.setAutoCommit(false);
            try{
                new ThanhToanDao().lockInvoice(c,request.invoiceId());
                boolean daCoLanThu=dao.voiLanThuDaKhoa(c,request.parts().getFirst().id(),new LanThanhToanDao.XuLyLanThu(){
                    public void xuLy(ThongTinLanThuDaKhoa lanThu)throws SQLException {
                        var part=request.parts().getFirst();
                        if(!request.invoiceId().equals(lanThu.maHoaDon())||!session.accountId().equals(lanThu.maTaiKhoan())||part.amount().compareTo(lanThu.soTien())!=0||!part.method().name().equals(lanThu.phuongThuc())||!Objects.equals(part.reference(),lanThu.maGiaoDich())||!equal(part.customerTender(),lanThu.tienKhachDua())||"DA_HUY".equals(lanThu.trangThai()))
                            throw new IllegalArgumentException("Lần thu thuộc tài khoản khác, đã được giải quyết hoặc có dữ liệu khác. Kiểm tra kết quả trước khi thu tiếp.");
                        auth.requireCurrent(session,Quyen.BILLING_MANAGE,c);c.commit();
                    }
                });
                if(daCoLanThu)return;
                var prior=new ThanhToanDao().existing(c,request.parts().getFirst().id());
                boolean recorded=prior!=null;
                if(recorded&&!ThanhToanService.matches(prior,request.invoiceId(),new TaoHoaDonDao().employee(c,session.accountId()),request.parts().getFirst()))
                    throw new IllegalArgumentException("Mã lần thu đã có giao dịch khác hoặc đã điều chỉnh.");
                if(!recorded&&pending(c,request.invoiceId())!=null)throw new IllegalArgumentException("Hóa đơn đang có lần thu chờ đối soát trên máy khác. Bấm Kiểm tra để tải lần thu đó.");
                ThongTinHoaDon invoice=new HoaDonDao().getForPayment(c,request.invoiceId());
                if(!recorded&&(invoice==null||invoice.trangThai()==TrangThaiHoaDon.DA_HUY||!invoice.canhBao().isEmpty()||request.amount().compareTo(invoice.conNo())>0))throw new IllegalArgumentException("Hóa đơn không còn đủ số nợ cho lần thu này.");
                if(!recorded)new ThanhToanDao().requireNoPendingCancellation(c,request.invoiceId());
                var part=request.parts().getFirst();
                dao.taoLanThu(c,request,session.accountId(),recorded?"THANH_CONG":"CHO_XU_LY");
                auth.requireCurrent(session,Quyen.BILLING_MANAGE,c);c.commit();
            }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}throw e;}
        }
    }
    static void requireAndComplete(Connection c,YeuCauThanhToan request,String account)throws SQLException {
        // Keep the reservation until the client has received the commit response.
        new LanThanhToanDao().kiemTraLanThuConHieuLuc(c,request,account);
    }
    public void acknowledge(YeuCauThanhToan request)throws SQLException {
        auth.require(session,Quyen.BILLING_MANAGE);
        try(Connection c=connections.open()){
            c.setAutoCommit(false);
            try{
                ThongTinThanhToan payment=new ThanhToanDao().existing(c,request.parts().getFirst().id());
                if(payment==null||!ThanhToanService.matches(payment,request.invoiceId(),new TaoHoaDonDao().employee(c,session.accountId()),request.parts().getFirst()))throw new IllegalArgumentException("Chưa có giao dịch khớp để kết thúc đối soát.");
                dao.xacNhanHoanTat(c,request,session.accountId());
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
                dao.khoaHoaDon(c,invoice);
                ThongTinThanhToan recorded=new ThanhToanDao().existing(c,attempt);
                ThanhToanDangCho pending=pending(c,invoice);
                if(pending==null||!pending.request().parts().getFirst().id().equals(attempt))throw new IllegalArgumentException("Lần thu đã được xử lý.");
                if(recorded!=null){
                    var part=pending.request().parts().getFirst();
                    if(!invoice.equals(recorded.maHoaDon())||recorded.trangThai()!=TrangThaiThanhToan.THANH_CONG||recorded.soTien().compareTo(part.amount())!=0||recorded.phuongThuc()!=part.method()||!Objects.equals(recorded.maGiaoDich(),part.reference())||!equal(recorded.tienKhachDua(),part.customerTender()))throw new IllegalArgumentException("Giao dịch không khớp lần thu; cần kiểm tra thủ công.");
                }
                dao.ketThucDoiSoat(c,invoice,attempt,reason,session.accountId(),recorded==null?"DA_HUY":"THANH_CONG");
                dao.ghiNhatKyDoiSoat(c,session.accountId(),attempt);
                auth.requireCurrent(session,Quyen.INVOICE_CANCEL_APPROVE,c);c.commit();
            }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}throw e;}
        }
    }
    private static boolean equal(java.math.BigDecimal a,java.math.BigDecimal b){return a==null?b==null:b!=null&&a.compareTo(b)==0;}
}
