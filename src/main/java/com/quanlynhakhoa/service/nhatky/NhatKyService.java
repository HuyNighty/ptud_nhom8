package com.quanlynhakhoa.service.nhatky;

import com.quanlynhakhoa.dao.nhatky.NhatKyDao;
import com.quanlynhakhoa.model.nhatky.KetQuaNhatKy;
import com.quanlynhakhoa.model.nhatky.BoLocNhatKy;
import com.quanlynhakhoa.hethong.phanquyen.AuthorizationService;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.hethong.phanquyen.RolePermissionPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** Enforce AUDIT_READ before opening JDBC and after reading it (including logout races). */
public final class NhatKyService {
    @FunctionalInterface
    public interface ConnectionProvider {
        Connection open() throws SQLException;
    }

    private final DesktopSession session;
    private final ConnectionProvider connections;
    private final NhatKyDao dao;
    private final AuthorizationService authorization =
            new AuthorizationService(new RolePermissionPolicy());

    public NhatKyService(DesktopSession session, ConnectionProvider connections) {
        this(session, connections, new NhatKyDao());
    }

    NhatKyService(DesktopSession session, ConnectionProvider connections, NhatKyDao dao) {
        this.session = Objects.requireNonNull(session, "session");
        this.connections = Objects.requireNonNull(connections, "connections");
        this.dao = Objects.requireNonNull(dao, "dao");
    }

    public KetQuaNhatKy search(BoLocNhatKy filter) throws SQLException {
        authorization.require(session, Quyen.AUDIT_READ);
        Objects.requireNonNull(filter, "filter");
        try (Connection connection = connections.open()) {
            authorization.require(session, Quyen.AUDIT_READ);
            KetQuaNhatKy result = dao.search(connection, filter);
            authorization.require(session, Quyen.AUDIT_READ);
            return result;
        }
    }
}
