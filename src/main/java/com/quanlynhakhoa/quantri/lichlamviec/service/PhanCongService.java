package com.quanlynhakhoa.quantri.lichlamviec.service;

import com.quanlynhakhoa.quantri.lichlamviec.dao.PhanCongDao;
import com.quanlynhakhoa.quantri.lichlamviec.model.*;
import com.quanlynhakhoa.hethong.phanquyen.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public final class PhanCongService {
    @FunctionalInterface public interface Connections { Connection open() throws SQLException; }
    public record DuLieu(List<PhanCong> rows, List<NhanVienChon> employees, List<CaLamViec> shifts, String message) {
        public DuLieu {rows=List.copyOf(rows);employees=List.copyOf(employees);shifts=List.copyOf(shifts);}
    }
    private static final java.security.SecureRandom RANDOM=new java.security.SecureRandom();
    private final DesktopSession session;
    private final Connections connections;
    private final PhanCongDao dao=new PhanCongDao();
    private final AuthorizationService auth=new AuthorizationService(new RolePermissionPolicy());
    public PhanCongService(DesktopSession session,Connections connections){this.session=Objects.requireNonNull(session);this.connections=Objects.requireNonNull(connections);}
    public boolean canManage(){return auth.can(session,Permission.WORKFORCE_MANAGE);}
    public boolean isActive(){return session.isActive();}
    public DuLieu management(LocalDate from,LocalDate to,String keyword,String employee) throws SQLException {
        auth.require(session,Permission.WORKFORCE_READ);range(from,to);
        try(var c=connections.open()){
            var data=new DuLieu(dao.schedule(c,from,to,employee),dao.employees(c,keyword==null?"":keyword.trim()),dao.shifts(c),"");
            auth.require(session,Permission.WORKFORCE_READ);return data;
        }
    }
    /** No caller-supplied employee ID: resolve only the trusted account association. */
    public DuLieu mine(LocalDate from,LocalDate to) throws SQLException {
        auth.require(session,Permission.SCHEDULE_SELF_READ);range(from,to);
        try(var c=connections.open()){
            String employee=dao.employeeForAccount(c,session.accountId());
            var rows=employee==null?List.<PhanCong>of():dao.schedule(c,from,to,employee);
            auth.require(session,Permission.SCHEDULE_SELF_READ);
            return new DuLieu(rows,List.of(),List.of(),employee==null?"Tài khoản chưa liên kết với hồ sơ nhân viên.":"");
        }
    }
    public String create(String employee,String shift,LocalDate day,String task) throws SQLException {
        auth.require(session,Permission.WORKFORCE_MANAGE);
        String nv=required(employee,10,"Chọn nhân viên."),ca=required(shift,10,"Chọn ca làm việc.");
        range(day,day);String work=required(task,200,"Nhiệm vụ phải có 1–200 ký tự.");
        return write(c->{
            dao.lockEmployee(c,nv,day,true);CaLamViec chosen=dao.lockedShift(c,ca);
            checkAssignment(chosen,dao.schedule(c,day,day,nv),dao.maximumPerDay(c));
            StringBuilder code=new StringBuilder();for(int i=0;i<10;i++)code.append("0123456789ABCDEFGHJKMNPQRSTVWXYZ".charAt(RANDOM.nextInt(32)));
            String id=code.toString();
            dao.insert(c,id,nv,ca,day,work);dao.audit(c,session.accountId(),id,"Tạo phân công");return id;
        });
    }
    /** A new shift and its assignment commit together or are both rolled back. */
    public String createWithNewShift(String employee, String shiftName, java.time.LocalTime start,
                                     java.time.LocalTime end, LocalDate day, String task) throws SQLException {
        auth.require(session, Permission.WORKFORCE_MANAGE);
        String nv = required(employee, 10, "Chọn nhân viên.");
        String name = required(shiftName, 50, "Tên ca phải có 1–50 ký tự.");
        range(day, day);
        String work = required(task, 200, "Nhiệm vụ phải có 1–200 ký tự.");
        if (start == null || end == null || !start.isBefore(end))
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu; không hỗ trợ ca qua đêm.");
        if (start.getSecond() != 0 || start.getNano() != 0 || end.getSecond() != 0 || end.getNano() != 0)
            throw new IllegalArgumentException("Nhập giờ theo HH:mm, không nhập giây.");
        return write(c -> {
            dao.lockEmployee(c, nv, day, true);
            CaLamViec shift = new CaLamViec(newCode(), name, start, end);
            checkAssignment(shift, dao.schedule(c, day, day, nv), dao.maximumPerDay(c));
            dao.insertShift(c, shift);
            String id = newCode();
            dao.insert(c, id, nv, shift.id(), day, work);
            dao.audit(c, session.accountId(), id, "Tạo ca mới và phân công");
            return id;
        });
    }
    private static String newCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 10; i++) code.append("0123456789ABCDEFGHJKMNPQRSTVWXYZ".charAt(RANDOM.nextInt(32)));
        return code.toString();
    }
    /** PO APPROVED 01/11/2026: UI hint; DAO conditional UPDATE remains authoritative. */
    public static boolean canCancelStatus(String status) { return "CHO_HIEU_LUC".equals(status); }
    public void cancel(String id,String reason) throws SQLException {
        auth.require(session,Permission.WORKFORCE_MANAGE);
        String key=required(id,10,"Chọn phân công."),why=required(reason,300,"Lý do hủy phải có 1–300 ký tự.");
        write(c->{String nv=dao.assignmentEmployee(c,key);dao.lockEmployee(c,nv,null,false);dao.cancel(c,key,why);dao.audit(c,session.accountId(),key,"Hủy phân công");return key;});
    }
    private interface Work {String run(Connection c) throws SQLException;}
    private String write(Work work) throws SQLException {
        auth.require(session,Permission.WORKFORCE_MANAGE);
        try(var c=connections.open()){
            if(!c.getAutoCommit())throw new SQLException("Cần kết nối JDBC mới.");
            dao.requireDevDatabase(c); // Guard BEFORE any write or transaction.
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);c.setAutoCommit(false);
            boolean committing=false;
            try {
                auth.require(session,Permission.WORKFORCE_MANAGE);String result=work.run(c);
                auth.require(session,Permission.WORKFORCE_MANAGE);committing=true;c.commit();return result;
            } catch(SQLException|RuntimeException e){
                try{c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}
                if(committing)throw new SQLException("Chưa xác định kết quả lưu. Hãy tải lại trước khi thử lại.",e);
                if(e instanceof SQLException sql && (sql.getErrorCode()==2601||sql.getErrorCode()==2627))throw new IllegalArgumentException("Phân công hoặc mã đã tồn tại. Hãy tải lại.");
                throw e;
            }
        }
    }
    public static void checkAssignment(CaLamViec chosen,List<PhanCong> existing,int maximum){
        if(!chosen.batDau().isBefore(chosen.ketThuc()))throw new IllegalArgumentException("Ca không hợp lệ; không hỗ trợ ca qua đêm.");
        var active=existing.stream().filter(p->!"DA_HUY".equals(p.trangThai())).toList();
        if(active.size()>=maximum)throw new IllegalArgumentException("Nhân viên đã đủ số ca tối đa trong ngày.");
        if(active.stream().anyMatch(p->chosen.batDau().isBefore(p.ca().ketThuc())&&p.ca().batDau().isBefore(chosen.ketThuc())))throw new IllegalArgumentException("Ca làm việc bị trùng giờ với phân công hiện có.");
    }
    public static void range(LocalDate from,LocalDate to){
        if(from==null||to==null||from.isAfter(to)||from.getYear()<1||to.getYear()>9999)throw new IllegalArgumentException("Khoảng ngày không hợp lệ (yyyy-MM-dd).");
    }
    private static String required(String s,int max,String error){s=s==null?"":s.trim();if(s.isEmpty()||s.length()>max)throw new IllegalArgumentException(error);return s;}
}
