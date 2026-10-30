package com.quanlynhakhoa.billing.service;

import com.quanlynhakhoa.billing.dao.*;
import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.security.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/** Does not accept arbitrary employee, invoice status, prices or totals from a form. */
public final class InvoiceCreationService {
    private final DesktopSession session;
    private final ConnectionProvider connections;
    private final ClinicalBillingSource source;
    private final InvoiceIdGenerator ids;
    private final InvoiceCreateDao dao;
    private final AuthorizationService authorization=new AuthorizationService(new RolePermissionPolicy());
    public InvoiceCreationService(DesktopSession session,ConnectionProvider connections,ClinicalBillingSource source,InvoiceIdGenerator ids) {
        this(session,connections,source,ids,new InvoiceCreateDao());
    }
    public InvoiceCreationService(DesktopSession session,ConnectionProvider connections,ClinicalBillingSource source,InvoiceIdGenerator ids,InvoiceCreateDao dao) {
        this.session=Objects.requireNonNull(session);this.connections=Objects.requireNonNull(connections);
        this.source=Objects.requireNonNull(source);this.ids=Objects.requireNonNull(ids);this.dao=Objects.requireNonNull(dao);
    }
    private void require(){authorization.require(session,Permission.BILLING_MANAGE);}
    public List<BillingChoice> patients(String text) throws SQLException {
        require();String keyword=text==null?"":text.trim();if(keyword.length()>100)throw new IllegalArgumentException("Từ khóa tối đa 100 ký tự.");
        try(Connection c=connections.open()){require();List<BillingChoice> result=dao.patients(c,keyword);require();return result;}
    }
    public List<BillingChoice> visits(String patient) throws SQLException {
        require();key(patient);try(Connection c=connections.open()){require();List<BillingChoice> result=dao.visits(c,patient);require();return result;}
    }
    public InvoiceDraft preview(String patient,String visit) throws SQLException {
        require();key(patient);key(visit);
        try(Connection c=connections.open()) {
            begin(c);
            try {require();dao.validateVisit(c,patient,visit);dao.employee(c,session.accountId());
                InvoiceDraft draft=source.load(c,patient,visit,true);validate(draft,patient,visit);checkServices(c,draft);require();c.commit();return draft;
            } catch(SQLException|RuntimeException e){rollback(c,e);throw e;}
        }
    }
    public String create(InvoiceDraft reviewed) throws SQLException {
        require();Objects.requireNonNull(reviewed,"Bản xem trước");key(reviewed.patientId());key(reviewed.visitId());
        boolean committed=false;String id=null;
        try(Connection c=connections.open()) {
            begin(c);
            try {
                require();dao.requireWritePermissions(c);dao.validateVisit(c,reviewed.patientId(),reviewed.visitId());
                String employee=dao.employee(c,session.accountId());
                InvoiceDraft trusted=source.load(c,reviewed.patientId(),reviewed.visitId(),true);
                validate(trusted,reviewed.patientId(),reviewed.visitId());
                checkServices(c,trusted);
                if(!trusted.equals(reviewed))throw new IllegalArgumentException("Dữ liệu khám/giá đã đổi. Tải lại bản xem trước rồi xác nhận.");
                id=ids.next(c);key(id);
                dao.insertHeader(c,id,trusted.patientId(),trusted.visitId(),employee);
                for(InvoiceDraft.Line line:trusted.lines())dao.insertLine(c,id,line);
                dao.audit(c,session.accountId(),id);require();c.commit();committed=true;
            } catch(SQLException|RuntimeException e){rollback(c,e);throw e;}
        } catch(SQLException e) {
            if(committed)throw new CommittedInvoiceException(id,e);
            if(e.getErrorCode()==2601||e.getErrorCode()==2627)throw new IllegalArgumentException("Trùng mã hoặc lượt khám đã được lập hóa đơn bởi người khác. Tải lại để kiểm tra.",e);
            throw e;
        }
        return id;
    }
    public static final class CommittedInvoiceException extends SQLException {
        private final String invoiceId;
        public CommittedInvoiceException(String id,SQLException cause){super("Đã lưu hóa đơn; lỗi đóng kết nối.",cause);invoiceId=id;}
        public String invoiceId(){return invoiceId;}
    }
    private static void begin(Connection c)throws SQLException {if(!c.getAutoCommit())throw new SQLException("Expected fresh connection");c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);c.setAutoCommit(false);}
    private static void rollback(Connection c,Exception e){try{c.rollback();}catch(SQLException failure){e.addSuppressed(failure);}}
    private static void key(String id){if(id==null||id.isBlank()||id.length()>10)throw new IllegalArgumentException("Mã phải có 1–10 ký tự.");for(char ch:id.toCharArray())if(ch<33||ch>126)throw new IllegalArgumentException("Mã phải là ASCII không khoảng trắng.");}
    private void checkServices(Connection c,InvoiceDraft draft) throws SQLException {
        for(InvoiceDraft.Line line:draft.lines()) {
            boolean active=dao.serviceActive(c,line.serviceId());
            source.validateServiceState(c,draft,line.serviceId(),active);
        }
    }
    private static void validate(InvoiceDraft draft,String patient,String visit) {
        if(draft==null||!patient.equals(draft.patientId())||!visit.equals(draft.visitId())||draft.revision()==null||draft.revision().isBlank())throw new IllegalArgumentException("Nguồn tính phí không hợp lệ.");
        if(draft.lines().isEmpty())throw new IllegalArgumentException("Không tạo hóa đơn rỗng.");
        for(InvoiceDraft.Line line:draft.lines()) {
            key(line.serviceId());BigDecimal price=line.unitPrice();
            if(line.quantity()<=0||price==null||price.signum()<0||price.stripTrailingZeros().scale()>0||price.compareTo(new BigDecimal("999999999999999999"))>0)
                throw new IllegalArgumentException("Số lượng/đơn giá tính phí không hợp lệ.");
        }
    }
}
