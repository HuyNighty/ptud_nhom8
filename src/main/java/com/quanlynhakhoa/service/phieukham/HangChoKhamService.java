package com.quanlynhakhoa.service.phieukham;

import com.quanlynhakhoa.dao.phieukham.HangChoKhamDao;
import com.quanlynhakhoa.model.phieukham.KetQuaHangChoKham;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Objects;

/** Checks access before and after JDBC; does not start or complete an examination. */
public final class HangChoKhamService {
    @FunctionalInterface public interface ConnectionProvider {
        Connection open() throws SQLException;
    }

    private final DesktopSession session;
    private final ConnectionProvider connections;
    private final HangChoKhamDao dao;
    private final AuthorizationService auth = new AuthorizationService(new RolePermissionPolicy());

    public HangChoKhamService(DesktopSession session, ConnectionProvider connections) {
        this(session, connections, new HangChoKhamDao());
    }

    public HangChoKhamService(DesktopSession session, ConnectionProvider connections, HangChoKhamDao dao) {
        this.session = Objects.requireNonNull(session, "session");
        this.connections = Objects.requireNonNull(connections, "connections");
        this.dao = Objects.requireNonNull(dao, "dao");
    }

    public KetQuaHangChoKham load(LocalDate day) throws SQLException {
        Objects.requireNonNull(day, "day");
        requireQueueAccess();
        try (Connection connection = connections.open()) {
            requireQueueAccess();
            KetQuaHangChoKham rows = dao.listForDay(connection, day, session.role(), session.accountId());
            requireQueueAccess();
            return rows;
        }
    }

    private void requireQueueAccess() {
        auth.require(session, Quyen.MEDICAL_RECORD_READ);
        if (session.role() != VaiTroHeThong.DOCTOR && session.role() != VaiTroHeThong.SYSTEM_ADMIN) {
            throw new AccessDeniedException();
        }
    }
}
