package com.quanlynhakhoa.nghiepvu.hoadon;

import com.quanlynhakhoa.hethong.xacthuc.AuthDecision;
import com.quanlynhakhoa.nghiepvu.hoadon.dao.*;
import com.quanlynhakhoa.nghiepvu.hoadon.entity.*;
import com.quanlynhakhoa.nghiepvu.hoadon.service.*;
import com.quanlynhakhoa.quantri.vaitro.entity.Role;
import com.quanlynhakhoa.hethong.phanquyen.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import static com.quanlynhakhoa.nghiepvu.hoadon.ServiceBillingSelfTest.*;

public final class CancellationSelfTest {
    private static class Invoices extends InvoiceDao {
        BigDecimal paid=BigDecimal.ZERO;InvoiceStatus status=InvoiceStatus.CHUA_THANH_TOAN;
        @Override public Invoice getForPayment(Connection c,String id){return new Invoice("H1","NV1","BN1","Synthetic",null,LocalDateTime.now(),status,new BigDecimal("300"),paid,BigDecimal.ZERO);}
        @Override public List<InvoiceLine> lines(Connection c,String id){return List.of(new InvoiceLine(1,"H1","DV1",null,null,1,new BigDecimal("300")));}
        @Override public List<Payment> payments(Connection c,String id){return paid.signum()==0?List.of():List.of(new Payment("P1","H1",LocalDateTime.now(),paid,Payment.PhuongThuc.TIEN_MAT,null,paid,Payment.TrangThai.THANH_CONG,"NV1",null,null,null));}
    }
    private static class Dao extends CancellationDao {
        boolean pending;InvoiceCancellationRequest.TrangThai status=InvoiceCancellationRequest.TrangThai.CHO_DUYET;String requester="TST_ACC";
        @Override public void lockInvoice(Connection c,String id){check(id.equals("H1"),"locks correct invoice first");}
        @Override public void lockRequest(Connection c,String id){check(id.equals("Y1"),"locks correct request");}
        @Override public boolean pending(Connection c,String id){return pending;}
        @Override public String invoiceId(Connection c,String id)throws SQLException{check(c.getAutoCommit(),"request invoice mapping read before transaction lock acquisition");return "H1";}
        @Override public InvoiceCancellationRequest get(Connection c,String id){return new InvoiceCancellationRequest("Y1","H1","BN1","Synthetic",new BigDecimal("300"),BigDecimal.ZERO,requester,LocalDateTime.now(),"Synthetic reason",status,null,null,null);}
        @Override public List<InvoiceCancellationRequest> find(Connection c,String text,LocalDate from,LocalDate to,InvoiceCancellationRequest.TrangThai status){return List.of(get(c,"Y1"));}
    }
    public static void main(String[] args)throws Exception {
        final FeatureJdbcDouble jdbc=new FeatureJdbcDouble();final Dao dao=new Dao();final Invoices invoices=new Invoices();
        InvoiceIdGenerator ids=new InvoiceIdGenerator(){public String next(Connection c){return "Y1";}};
        final DesktopSession receptionist=session(Role.RECEPTIONIST);
        final DesktopSession manager=DesktopSession.fromSuccessfulLogin(new AuthDecision(true,"REVIEWER",Role.CLINIC_MANAGER,false));
        final CancellationService requests=new CancellationService(receptionist,jdbc,dao,invoices,ids);
        final CancellationService reviews=new CancellationService(manager,jdbc,dao,invoices,ids);
        check(requests.preview("H1").invoice().hoaDon().tongTien().intValueExact()==300,"preview contains trusted header, lines and history");
        check(requests.request("H1","  Sai chứng từ  ").equals("Y1"),"request saved");
        check(jdbc.persisted.size()==1&&jdbc.audits.size()==1,"request and audit committed together");
        check(!jdbc.sql.stream().anyMatch(new java.util.function.Predicate<String>(){public boolean test(String sql){return sql.startsWith("UPDATE dbo.HoaDon");}}),"submission does not cancel invoice");
        int before=jdbc.opens;
        expect(IllegalArgumentException.class,new Action(){public void run()throws Exception{requests.request("H1"," ");}});
        expect(IllegalArgumentException.class,new Action(){public void run()throws Exception{requests.request("H1","x".repeat(501));}});check(jdbc.opens==before,"bad reason before connection");
        dao.pending=true;expect(IllegalArgumentException.class,new Action(){public void run()throws Exception{requests.request("H1","duplicate");}});dao.pending=false;
        invoices.status=InvoiceStatus.DA_HUY;expect(IllegalArgumentException.class,new Action(){public void run()throws Exception{requests.request("H1","cancelled");}});invoices.status=InvoiceStatus.CHUA_THANH_TOAN;
        check(reviews.find("",null,null,null).size()==1&&reviews.detail("Y1").invoice().chiTiet().size()==1,"review list/detail available to manager");
        int writes=jdbc.persisted.size();reviews.decide("Y1",true,"");
        check(jdbc.persisted.size()==writes+2,"approval updates invoice and request in one commit");
        int auditCount=jdbc.audits.size();jdbc.auditFailure=true;writes=jdbc.persisted.size();
        expect(SQLException.class,new Action(){public void run()throws Exception{reviews.decide("Y1",true,"audit failure");}});
        check(jdbc.persisted.size()==writes&&jdbc.audits.size()==auditCount,"audit error rolls back both status changes");jdbc.auditFailure=false;
        before=jdbc.opens;expect(IllegalArgumentException.class,new Action(){public void run()throws Exception{reviews.decide("Y1",false," ");}});check(jdbc.opens==before,"rejection reason required before connection");
        invoices.paid=new BigDecimal("100");invoices.status=InvoiceStatus.THANH_TOAN_MOT_PHAN;writes=jdbc.updates;
        expect(IllegalArgumentException.class,new Action(){public void run()throws Exception{requests.request("H1","Paid invoice");}});
        check(jdbc.updates==writes,"partially paid invoice rejected before request/audit insert");
        invoices.paid=new BigDecimal("300");invoices.status=InvoiceStatus.DA_THANH_TOAN;
        expect(IllegalArgumentException.class,new Action(){public void run()throws Exception{requests.request("H1","Fully paid invoice");}});
        check(jdbc.updates==writes,"fully paid invoice rejected before request/audit insert");
        invoices.paid=new BigDecimal("100");invoices.status=InvoiceStatus.THANH_TOAN_MOT_PHAN;
        expect(IllegalArgumentException.class,new Action(){public void run()throws Exception{reviews.decide("Y1",true,"paid");}});check(jdbc.updates==writes,"paid invoice cannot be cancelled and no refund is invented");
        reviews.decide("Y1",false,"Chưa xử lý khoản thu");check(jdbc.updates==writes+2,"rejection updates request and audit only");
        dao.status=InvoiceCancellationRequest.TrangThai.DA_DUYET;expect(IllegalArgumentException.class,new Action(){public void run()throws Exception{reviews.decide("Y1",false,"again");}});dao.status=InvoiceCancellationRequest.TrangThai.CHO_DUYET;
        dao.requester="REVIEWER";expect(IllegalArgumentException.class,new Action(){public void run()throws Exception{reviews.decide("Y1",false,"self");}});dao.requester="TST_ACC";
        before=jdbc.opens;
        expect(AccessDeniedException.class,new Action(){public void run()throws Exception{requests.find("",null,null,null);}});
        expect(AccessDeniedException.class,new Action(){public void run()throws Exception{requests.detail("Y1");}});
        expect(AccessDeniedException.class,new Action(){public void run()throws Exception{requests.decide("Y1",true,"");}});
        expect(AccessDeniedException.class,new Action(){public void run()throws Exception{reviews.request("H1","manager lacks request permission");}});check(jdbc.opens==before,"wrong permissions rejected before JDBC");
        manager.logout();receptionist.logout();before=jdbc.opens;
        expect(AccessDeniedException.class,new Action(){public void run()throws Exception{requests.preview("H1");}});
        expect(AccessDeniedException.class,new Action(){public void run()throws Exception{requests.request("H1","logout");}});
        expect(AccessDeniedException.class,new Action(){public void run()throws Exception{reviews.find("",null,null,null);}});
        expect(AccessDeniedException.class,new Action(){public void run()throws Exception{reviews.detail("Y1");}});
        expect(AccessDeniedException.class,new Action(){public void run()throws Exception{reviews.decide("Y1",true,"");}});check(jdbc.opens==before,"logout blocks all cancellation data methods");
        for(String sql:jdbc.sql)check(!sql.contains("DELETE")&&!sql.contains("UPDATE dbo.ThanhToan"),"history preserved");
        System.out.println("PASS: CancellationSelfTest (request/approval/rejection, reasons, paid guard, audit rollback, permissions; synthetic JDBC).");
    }
}
