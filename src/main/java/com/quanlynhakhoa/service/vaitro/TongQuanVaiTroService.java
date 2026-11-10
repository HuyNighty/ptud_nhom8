package com.quanlynhakhoa.service.vaitro;

import com.quanlynhakhoa.dao.vaitro.TongQuanVaiTroDao;
import com.quanlynhakhoa.model.vaitro.TongQuanVaiTro;
import com.quanlynhakhoa.service.taikhoan.DanhSachTaiKhoanService;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.hethong.phanquyen.AuthorizationService;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.hethong.phanquyen.RolePermissionPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** The existing RolePermissionPolicy remains the only source of authority. */
public final class TongQuanVaiTroService {
    private final DesktopSession session;
    private final DanhSachTaiKhoanService.ConnectionProvider connections;
    private final RolePermissionPolicy policy = new RolePermissionPolicy();
    private final AuthorizationService authorization = new AuthorizationService(policy);
    private final TongQuanVaiTroDao dao = new TongQuanVaiTroDao();

    public TongQuanVaiTroService(DesktopSession session,
                               DanhSachTaiKhoanService.ConnectionProvider connections) {
        this.session = Objects.requireNonNull(session, "session");
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    public List<TongQuanVaiTro> load() throws SQLException {
        requireRead();
        try (Connection connection = connections.open()) {
            requireRead();
            Map<VaiTroHeThong, Integer> counts = dao.countAccountsByRole(connection);
            requireRead();
            List<TongQuanVaiTro> rows = new ArrayList<>();
            for (VaiTroHeThong role : VaiTroHeThong.values()) {
                rows.add(new TongQuanVaiTro(role, policy.permissionsFor(role), counts.get(role)));
            }
            requireRead();
            return List.copyOf(rows);
        }
    }

    private void requireRead() {
        // Role-management permission controls access to the security policy overview.
        authorization.require(session, Quyen.ROLE_MANAGE);
    }
}
