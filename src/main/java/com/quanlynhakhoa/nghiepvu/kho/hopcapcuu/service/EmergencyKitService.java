package com.quanlynhakhoa.nghiepvu.kho.hopcapcuu.service;

import com.quanlynhakhoa.model.kho.DongKiemTraCapCuu;

import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import com.quanlynhakhoa.hethong.phanquyen.AuthorizationService;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.hethong.phanquyen.Permission;
import com.quanlynhakhoa.hethong.phanquyen.RolePermissionPolicy;
import com.quanlynhakhoa.nghiepvu.kho.hopcapcuu.dao.EmergencyKitDao;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public final class EmergencyKitService {
    private final DesktopSession session;
    private final AuthorizationService authorization = new AuthorizationService(new RolePermissionPolicy());
    private final EmergencyKitDao dao = new EmergencyKitDao();

    public EmergencyKitService(DesktopSession session) {
        if (session == null) throw new IllegalArgumentException("Cần phiên đăng nhập.");
        this.session = session;
    }

    public List<Object[]> danhMuc() throws SQLException {
        authorization.require(session, Permission.EMERGENCY_KIT_CHECK);
        return dao.danhMuc();
    }

    public List<Object[]> lichSu() throws SQLException {
        authorization.require(session, Permission.EMERGENCY_KIT_CHECK);
        return dao.lichSu();
    }

    public List<Object[]> chiTiet(String id) throws SQLException {
        authorization.require(session, Permission.EMERGENCY_KIT_CHECK);
        return dao.chiTiet(code(id));
    }

    public void ghiKiemTra(String id, String note, List<DongKiemTraCapCuu> lines) throws SQLException {
        authorization.require(session, Permission.EMERGENCY_KIT_CHECK);
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
