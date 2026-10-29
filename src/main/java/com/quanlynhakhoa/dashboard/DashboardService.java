package com.quanlynhakhoa.dashboard;

import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

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
                exams = dao.countInProgress(connection);
            }
            if (canBilling) {
                requireActive();
                collected = dao.collectedToday(connection, today);
            }
            requireActive();
            return new Summary(today, patients, appointments, exams, collected);
        }
    }

    private void requireActive() {
        if (!session.isActive()) throw new AccessDeniedException();
    }
}
