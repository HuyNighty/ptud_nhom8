package com.quanlynhakhoa.quantri.taikhoan;

import com.quanlynhakhoa.model.taikhoan.KetQuaDanhSachTaiKhoan;
import com.quanlynhakhoa.enums.TrangThaiTaiKhoan;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.hethong.phanquyen.AuthorizationService;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.hethong.phanquyen.Permission;
import com.quanlynhakhoa.hethong.phanquyen.RolePermissionPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public final class AccountDirectoryService {
    @FunctionalInterface
    public interface ConnectionProvider {
        Connection open() throws SQLException;
    }

    private final DesktopSession session;
    private final ConnectionProvider connections;
    private final AuthorizationService authorization =
            new AuthorizationService(new RolePermissionPolicy());
    private final AccountDirectoryDao dao = new AccountDirectoryDao();

    public AccountDirectoryService(DesktopSession session, ConnectionProvider connections) {
        this.session = Objects.requireNonNull(session, "session");
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    public KetQuaDanhSachTaiKhoan search(String keyword, VaiTroHeThong role,
                                             TrangThaiTaiKhoan status) throws SQLException {
        authorization.require(session, Permission.ACCOUNT_MANAGE);
        String normalized = Objects.requireNonNull(keyword, "keyword").trim();
        if (normalized.length() > 80) {
            throw new IllegalArgumentException("Search keyword must have at most 80 characters");
        }
        try (Connection connection = connections.open()) {
            authorization.require(session, Permission.ACCOUNT_MANAGE);
            KetQuaDanhSachTaiKhoan result = dao.search(connection, normalized, role, status);
            authorization.require(session, Permission.ACCOUNT_MANAGE);
            return result;
        }
    }
}
