package com.quanlynhakhoa.examination;

import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Objects;

/** Checks access before and after JDBC; does not start or complete an examination. */
public final class ExamQueueService {
    @FunctionalInterface public interface ConnectionProvider {
        Connection open() throws SQLException;
    }

    private final DesktopSession session;
    private final ConnectionProvider connections;
    private final ExamQueueDao dao;
    private final AuthorizationService auth = new AuthorizationService(new RolePermissionPolicy());

    public ExamQueueService(DesktopSession session, ConnectionProvider connections) {
        this(session, connections, new ExamQueueDao());
    }

    public ExamQueueService(DesktopSession session, ConnectionProvider connections, ExamQueueDao dao) {
        this.session = Objects.requireNonNull(session, "session");
        this.connections = Objects.requireNonNull(connections, "connections");
        this.dao = Objects.requireNonNull(dao, "dao");
    }

    public ExamQueueDao.Result load(LocalDate day) throws SQLException {
        Objects.requireNonNull(day, "day");
        requireQueueAccess();
        try (Connection connection = connections.open()) {
            requireQueueAccess();
            ExamQueueDao.Result rows = dao.listForDay(connection, day, session.role(), session.accountId());
            requireQueueAccess();
            return rows;
        }
    }

    private void requireQueueAccess() {
        auth.require(session, Permission.MEDICAL_RECORD_READ);
        if (session.role() != Role.DOCTOR && session.role() != Role.SYSTEM_ADMIN) {
            throw new AccessDeniedException();
        }
    }
}
