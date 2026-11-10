package com.quanlynhakhoa.service.tongquan;

import com.quanlynhakhoa.dao.tongquan.TongQuanDao;
import com.quanlynhakhoa.dao.tongquan.ChiTietTongQuanDao;
import com.quanlynhakhoa.model.tongquan.TomTatTongQuan;
import com.quanlynhakhoa.model.tongquan.SoLieuNgay;
import com.quanlynhakhoa.model.tongquan.LichHenTongQuan;
import com.quanlynhakhoa.model.tongquan.DuLieuTongQuan;
import com.quanlynhakhoa.model.tongquan.CongViecTongQuan;
import com.quanlynhakhoa.model.tongquan.CanhBaoTonKho;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.*;
import com.quanlynhakhoa.enums.Module;
import com.quanlynhakhoa.enums.VaiTroHeThong;

public final class TongQuanService {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final DesktopSession session;
    private final ConnectionProvider connections;
    private final AuthorizationService authorization =
            new AuthorizationService(new RolePermissionPolicy());
    private final TongQuanDao dao = new TongQuanDao();

    @FunctionalInterface
    public interface ConnectionProvider {
        Connection open() throws SQLException;
    }



    public TongQuanService(DesktopSession session, ConnectionProvider connections) {
        this.session = Objects.requireNonNull(session, "session");
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    public TomTatTongQuan load() throws SQLException {
        requireActive();
        LocalDate today = LocalDate.now(CLINIC_ZONE);
        boolean canPatients = authorization.can(session, Quyen.PATIENT_READ);
        boolean canAppointments = authorization.can(session, Quyen.APPOINTMENT_READ);
        boolean canExams = authorization.can(session, Quyen.MEDICAL_RECORD_READ);
        boolean canBilling = authorization.can(session, Quyen.BILLING_READ);

        if (!canPatients && !canAppointments && !canExams && !canBilling) {
            return new TomTatTongQuan(today, null, null, null, null);
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
                        ? new ChiTietTongQuanDao().demPhieuKhamDangXuLyCuaBacSi(connection,session.accountId())
                        : dao.countInProgress(connection);
            }
            if (canBilling) {
                requireActive();
                collected = dao.collectedToday(connection, today);
            }
            requireActive();
            return new TomTatTongQuan(today, patients, appointments, exams, collected);
        }
    }






    public boolean can(Quyen permission) { return authorization.can(session,permission); }

    public DuLieuTongQuan loadOverview() throws SQLException {
        requireActive();
        TomTatTongQuan summary=load();
        LocalDate today=summary.date();
        ChiTietTongQuanDao details=new ChiTietTongQuanDao();
        List<LichHenTongQuan> appointments=List.of();List<CongViecTongQuan> work=new ArrayList<>();
        List<CanhBaoTonKho> alerts=List.of();List<SoLieuNgay> days=new ArrayList<>();List<String> warnings=new ArrayList<>();
        boolean visitAccess=can(Quyen.MEDICAL_RECORD_READ), moneyAccess=can(Quyen.REVENUE_REPORT_READ);
        Map<LocalDate,Long> visits=null;Map<LocalDate,BigDecimal> money=null;
        try(Connection c=connections.open()) {
            if(can(Quyen.APPOINTMENT_READ)) {
                try{appointments=details.appointments(c,today);}catch(SQLException e){warnings.add("Chưa tải được lịch hẹn");}
            }
            if(can(Quyen.INVOICE_CANCEL_APPROVE)) {
                try{work.add(new CongViecTongQuan("Hủy hóa đơn chờ bạn duyệt",details.demYeuCauHuyCanDuyet(c,session.accountId()),Module.INVOICE_CANCELLATIONS,""));}
                catch(SQLException e){warnings.add("Chưa tải được yêu cầu hủy");}
            }
            if(can(Quyen.INVENTORY_MANAGE)) {
                try{work.add(new CongViecTongQuan("Cấp phát chờ duyệt",details.demYeuCauCapPhatChoDuyet(c),Module.INVENTORY,"supply"));}
                catch(SQLException e){warnings.add("Chưa tải được yêu cầu cấp phát");}
                try{work.add(new CongViecTongQuan("Kiểm kê chờ bạn duyệt",details.demKiemKeCanDuyet(c,session.accountId()),Module.INVENTORY,"stocktake"));}
                catch(SQLException e){warnings.add("Chưa tải được kiểm kê");}
            } else if(can(Quyen.SUPPLY_REQUEST)) {
                try{work.add(new CongViecTongQuan("Yêu cầu cấp phát của bạn đang chờ",details.demYeuCauCapPhatCuaTaiKhoan(c,session.accountId()),Module.SUPPLY_REQUESTS,""));}
                catch(SQLException e){warnings.add("Chưa tải được yêu cầu của bạn");}
            }
            if(can(Quyen.INVENTORY_READ)) {
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
        for(int i=6;i>=0;i--){LocalDate date=today.minusDays(i);days.add(new SoLieuNgay(date,visits==null?null:visits.getOrDefault(date,0L),money==null?null:money.getOrDefault(date,BigDecimal.ZERO)));}
        requireActive();
        return new DuLieuTongQuan(summary,appointments,work,alerts,days,warnings);
    }

    private void requireActive() {
        if (!session.isActive()) throw new AccessDeniedException();
    }
}
