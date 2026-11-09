package com.quanlynhakhoa.service.kho.hopcapcuu;

import com.quanlynhakhoa.model.kho.DongKiemTraCapCuu;
import com.quanlynhakhoa.config.phattrien.TeamDevConnection;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import com.quanlynhakhoa.dao.kho.hopcapcuu.HopCapCuuDao;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public final class HopCapCuuService {
    private final DesktopSession session;
    private final AuthorizationService authorization = new AuthorizationService(new RolePermissionPolicy());
    private final HopCapCuuDao dao = new HopCapCuuDao();

    public HopCapCuuService(DesktopSession session) {
        if (session == null) throw new IllegalArgumentException("Cần phiên đăng nhập.");
        this.session = session;
    }

    public List<Object[]> danhMuc() throws SQLException {
        authorization.require(session, Quyen.EMERGENCY_KIT_CHECK);
        return dao.danhMuc();
    }

    public List<Object[]> lichSu() throws SQLException {
        authorization.require(session, Quyen.EMERGENCY_KIT_CHECK);
        return dao.lichSu();
    }

    public List<Object[]> chiTiet(String id) throws SQLException {
        authorization.require(session, Quyen.EMERGENCY_KIT_CHECK);
        return dao.chiTiet(code(id));
    }

    public void ghiKiemTra(String id, String note, List<DongKiemTraCapCuu> lines) throws SQLException {
        authorization.require(session, Quyen.EMERGENCY_KIT_CHECK);
        String checkId = code(id);
        if (note != null && note.trim().length() > 500) throw new IllegalArgumentException("Ghi chú tối đa 500 ký tự.");
        if (lines == null || lines.isEmpty()) throw new IllegalArgumentException("Không có danh mục nào để kiểm tra.");
        java.util.HashSet<String> seen = new java.util.HashSet<>();

        for (DongKiemTraCapCuu line : lines) {
            if (line == null || line.actualQuantity() < 0 || !seen.add(code(line.itemId()))) {
                throw new IllegalArgumentException("Chi tiết kiểm tra không hợp lệ hoặc bị lặp.");
            }
        }

        try (Connection con = TeamDevConnection.open()) {
            con.setAutoCommit(false);
            try {
                dao.ghiKiemTra(con, session.accountId(), checkId, note == null || note.isBlank() ? null : note.trim(), lines);
                con.commit();
            } catch (SQLException | RuntimeException error) {
                try {
                    con.rollback();
                } catch (SQLException rollback) {
                    error.addSuppressed(rollback);
                }
                throw error;
            }
        }
    }

    private static String code(String value) {
        String s = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!s.matches("[A-Z0-9_-]{1,10}")) throw new IllegalArgumentException("Mã kiểm tra/mục bắt buộc, tối đa 10 ký tự A-Z, 0-9, _ hoặc -.");
        return s;
    }


}
