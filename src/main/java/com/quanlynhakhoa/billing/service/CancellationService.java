package com.quanlynhakhoa.billing.service;

import com.quanlynhakhoa.billing.dao.*;
import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.security.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public final class CancellationService {
    public record Detail(YeuCauHuyHoaDon request,ChiTietBilling invoice) { }
    public record Preview(ChiTietBilling invoice,boolean pending) { }
    private final DesktopSession session;private final ConnectionProvider connections;
    private final CancellationDao dao;private final HoaDonDao invoices;private final InvoiceIdGenerator ids;
    private final AuthorizationService authorization=new AuthorizationService(new RolePermissionPolicy());
    public CancellationService(DesktopSession session,ConnectionProvider connections){this(session,connections,new CancellationDao(),new HoaDonDao(),new RandomInvoiceIdGenerator());}
    public CancellationService(DesktopSession session,ConnectionProvider connections,CancellationDao dao,HoaDonDao invoices,InvoiceIdGenerator ids) {
        this.session=Objects.requireNonNull(session);this.connections=Objects.requireNonNull(connections);this.dao=Objects.requireNonNull(dao);this.invoices=Objects.requireNonNull(invoices);this.ids=Objects.requireNonNull(ids);
    }
    private void require(Permission permission){authorization.require(session,permission);}
    public List<YeuCauHuyHoaDon> find(String keyword,LocalDate from,LocalDate to,YeuCauHuyHoaDon.TrangThai status)throws SQLException {
        require(Permission.INVOICE_CANCEL_APPROVE);String text=keyword==null?"":keyword.trim();
        if(text.length()>100)throw new IllegalArgumentException("Từ khóa tối đa 100 ký tự.");dates(from,to);
        try(Connection c=connections.open()){require(Permission.INVOICE_CANCEL_APPROVE);begin(c);
            try{List<YeuCauHuyHoaDon> result=dao.find(c,text,from,to,status);require(Permission.INVOICE_CANCEL_APPROVE);c.commit();return result;}
            catch(SQLException|RuntimeException e){rollback(c,e);throw e;}
        }
    }
    public Preview preview(String invoiceId)throws SQLException {
        require(Permission.INVOICE_CANCEL_REQUEST);key(invoiceId);
        try(Connection c=connections.open()){require(Permission.INVOICE_CANCEL_REQUEST);begin(c);
            try{ChiTietBilling detail=readInvoice(c,invoiceId);boolean pending=dao.pending(c,invoiceId);require(Permission.INVOICE_CANCEL_REQUEST);c.commit();return new Preview(detail,pending);}
            catch(SQLException|RuntimeException e){rollback(c,e);throw e;}
        }
    }
    public Detail detail(String requestId)throws SQLException {
        require(Permission.INVOICE_CANCEL_APPROVE);key(requestId);
        try(Connection c=connections.open()){require(Permission.INVOICE_CANCEL_APPROVE);begin(c);
            try{YeuCauHuyHoaDon request=dao.get(c,requestId);if(request==null)throw new IllegalArgumentException("Yêu cầu không tồn tại.");
                Detail result=new Detail(request,readInvoice(c,request.maHoaDon()));require(Permission.INVOICE_CANCEL_APPROVE);c.commit();return result;}
            catch(SQLException|RuntimeException e){rollback(c,e);throw e;}
        }
    }
    public String request(String invoiceId,String reason)throws SQLException {
        require(Permission.INVOICE_CANCEL_REQUEST);key(invoiceId);String normalized=reason(reason);String id=null;boolean committed=false;
        try(Connection c=connections.open()){require(Permission.INVOICE_CANCEL_REQUEST);begin(c);
            try{dao.lockInvoice(c,invoiceId);HoaDon invoice=invoices.getForPayment(c,invoiceId);
                if(invoice==null||invoice.trangThai()==TrangThaiHoaDon.DA_HUY)throw new IllegalArgumentException("Không gửi yêu cầu cho hóa đơn đã hủy/không tồn tại.");
                if(dao.pending(c,invoiceId))throw new IllegalArgumentException("Hóa đơn đã có yêu cầu đang chờ duyệt.");
                id=ids.next(c);key(id);dao.insert(c,id,invoiceId,session.accountId(),normalized);dao.audit(c,session.accountId(),id,"Gửi yêu cầu hủy");
                require(Permission.INVOICE_CANCEL_REQUEST);c.commit();committed=true;
            }catch(SQLException|RuntimeException e){rollback(c,e);throw e;}
        }catch(SQLException e){if(committed)throw new CommittedCancellationException(id,e);if(e.getErrorCode()==2601||e.getErrorCode()==2627)throw new IllegalArgumentException("Trùng mã hoặc đã có yêu cầu chờ duyệt; tải lại để kiểm tra.",e);throw e;}
        return id;
    }
    public void decide(String requestId,boolean approve,String decisionReason)throws SQLException {
        require(Permission.INVOICE_CANCEL_APPROVE);key(requestId);
        String normalized=approve&&(decisionReason==null||decisionReason.isBlank())?"Đồng ý hủy hóa đơn":reason(decisionReason);
        boolean committed=false;
        try(Connection c=connections.open()) {
            require(Permission.INVOICE_CANCEL_APPROVE);
            if(!c.getAutoCommit())throw new SQLException("Expected fresh connection");
            String invoiceId=dao.invoiceId(c,requestId);begin(c);
            try{dao.lockInvoice(c,invoiceId);dao.lockRequest(c,requestId);
                YeuCauHuyHoaDon request=dao.get(c,requestId);
                if(request==null||!invoiceId.equals(request.maHoaDon())||request.trangThai()!=YeuCauHuyHoaDon.TrangThai.CHO_DUYET)throw new IllegalArgumentException("Yêu cầu đã được xử lý hoặc không tồn tại.");
                if(session.accountId().equals(request.maTaiKhoanTao()))throw new IllegalArgumentException("Người gửi không được tự duyệt hoặc từ chối yêu cầu của mình.");
                HoaDon invoice=invoices.getForPayment(c,invoiceId);
                if(invoice==null||invoice.trangThai()==TrangThaiHoaDon.DA_HUY)throw new IllegalArgumentException("Hóa đơn đã hủy/không tồn tại; cần đối soát.");
                if(approve){
                    if(invoice.daThu().signum()!=0)throw new IllegalArgumentException("Hóa đơn có khoản thu hợp lệ. Chưa có quy trình hoàn tiền nên chưa thể duyệt hủy.");
                    dao.cancelInvoice(c,invoiceId);
                }
                // A rejection changes only the request: invoice/payment history remains untouched.
                dao.decide(c,requestId,session.accountId(),normalized,approve);dao.audit(c,session.accountId(),requestId,approve?"Duyệt hủy hóa đơn":"Từ chối hủy hóa đơn");
                require(Permission.INVOICE_CANCEL_APPROVE);c.commit();committed=true;
            }catch(SQLException|RuntimeException e){rollback(c,e);throw e;}
        }catch(SQLException e){if(committed)throw new CommittedCancellationException(requestId,e);throw e;}
    }
    private ChiTietBilling readInvoice(Connection c,String id)throws SQLException {
        HoaDon invoice=invoices.getForPayment(c,id);if(invoice==null)throw new IllegalArgumentException("Hóa đơn không tồn tại.");
        List<ChiTietHoaDon> lines=invoices.lines(c,id);List<ThanhToan> payments=invoices.payments(c,id);
        if(BillingCalculator.total(lines).compareTo(invoice.tongTien())!=0||BillingCalculator.collected(payments).compareTo(invoice.daThu())!=0)throw new SQLException("Kết quả hóa đơn không nhất quán.");
        return new ChiTietBilling(invoice,lines,payments);
    }
    public static final class CommittedCancellationException extends SQLException {
        private final String id;public CommittedCancellationException(String id,SQLException cause){super("Đã lưu thao tác hủy; lỗi đóng kết nối.",cause);this.id=id;}public String id(){return id;}
    }
    private static String reason(String value){String text=value==null?"":value.trim();if(text.isEmpty()||text.length()>500)throw new IllegalArgumentException("Lý do bắt buộc, tối đa 500 ký tự.");return text;}
    private static void key(String id){if(id==null||id.isBlank()||id.length()>10)throw new IllegalArgumentException("Mã cần 1–10 ký tự.");for(char ch:id.toCharArray())if(ch<33||ch>126)throw new IllegalArgumentException("Mã phải là ASCII không khoảng trắng.");}
    private static void dates(LocalDate from,LocalDate to){LocalDate min=LocalDate.of(1,1,1),max=LocalDate.of(9999,12,30);if(from!=null&&(from.isBefore(min)||from.isAfter(max))||to!=null&&(to.isBefore(min)||to.isAfter(max))||from!=null&&to!=null&&from.isAfter(to))throw new IllegalArgumentException("Khoảng ngày không hợp lệ.");}
    private static void begin(Connection c)throws SQLException{if(!c.getAutoCommit())throw new SQLException("Expected fresh connection");c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);c.setAutoCommit(false);}
    private static void rollback(Connection c,Exception e){try{c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}}
}
