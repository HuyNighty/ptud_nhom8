package com.quanlynhakhoa.billing.service;

import com.quanlynhakhoa.billing.dao.*;
import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.security.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/** Shared by invoice payment, debt collection and the dedicated payment page. */
public final class PaymentService {
    private final DesktopSession session;
    private final ConnectionProvider connections;
    private final PaymentDao dao;
    private final HoaDonDao invoices;
    private final InvoiceCreateDao accounts;
    private final AuthorizationService authorization=new AuthorizationService(new RolePermissionPolicy());
    public PaymentService(DesktopSession session,ConnectionProvider connections){this(session,connections,new PaymentDao(),new HoaDonDao(),new InvoiceCreateDao());}
    public PaymentService(DesktopSession session,ConnectionProvider connections,PaymentDao dao,HoaDonDao invoices,InvoiceCreateDao accounts) {
        this.session=Objects.requireNonNull(session);this.connections=Objects.requireNonNull(connections);
        this.dao=Objects.requireNonNull(dao);this.invoices=Objects.requireNonNull(invoices);this.accounts=Objects.requireNonNull(accounts);
    }
    public void collect(PaymentRequest request)throws SQLException {
        require();validate(request);boolean committed=false;
        try(Connection c=connections.open()) {
            require();if(!c.getAutoCommit())throw new SQLException("Expected fresh connection");
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);c.setAutoCommit(false);
            try {
                dao.requirePermissions(c);dao.lockInvoice(c,request.invoiceId());
                String employee=accounts.employee(c,session.accountId());
                int existing=0;
                for(PaymentRequest.Part part:request.parts()) {
                    ThanhToan prior=dao.existing(c,part.id());
                    if(prior!=null){
                        if(!matches(prior,request.invoiceId(),employee,part))throw new IllegalArgumentException("Mã lần thu đã tồn tại với dữ liệu khác hoặc đã hủy.");
                        existing++;
                    }
                }
                if(existing>0){
                    if(existing!=request.parts().size())throw new IllegalArgumentException("Kết quả lần thu không đầy đủ, cần đối soát.");
                    require();c.commit();committed=true;
                } else {
                    HoaDon invoice=invoices.getForPayment(c,request.invoiceId());
                    if(invoice==null||invoice.trangThai()==TrangThaiHoaDon.DA_HUY)throw new IllegalArgumentException("Không được thu trên hóa đơn đã hủy/không tồn tại.");
                    if(invoice.conNo().signum()<=0||!invoice.canhBao().isEmpty())throw new IllegalArgumentException("Hóa đơn đã hết nợ hoặc dữ liệu cần đối soát.");
                    if(request.amount().compareTo(invoice.conNo())>0)throw new IllegalArgumentException("Số tiền thu vượt số còn nợ hiện tại.");
                    dao.requireNoPendingCancellation(c,request.invoiceId());
                    for(PaymentRequest.Part part:request.parts()) {
                        if(part.method()==ThanhToan.PhuongThuc.CHUYEN_KHOAN)dao.requireNewTransferReference(c,part.reference());
                        dao.insert(c,request.invoiceId(),employee,part);
                    }
                    TrangThaiHoaDon status=request.amount().compareTo(invoice.conNo())==0?TrangThaiHoaDon.DA_THANH_TOAN:TrangThaiHoaDon.THANH_TOAN_MOT_PHAN;
                    dao.updateStatus(c,request.invoiceId(),status);dao.audit(c,session.accountId(),request.invoiceId());
                    require();c.commit();committed=true;
                }
            }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}throw e;}
        }catch(SQLException e){if(committed)throw new CommittedPaymentException(e);throw e;}
    }
    public boolean pendingCancellation(String invoiceId)throws SQLException {
        require();key(invoiceId);try(Connection c=connections.open()){require();boolean result=dao.hasPendingCancellation(c,invoiceId);require();return result;}
    }
    public static final class CommittedPaymentException extends SQLException {
        public CommittedPaymentException(SQLException e){super("Đã thu thành công; lỗi đóng kết nối.",e);}
    }
    private void require(){authorization.require(session,Permission.BILLING_MANAGE);}
    private static boolean equalMoney(BigDecimal a,BigDecimal b){return a==null?b==null:b!=null&&a.compareTo(b)==0;}
    private static boolean matches(ThanhToan prior,String invoice,String employee,PaymentRequest.Part part) {
        return invoice.equals(prior.maHoaDon())&&employee.equals(prior.maNhanVien())&&prior.trangThai()==ThanhToan.TrangThai.THANH_CONG
                &&prior.phuongThuc()==part.method()&&equalMoney(prior.soTien(),part.amount())
                &&Objects.equals(prior.maGiaoDich(),part.reference())&&equalMoney(prior.tienKhachDua(),part.customerTender());
    }
    public static void validate(PaymentRequest request) {
        Objects.requireNonNull(request,"Khoản thu");key(request.invoiceId());
        if(request.amount()==null||request.amount().signum()<=0||request.amount().stripTrailingZeros().scale()>0)throw new IllegalArgumentException("Số tiền thu phải là VND nguyên lớn hơn 0.");
        if(request.parts().isEmpty()||request.parts().size()>2)throw new IllegalArgumentException("Chọn tiền mặt, chuyển khoản hoặc phối hợp hai phương thức.");
        BigDecimal total=BigDecimal.ZERO;Set<String> ids=new HashSet<>();Set<ThanhToan.PhuongThuc> methods=new HashSet<>();
        for(PaymentRequest.Part part:request.parts()) {
            key(part.id());money(part.amount(),true);
            if(!ids.add(part.id())||!methods.add(part.method()))throw new IllegalArgumentException("Trùng mã lần thu hoặc phương thức.");
            if(part.method()==ThanhToan.PhuongThuc.TIEN_MAT){
                money(part.customerTender(),false);if(part.customerTender().compareTo(part.amount())<0)throw new IllegalArgumentException("Tiền khách đưa chưa đủ phần tiền mặt.");
                if(part.reference()!=null)throw new IllegalArgumentException("Tiền mặt không có mã chuyển khoản.");
            }else if(part.method()==ThanhToan.PhuongThuc.CHUYEN_KHOAN){
                if(!request.transferConfirmed())throw new IllegalArgumentException("Cần xác nhận đã nhận chuyển khoản thực tế.");
                String ref=part.reference();if(ref==null||ref.isBlank()||ref.length()>50||!ref.equals(ref.trim()))throw new IllegalArgumentException("Mã giao dịch chuyển khoản cần 1–50 ký tự.");
                for(char ch:ref.toCharArray())if(ch<33||ch>126)throw new IllegalArgumentException("Mã giao dịch chỉ dùng ASCII không khoảng trắng.");
                if(part.customerTender()!=null)throw new IllegalArgumentException("Chuyển khoản không có tiền khách đưa.");
            }else throw new IllegalArgumentException("Phương thức thu không được hỗ trợ ở màn hình này.");
            total=total.add(part.amount());
        }
        if(total.compareTo(request.amount())!=0)throw new IllegalArgumentException("Tổng phân bổ phải bằng số tiền thu.");
    }
    private static void key(String text){if(text==null||text.isBlank()||text.length()>10)throw new IllegalArgumentException("Mã cần 1–10 ký tự.");for(char ch:text.toCharArray())if(ch<33||ch>126)throw new IllegalArgumentException("Mã phải là ASCII không khoảng trắng.");}
    private static void money(BigDecimal value,boolean positive){if(value==null||value.signum()<0||(positive&&value.signum()==0)||value.stripTrailingZeros().scale()>0||value.compareTo(new BigDecimal("999999999999999999"))>0)throw new IllegalArgumentException("Tiền phải là VND nguyên trong giới hạn DECIMAL(18,0).");}
}
