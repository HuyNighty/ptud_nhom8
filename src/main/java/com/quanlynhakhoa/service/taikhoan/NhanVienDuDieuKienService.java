package com.quanlynhakhoa.service.taikhoan;

import com.quanlynhakhoa.dao.taikhoan.NhanVienDuDieuKienDao;
import com.quanlynhakhoa.model.taikhoan.KetQuaNhanVienDuDieuKien;
import com.quanlynhakhoa.hethong.phanquyen.AuthorizationService;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.hethong.phanquyen.RolePermissionPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** Restricts eligible-employee discovery to account managers. */
public final class NhanVienDuDieuKienService {
    @FunctionalInterface public interface ConnectionProvider {
        Connection open() throws SQLException;
    }

    private final DesktopSession session;
    private final ConnectionProvider connections;
    private final AuthorizationService authorization =
            new AuthorizationService(new RolePermissionPolicy());
    private final NhanVienDuDieuKienDao dao = new NhanVienDuDieuKienDao();

    public NhanVienDuDieuKienService(DesktopSession session, ConnectionProvider connections) {
        this.session = Objects.requireNonNull(session, "session");
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    public KetQuaNhanVienDuDieuKien listEligible() throws SQLException {
        authorization.require(session, Quyen.ACCOUNT_MANAGE);
        try (Connection connection = connections.open()) {
            authorization.require(session, Quyen.ACCOUNT_MANAGE);
            KetQuaNhanVienDuDieuKien result = dao.findEligible(connection);
            authorization.require(session, Quyen.ACCOUNT_MANAGE);
            return result;
        }
    }
}
