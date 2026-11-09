package com.quanlynhakhoa.service.hoadon;

import com.quanlynhakhoa.security.RolePermissionPolicy;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.dao.hoadon.HuyHoaDonDao;
import com.quanlynhakhoa.dao.hoadon.HoaDonDao;
import com.quanlynhakhoa.dao.hoadon.ConnectionProvider;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.enums.TrangThaiYeuCauHuyHoaDon;
import com.quanlynhakhoa.enums.TrangThaiHoaDon;
import com.quanlynhakhoa.model.hoadon.XemTruocHuyHoaDon;
import com.quanlynhakhoa.model.hoadon.ChiTietYeuCauHuy;
import com.quanlynhakhoa.model.hoadon.ThongTinYeuCauHuyHoaDon;
import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import com.quanlynhakhoa.model.hoadon.DuLieuChiTietHoaDon;
import com.quanlynhakhoa.model.hoadon.ThongTinThanhToan;
import com.quanlynhakhoa.model.hoadon.DongChiTietHoaDon;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public final class HuyHoaDonService {


    private final DesktopSession session;private final ConnectionProvider connections;
    private final HuyHoaDonDao dao;private final HoaDonDao invoices;private final InvoiceIdGenerator ids;
    private final AuthorizationService authorization=new AuthorizationService(new RolePermissionPolicy());
    public HuyHoaDonService(DesktopSession session,ConnectionProvider connections){this(session,connections,new HuyHoaDonDao(),new HoaDonDao(),new RandomInvoiceIdGenerator());}
    public HuyHoaDonService(DesktopSession session,ConnectionProvider connections,HuyHoaDonDao dao,HoaDonDao invoices,InvoiceIdGenerator ids) {
        this.session=Objects.requireNonNull(session);this.connections=Objects.requireNonNull(connections);this.dao=Objects.requireNonNull(dao);this.invoices=Objects.requireNonNull(invoices);this.ids=Objects.requireNonNull(ids);
    }
    private void require(Quyen permission){authorization.require(session,permission);}
    public List<ThongTinYeuCauHuyHoaDon> find(String keyword,LocalDate from,LocalDate to,TrangThaiYeuCauHuyHoaDon status)throws SQLException {
        require(Quyen.INVOICE_CANCEL_APPROVE);String text=keyword==null?"":keyword.trim();
        if(text.length()>100)throw new IllegalArgumentException("Từ khóa tối đa 100 ký tự.");dates(from,to);
        try(Connection c=connections.open()){require(Quyen.INVOICE_CANCEL_APPROVE);begin(c);
            try{List<ThongTinYeuCauHuyHoaDon> result=dao.find(c,text,from,to,status);require(Quyen.INVOICE_CANCEL_APPROVE);c.commit();return result;}
            catch(SQLException|RuntimeException e){rollback(c,e);throw e;}
        }
    }
    public XemTruocHuyHoaDon preview(String invoiceId)throws SQLException {
        require(Quyen.INVOICE_CANCEL_REQUEST);key(invoiceId);
        try(Connection c=connections.open()){require(Quyen.INVOICE_CANCEL_REQUEST);begin(c);
            try{DuLieuChiTietHoaDon detail=readInvoice(c,invoiceId);boolean pending=dao.pending(c,invoiceId);require(Quyen.INVOICE_CANCEL_REQUEST);c.commit();return new XemTruocHuyHoaDon(detail,pending);}
            catch(SQLException|RuntimeException e){rollback(c,e);throw e;}
        }
    }
    public ChiTietYeuCauHuy detail(String requestId)throws SQLException {
        require(Quyen.INVOICE_CANCEL_APPROVE);key(requestId);
        try(Connection c=connections.open()){require(Quyen.INVOICE_CANCEL_APPROVE);begin(c);
            try{ThongTinYeuCauHuyHoaDon request=dao.get(c,requestId);if(request==null)throw new IllegalArgumentException("Yêu cầu không tồn tại.");
                ChiTietYeuCauHuy result=new ChiTietYeuCauHuy(request,readInvoice(c,request.maHoaDon()));require(Quyen.INVOICE_CANCEL_APPROVE);c.commit();return result;}
            catch(SQLException|RuntimeException e){rollback(c,e);throw e;}
        }
    }
    public String request(String invoiceId,String reason)throws SQLException {
        require(Quyen.INVOICE_CANCEL_REQUEST);key(invoiceId);String normalized=reason(reason);String id=null;boolean committed=false;
        try(Connection c=connections.open()){require(Quyen.INVOICE_CANCEL_REQUEST);begin(c);
            try{dao.lockInvoice(c,invoiceId);ThongTinHoaDon invoice=invoices.getForPayment(c,invoiceId);
                if(invoice==null||invoice.trangThai()==TrangThaiHoaDon.DA_HUY)throw new IllegalArgumentException("Không gửi yêu cầu cho hóa đơn đã hủy/không tồn tại.");
                if(invoice.daThu().signum()!=0)throw new IllegalArgumentException("Hóa đơn đã có khoản thu; chưa hỗ trợ hoàn tiền nên không thể gửi yêu cầu hủy.");
                if(LanThanhToanService.pending(c,invoiceId)!=null)throw new IllegalArgumentException("Hóa đơn còn lần thu chờ đối soát; giải quyết trước khi yêu cầu hủy.");
                if(dao.pending(c,invoiceId))throw new IllegalArgumentException("Hóa đơn đã có yêu cầu đang chờ duyệt.");
                id=ids.next(c);key(id);dao.insert(c,id,invoiceId,session.accountId(),normalized);dao.audit(c,session.accountId(),id,"Gửi yêu cầu hủy");
                authorization.requireCurrent(session,Quyen.INVOICE_CANCEL_REQUEST,c);require(Quyen.INVOICE_CANCEL_REQUEST);c.commit();committed=true;
            }catch(SQLException|RuntimeException e){rollback(c,e);throw e;}
        }catch(SQLException e){if(committed)throw new CommittedCancellationException(id,e);if(e.getErrorCode()==2601||e.getErrorCode()==2627)throw new IllegalArgumentException("Trùng mã hoặc đã có yêu cầu chờ duyệt; tải lại để kiểm tra.",e);throw e;}
        return id;
    }
    public void decide(String requestId,boolean approve,String decisionReason)throws SQLException {
        require(Quyen.INVOICE_CANCEL_APPROVE);key(requestId);
        String normalized=approve&&(decisionReason==null||decisionReason.isBlank())?"Đồng ý hủy hóa đơn":reason(decisionReason);
        boolean committed=false;
        try(Connection c=connections.open()) {
            require(Quyen.INVOICE_CANCEL_APPROVE);
            if(!c.getAutoCommit())throw new SQLException("Expected fresh connection");
            String invoiceId=dao.invoiceId(c,requestId);begin(c);
            try{dao.lockInvoice(c,invoiceId);dao.lockRequest(c,requestId);
                ThongTinYeuCauHuyHoaDon request=dao.get(c,requestId);
                if(request==null||!invoiceId.equals(request.maHoaDon())||request.trangThai()!=TrangThaiYeuCauHuyHoaDon.CHO_DUYET)throw new IllegalArgumentException("Yêu cầu đã được xử lý hoặc không tồn tại.");
                if(session.accountId().equals(request.maTaiKhoanTao()))throw new IllegalArgumentException("Người gửi không được tự duyệt hoặc từ chối yêu cầu của mình.");
                ThongTinHoaDon invoice=invoices.getForPayment(c,invoiceId);
                if(invoice==null||invoice.trangThai()==TrangThaiHoaDon.DA_HUY)throw new IllegalArgumentException("Hóa đơn đã hủy/không tồn tại; cần đối soát.");
                if(approve){
                    if(LanThanhToanService.pending(c,invoiceId)!=null)throw new IllegalArgumentException("Còn lần thu chưa đối soát; chưa thể hủy hóa đơn.");
                    if(invoice.daThu().signum()!=0)throw new IllegalArgumentException("Hóa đơn có khoản thu hợp lệ. Chưa có quy trình hoàn tiền nên chưa thể duyệt hủy.");
                    dao.cancelInvoice(c,invoiceId);
                }
                // A rejection changes only the request: invoice/payment history remains untouched.
                dao.decide(c,requestId,session.accountId(),normalized,approve);dao.audit(c,session.accountId(),requestId,approve?"Duyệt hủy hóa đơn":"Từ chối hủy hóa đơn");
                authorization.requireCurrent(session,Quyen.INVOICE_CANCEL_APPROVE,c);require(Quyen.INVOICE_CANCEL_APPROVE);c.commit();committed=true;
            }catch(SQLException|RuntimeException e){rollback(c,e);throw e;}
        }catch(SQLException e){if(committed)throw new CommittedCancellationException(requestId,e);throw e;}
    }
    private DuLieuChiTietHoaDon readInvoice(Connection c,String id)throws SQLException {
        ThongTinHoaDon invoice=invoices.getForPayment(c,id);if(invoice==null)throw new IllegalArgumentException("Hóa đơn không tồn tại.");
        List<DongChiTietHoaDon> lines=invoices.lines(c,id);List<ThongTinThanhToan> payments=invoices.payments(c,id);
        if(TinhTienHoaDon.total(lines).compareTo(invoice.tongTien())!=0||TinhTienHoaDon.collected(payments).compareTo(invoice.daThu())!=0)throw new SQLException("Kết quả hóa đơn không nhất quán.");
        return new DuLieuChiTietHoaDon(invoice,lines,payments);
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
