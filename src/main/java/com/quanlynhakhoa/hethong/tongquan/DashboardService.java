package com.quanlynhakhoa.hethong.tongquan;

import com.quanlynhakhoa.hethong.phanquyen.AccessDeniedException;
import com.quanlynhakhoa.hethong.phanquyen.AuthorizationService;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.hethong.phanquyen.Permission;
import com.quanlynhakhoa.hethong.phanquyen.RolePermissionPolicy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.*;
import java.time.LocalDateTime;
import com.quanlynhakhoa.hethong.dieuhuong.Module;
import com.quanlynhakhoa.enums.VaiTroHeThong;

public final class DashboardService {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final DesktopSession session;
    private final ConnectionProvider connections;
    private final AuthorizationService authorization =
            new AuthorizationService(new RolePermissionPolicy());
    private final DashboardDao dao = new DashboardDao();

    @FunctionalInterface
    public interface ConnectionProvider {
        Connection open() throws SQLException;
    }

    public record Summary(LocalDate date, Long patients, Long appointments,
                          Long inProgressExams, BigDecimal collected) { }

    public DashboardService(DesktopSession session, ConnectionProvider connections) {
        this.session = Objects.requireNonNull(session, "session");
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    public Summary load() throws SQLException {
        requireActive();
        LocalDate today = LocalDate.now(CLINIC_ZONE);
        boolean canPatients = authorization.can(session, Permission.PATIENT_READ);
        boolean canAppointments = authorization.can(session, Permission.APPOINTMENT_READ);
        boolean canExams = authorization.can(session, Permission.MEDICAL_RECORD_READ);
        boolean canBilling = authorization.can(session, Permission.BILLING_READ);

        if (!canPatients && !canAppointments && !canExams && !canBilling) {
            return new Summary(today, null, null, null, null);
        }

        try (Connection connection = connections.open()) {
            Long patients = null;
            Long appointments = null;
            Long exams = null;
            BigDecimal collected = null;
            if (canPatients) {
                requireActive();
                patients = dao.countPatients(connection);
            }
            if (canAppointments) {
                requireActive();
                appointments = dao.countAppointments(connection, today);
            }
            if (canExams) {
                requireActive();
                exams = session.role()==VaiTroHeThong.DOCTOR
                        ? new DashboardDetailsDao().count(connection,"SELECT COUNT_BIG(*) FROM dbo.PhieuKham p JOIN dbo.TaiKhoan a ON a.maNhanVien=p.maNhanVien JOIN dbo.NhanVien n ON n.maNhanVien=a.maNhanVien WHERE p.trangThai='DANG_KHAM' AND a.maTaiKhoan=? AND a.vaiTro='DOCTOR' AND a.trangThai='HOAT_DONG' AND n.chucDanh='BS' AND n.trangThai='DANG_LAM_VIEC'",session.accountId())
                        : dao.countInProgress(connection);
            }
            if (canBilling) {
                requireActive();
                collected = dao.collectedToday(connection, today);
            }
            requireActive();
            return new Summary(today, patients, appointments, exams, collected);
        }
    }

    public record Appointment(String id, LocalDateTime time, String patient, String doctor, String status) { }
    public record WorkItem(String label, long count, Module module, String context) { }
    public record StockAlert(String kind, String item, String detail, String context) { }
    public record Day(LocalDate date, Long visits, BigDecimal collected) { }
    public record Overview(Summary summary, List<Appointment> appointments, List<WorkItem> work,
                           List<StockAlert> alerts, List<Day> days, List<String> warnings) {
        public Overview {
            appointments=List.copyOf(appointments);work=List.copyOf(work);alerts=List.copyOf(alerts);
            days=List.copyOf(days);warnings=List.copyOf(warnings);
        }
    }
    public boolean can(Permission permission) { return authorization.can(session,permission); }

    public Overview loadOverview() throws SQLException {
        requireActive();
        Summary summary=load();
        LocalDate today=summary.date();
        DashboardDetailsDao details=new DashboardDetailsDao();
        List<Appointment> appointments=List.of();List<WorkItem> work=new ArrayList<>();
        List<StockAlert> alerts=List.of();List<Day> days=new ArrayList<>();List<String> warnings=new ArrayList<>();
        boolean visitAccess=can(Permission.MEDICAL_RECORD_READ), moneyAccess=can(Permission.REVENUE_REPORT_READ);
        Map<LocalDate,Long> visits=null;Map<LocalDate,BigDecimal> money=null;
        try(Connection c=connections.open()) {
            if(can(Permission.APPOINTMENT_READ)) {
                try{appointments=details.appointments(c,today);}catch(SQLException e){warnings.add("Chưa tải được lịch hẹn");}
            }
            if(can(Permission.INVOICE_CANCEL_APPROVE)) {
                try{work.add(new WorkItem("Hủy hóa đơn chờ bạn duyệt",details.count(c,"SELECT COUNT_BIG(*) FROM dbo.YeuCauHuyHoaDon WHERE trangThai='CHO_DUYET' AND maTaiKhoanTao<>?",session.accountId()),Module.INVOICE_CANCELLATIONS,""));}
                catch(SQLException e){warnings.add("Chưa tải được yêu cầu hủy");}
            }
            if(can(Permission.INVENTORY_MANAGE)) {
                try{work.add(new WorkItem("Cấp phát chờ duyệt",details.count(c,"SELECT COUNT_BIG(*) FROM dbo.YeuCauCapPhat WHERE trangThai='CHO_DUYET'",null),Module.INVENTORY,"supply"));}
                catch(SQLException e){warnings.add("Chưa tải được yêu cầu cấp phát");}
                try{work.add(new WorkItem("Kiểm kê chờ bạn duyệt",details.count(c,"SELECT COUNT_BIG(*) FROM dbo.PhieuDieuChinhKho WHERE loai='KIEM_KE' AND trangThai='CHO_DUYET' AND maTaiKhoanTao<>?",session.accountId()),Module.INVENTORY,"stocktake"));}
                catch(SQLException e){warnings.add("Chưa tải được kiểm kê");}
            } else if(can(Permission.SUPPLY_REQUEST)) {
                try{work.add(new WorkItem("Yêu cầu cấp phát của bạn đang chờ",details.count(c,"SELECT COUNT_BIG(*) FROM dbo.YeuCauCapPhat y JOIN dbo.TaiKhoan a ON a.maNhanVien=y.maNhanVienYeuCau WHERE a.maTaiKhoan=? AND y.trangThai IN ('CHO_DUYET','DA_DUYET')",session.accountId()),Module.SUPPLY_REQUESTS,""));}
                catch(SQLException e){warnings.add("Chưa tải được yêu cầu của bạn");}
            }
            if(can(Permission.INVENTORY_READ)) {
                try{alerts=details.stock(c,today);}catch(SQLException e){warnings.add("Chưa tải được cảnh báo kho");}
            }
            if(visitAccess) {
                try{visits=details.visits(c,today,session.role()==VaiTroHeThong.DOCTOR?session.accountId():null);}
                catch(SQLException e){warnings.add("Chưa tải được biểu đồ lượt khám");}
            }
            if(moneyAccess) {
                try{money=details.collections(c,today);}catch(SQLException e){warnings.add("Chưa tải được biểu đồ tiền thu");}
            }
        }
        for(int i=6;i>=0;i--){LocalDate date=today.minusDays(i);days.add(new Day(date,visits==null?null:visits.getOrDefault(date,0L),money==null?null:money.getOrDefault(date,BigDecimal.ZERO)));}
        requireActive();
        return new Overview(summary,appointments,work,alerts,days,warnings);
    }

    private void requireActive() {
        if (!session.isActive()) throw new AccessDeniedException();
    }
}
