package com.quanlynhakhoa.service.hoadon;

import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.dao.hoadon.ThanhToanDao;
import com.quanlynhakhoa.dao.hoadon.TaoHoaDonDao;
import com.quanlynhakhoa.dao.hoadon.HoaDonDao;
import com.quanlynhakhoa.enums.TrangThaiThanhToan;
import com.quanlynhakhoa.enums.TrangThaiHoaDon;
import com.quanlynhakhoa.enums.PhuongThucThanhToan;
import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import com.quanlynhakhoa.model.hoadon.YeuCauThanhToan;
import com.quanlynhakhoa.entity.ThanhToan;
import com.quanlynhakhoa.model.hoadon.PhanThanhToan;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import static com.quanlynhakhoa.service.hoadon.ServiceBillingSelfTest.*;

/** Protocol/transaction tests with a JDBC double. Real SQL concurrency still requires approved grants. */
public final class PaymentSelfTest {
    private static BigDecimal m(String n){return new BigDecimal(n);}
    private static ThongTinHoaDon invoice(String total,String paid,TrangThaiHoaDon status){return new ThongTinHoaDon("H1","NV1","BN1","Synthetic",null,LocalDateTime.now(),status,m(total),m(paid),BigDecimal.ZERO);}
    private static YeuCauThanhToan cash(String id,String amount){return new YeuCauThanhToan("H1",m(amount),List.of(new PhanThanhToan(id,m(amount),PhuongThucThanhToan.TIEN_MAT,null,m(amount))),false);}
    private static class Invoices extends HoaDonDao {
        ThongTinHoaDon current=invoice("300","0",TrangThaiHoaDon.CHUA_THANH_TOAN);
        @Override public ThongTinHoaDon getForPayment(Connection c,String id){return current;}
    }
    private static class Accounts extends TaoHoaDonDao {
        @Override public String employee(Connection c,String account){check("TST_ACC".equals(account),"authenticated account used");return "NV1";}
    }
    private static class Dao extends ThanhToanDao {
        Map<String,ThanhToan> existing=new HashMap<>();boolean failInsert,duplicateReference,pendingCancellation;int inserts;TrangThaiHoaDon nextStatus;
        @Override public void requirePermissions(Connection c){ }
        @Override public void lockInvoice(Connection c,String id){check("H1".equals(id),"locks selected invoice");}
        @Override public ThanhToan existing(Connection c,String id){return existing.get(id);}
        @Override public void requireNoPendingCancellation(Connection c,String id){if(pendingCancellation)throw new IllegalArgumentException("pending cancellation");}
        @Override public void requireNewTransferReference(Connection c,String reference){if(duplicateReference)throw new IllegalArgumentException("duplicate reference");}
        @Override public void insert(Connection c,String invoice,String employee,PhanThanhToan part)throws SQLException {
            if(failInsert&&++inserts==1)throw new SQLException("second part failed");super.insert(c,invoice,employee,part);
        }
        @Override public void updateStatus(Connection c,String id,TrangThaiHoaDon status)throws SQLException{nextStatus=status;super.updateStatus(c,id,status);}
    }
    public static void main(String[] args)throws Exception {
        final FeatureJdbcDouble jdbc=new FeatureJdbcDouble();final Dao dao=new Dao();final Invoices invoices=new Invoices();final DesktopSession principal=session(VaiTroHeThong.RECEPTIONIST);
        final ThanhToanService service=new ThanhToanService(principal,jdbc,dao,invoices,new Accounts(),new ThanhToanService.Attempts(){public void reserve(YeuCauThanhToan request){} public void complete(Connection c,YeuCauThanhToan request,String account){}});
        check(!service.pendingCancellation("H1"),"read-only pending cancellation check available to cashier");
        final YeuCauThanhToan mixed=new YeuCauThanhToan("H1",m("100"),List.of(new PhanThanhToan("P2",m("100"),PhuongThucThanhToan.CHUYEN_KHOAN,"BANK001",null)),true);
        final YeuCauThanhToan combined=new YeuCauThanhToan("H1",m("100"),List.of(new PhanThanhToan("P1",m("40"),PhuongThucThanhToan.TIEN_MAT,null,m("50")),new PhanThanhToan("P2",m("60"),PhuongThucThanhToan.CHUYEN_KHOAN,"BANK001",null)),true);
        int initialOpens=jdbc.opens;
        expect(IllegalArgumentException.class,new Action(){public void run()throws Exception{service.collect(combined);}});
        check(jdbc.opens==initialOpens,"combined method rejected before opening connection");
        service.collect(mixed);
        check(jdbc.persisted.size()==2&&jdbc.audits.size()==1&&jdbc.commits==1,"one method, invoice status, audit committed together");
        check(dao.nextStatus==TrangThaiHoaDon.THANH_TOAN_MOT_PHAN,"partial payment retains debt");
        check(jdbc.persisted.get(0).get(3).equals(m("100")),"single bank amount stored");
        check(jdbc.persisted.get(0).get(7).equals("NV1"),"cashier derived from account");
        for(PhanThanhToan part:mixed.parts())dao.existing.put(part.id(),new ThanhToan(part.id(),"H1",LocalDateTime.now(),part.amount(),part.method(),part.reference(),part.customerTender(),TrangThaiThanhToan.THANH_CONG,"NV1",null,null,null));
        int written=jdbc.updates;service.collect(mixed);check(jdbc.updates==written,"same confirmed request is idempotent");dao.existing.clear();
        invoices.current=invoice("300","100",TrangThaiHoaDon.THANH_TOAN_MOT_PHAN);service.collect(cash("P3","200"));
        check(dao.nextStatus==TrangThaiHoaDon.DA_THANH_TOAN,"full collection marks invoice paid");
        check(CongNoService.outstanding(List.of(invoice("300","300",TrangThaiHoaDon.DA_THANH_TOAN),invoice("300","0",TrangThaiHoaDon.DA_HUY),invoice("300","100",TrangThaiHoaDon.THANH_TOAN_MOT_PHAN))).size()==1,"debt projection excludes paid and cancelled invoices");
        invoices.current=invoice("300","250",TrangThaiHoaDon.THANH_TOAN_MOT_PHAN);
        expect(IllegalArgumentException.class,new Action(){public void run()throws Exception{service.collect(cash("OVER","100"));}});
        invoices.current=invoice("300","0",TrangThaiHoaDon.DA_HUY);
        expect(IllegalArgumentException.class,new Action(){public void run()throws Exception{service.collect(cash("CANCEL","100"));}});
        invoices.current=invoice("300","0",TrangThaiHoaDon.CHUA_THANH_TOAN);
        int persisted=jdbc.persisted.size();dao.failInsert=true;
        expect(SQLException.class,new Action(){public void run()throws Exception{service.collect(mixed);}});
        check(jdbc.persisted.size()==persisted,"failed payment insert rolls back transaction");dao.failInsert=false;
        jdbc.auditFailure=true;
        expect(SQLException.class,new Action(){public void run()throws Exception{service.collect(cash("AUDIT","100"));}});
        check(jdbc.persisted.size()==persisted,"audit failure rolls back payment and status");jdbc.auditFailure=false;
        dao.pendingCancellation=true;
        expect(IllegalArgumentException.class,new Action(){public void run()throws Exception{service.collect(cash("PENDING","100"));}});dao.pendingCancellation=false;
        dao.duplicateReference=true;
        expect(IllegalArgumentException.class,new Action(){public void run()throws Exception{service.collect(mixed);}});dao.duplicateReference=false;
        final YeuCauThanhToan[] bad={cash("ZERO","0"),cash("NEG","-1"),cash("FRACTION","0.5"),new YeuCauThanhToan("H1",m("101"),mixed.parts(),true),new YeuCauThanhToan("H1",m("100"),mixed.parts(),false),cash("BIG","1000000000000000000")};
        for(final YeuCauThanhToan request:bad)expect(IllegalArgumentException.class,new Action(){public void run(){ThanhToanService.validate(request);}});
        final ThanhToanService denied=new ThanhToanService(session(VaiTroHeThong.CLINIC_MANAGER),jdbc,dao,invoices,new Accounts(),new ThanhToanService.Attempts(){public void reserve(YeuCauThanhToan request){} public void complete(Connection c,YeuCauThanhToan request,String account){}});int before=jdbc.opens;
        expect(AccessDeniedException.class,new Action(){public void run()throws Exception{denied.collect(mixed);}});check(jdbc.opens==before,"manager read permission does not authorize collection");
        expect(AccessDeniedException.class,new Action(){public void run()throws Exception{denied.pendingCancellation("H1");}});check(jdbc.opens==before,"cashier pending check requires billing manage");
        principal.logout();before=jdbc.opens;
        expect(AccessDeniedException.class,new Action(){public void run()throws Exception{service.collect(mixed);}});check(jdbc.opens==before,"logout rejected before connection");
        expect(AccessDeniedException.class,new Action(){public void run()throws Exception{service.pendingCancellation("H1");}});check(jdbc.opens==before,"pending check blocked after logout");
        System.out.println("PASS: PaymentSelfTest (single-method/partial/full, duplicate request, rollback, permissions; synthetic JDBC).");
    }
}
