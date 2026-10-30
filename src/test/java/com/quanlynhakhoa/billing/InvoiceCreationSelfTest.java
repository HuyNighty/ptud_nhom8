package com.quanlynhakhoa.billing;

import com.quanlynhakhoa.billing.dao.*;
import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.billing.service.*;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.List;
import static com.quanlynhakhoa.billing.ServiceBillingSelfTest.*;

public final class InvoiceCreationSelfTest {
    private static class Dao extends InvoiceCreateDao {
        boolean duplicate, invalidVisit, noEmployee, noPermission, failLine;
        int lines;
        @Override public boolean serviceActive(Connection c,String id){return true;}
        @Override public void requireWritePermissions(Connection c)throws SQLException{if(noPermission)throw new SQLException("missing INSERT","42000",229);}
        @Override public void validateVisit(Connection c,String patient,String visit){if(invalidVisit||!patient.equals("BN1")||!visit.equals("PK1"))throw new IllegalArgumentException("invalid visit");if(duplicate)throw new IllegalArgumentException("duplicate visit");}
        @Override public String employee(Connection c,String account){if(noEmployee)throw new IllegalArgumentException("missing employee");check(account.equals("TST_ACC"),"session account supplied");return "NV1";}
        @Override public void insertLine(Connection c,String id,InvoiceDraft.Line line)throws SQLException{if(failLine && ++lines==2)throw new SQLException("line failure");super.insertLine(c,id,line);}
    }
    private static class Source implements ClinicalBillingSource {
        InvoiceDraft value=draft("100");int loads;
        public InvoiceDraft load(Connection c,String patient,String visit,boolean lock){check(lock,"source rows locked in transaction");loads++;return value;}
        public void validateServiceState(Connection c,InvoiceDraft draft,String id,boolean active){check(active,"catalogue state checked");}
    }
    private static InvoiceDraft draft(String price){return new InvoiceDraft("BN1","PK1","revision1",List.of(new InvoiceDraft.Line("DV1","Synthetic 1",2,new BigDecimal(price)),new InvoiceDraft.Line("DV2","Synthetic 2",1,BigDecimal.ZERO)));}
    public static void main(String[] args)throws Exception {
        final FeatureJdbcDouble jdbc=new FeatureJdbcDouble();final Dao dao=new Dao();final Source source=new Source();
        final DesktopSession session=session(Role.RECEPTIONIST);
        final InvoiceCreationService service=new InvoiceCreationService(session,jdbc,source,new InvoiceIdGenerator(){public String next(Connection c){return "HDTEST1";}},dao);
        final InvoiceDraft reviewed=service.preview("BN1","PK1");
        check(service.create(reviewed).equals("HDTEST1"),"created id");
        check(jdbc.persisted.size()==3 && jdbc.audits.size()==1 && jdbc.commits==2,"header, all lines and audit committed");
        boolean employeeBound=false;
        for(java.util.Map<Integer,Object> parameters:jdbc.bindings) {
            if("NV1".equals(parameters.get(2)))employeeBound=true;
        }
        check(employeeBound,"employee from authenticated account link");
        boolean header=false;
        for(String sql:jdbc.sql){if(sql.contains("INSERT INTO dbo.HoaDon")){header=true;check(sql.contains("CHUA_THANH_TOAN"),"unpaid status");}check(!sql.contains("INSERT INTO dbo.ThanhToan"),"no payment write");}
        check(header && reviewed.total().compareTo(new BigDecimal("200"))==0,"trusted totals");
        final int persisted=jdbc.persisted.size();
        source.value=draft("101");
        expect(IllegalArgumentException.class,new Action(){public void run()throws Exception{service.create(reviewed);}});
        check(jdbc.persisted.size()==persisted,"changed trusted price rejected before write");source.value=draft("100");
        dao.failLine=true;
        expect(SQLException.class,new Action(){public void run()throws Exception{service.create(reviewed);}});
        check(jdbc.persisted.size()==persisted,"second line failure rolls back header and first line");dao.failLine=false;
        jdbc.auditFailure=true;
        expect(SQLException.class,new Action(){public void run()throws Exception{service.create(reviewed);}});
        check(jdbc.persisted.size()==persisted,"audit failure rolls back all invoice rows");jdbc.auditFailure=false;
        dao.duplicate=true;expect(IllegalArgumentException.class,new Action(){public void run()throws Exception{service.create(reviewed);}});dao.duplicate=false;
        expect(IllegalArgumentException.class,new Action(){public void run()throws Exception{service.preview("BN2","PK1");}});
        dao.noEmployee=true;expect(IllegalArgumentException.class,new Action(){public void run()throws Exception{service.create(reviewed);}});dao.noEmployee=false;
        dao.noPermission=true;expect(SQLException.class,new Action(){public void run()throws Exception{service.create(reviewed);}});dao.noPermission=false;
        for(final InvoiceDraft invalid:List.of(new InvoiceDraft("BN1","PK1","r",List.of()),new InvoiceDraft("BN1","PK1","r",List.of(new InvoiceDraft.Line("DV1","X",0,BigDecimal.ONE))),draft("-1"),draft("1.5"),draft("1000000000000000000"))) {
            source.value=invalid;expect(IllegalArgumentException.class,new Action(){public void run()throws Exception{service.preview("BN1","PK1");}});
        }
        final HoaDonService defaultService=new HoaDonService(session,jdbc);
        int before=jdbc.opens;expect(IllegalStateException.class,new Action(){public void run()throws Exception{defaultService.createInvoice(reviewed);}});check(jdbc.opens==before,"unconfigured contract blocks before JDBC");
        final InvoiceCreationService denied=new InvoiceCreationService(session(Role.CLINIC_MANAGER),jdbc,source,new InvoiceIdGenerator(){public String next(Connection c){return "X";}},dao);
        before=jdbc.opens;expect(AccessDeniedException.class,new Action(){public void run()throws Exception{denied.create(reviewed);}});check(jdbc.opens==before,"read role cannot create");
        session.logout();before=jdbc.opens;
        expect(AccessDeniedException.class,new Action(){public void run()throws Exception{service.create(reviewed);}});
        expect(AccessDeniedException.class,new Action(){public void run()throws Exception{service.preview("BN1","PK1");}});
        expect(AccessDeniedException.class,new Action(){public void run()throws Exception{service.patients("");}});
        expect(AccessDeniedException.class,new Action(){public void run()throws Exception{service.visits("BN1");}});
        check(jdbc.opens==before,"logout blocks all creation methods before connection");
        System.out.println("PASS: InvoiceCreationSelfTest (JDBC double; real SQL write and concurrency acceptance pending).");
    }
}
