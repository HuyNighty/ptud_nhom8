package com.quanlynhakhoa.quantri.nhanvien;

import com.quanlynhakhoa.model.nhanvien.KetQuaDanhSachNhanVien;
import com.quanlynhakhoa.enums.TrangThaiNhanVien;
import com.quanlynhakhoa.hethong.phanquyen.AuthorizationService;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.hethong.phanquyen.RolePermissionPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** Permission enforcement belongs here, not merely in the Swing menu. */
public final class EmployeeDirectoryService {
    @FunctionalInterface
    public interface ConnectionProvider {
        Connection open() throws SQLException;
    }

    private final DesktopSession session;
    private final ConnectionProvider connections;
    private final AuthorizationService authorization =
            new AuthorizationService(new RolePermissionPolicy());
    private final EmployeeDirectoryDao dao = new EmployeeDirectoryDao();

    public EmployeeDirectoryService(DesktopSession session, ConnectionProvider connections) {
        this.session = Objects.requireNonNull(session, "session");
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    public KetQuaDanhSachNhanVien search(String keyword, TrangThaiNhanVien status) throws SQLException {
        authorization.require(session, Quyen.WORKFORCE_READ);
        String normalized = Objects.requireNonNull(keyword, "keyword").trim();
        if (normalized.length() > 80) {
            throw new IllegalArgumentException("Từ khóa tìm kiếm tối đa 80 ký tự.");
        }
        try (Connection connection = connections.open()) {
            authorization.require(session, Quyen.WORKFORCE_READ);
            KetQuaDanhSachNhanVien result = dao.search(connection, normalized, status);
            authorization.require(session, Quyen.WORKFORCE_READ);
            return result;
        }
    }
}
