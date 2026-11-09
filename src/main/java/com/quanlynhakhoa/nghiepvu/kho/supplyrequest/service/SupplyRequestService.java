package com.quanlynhakhoa.nghiepvu.kho.supplyrequest.service;

import com.quanlynhakhoa.model.kho.PhanBoLo;

import com.quanlynhakhoa.model.kho.DongYeuCauCungUng;

import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import com.quanlynhakhoa.hethong.phanquyen.AuthorizationService;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.hethong.phanquyen.Permission;
import com.quanlynhakhoa.hethong.phanquyen.RolePermissionPolicy;
import com.quanlynhakhoa.nghiepvu.kho.supplyrequest.dao.SupplyRequestDao;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Requester operations are SUPPLY_REQUEST; approval and issuing require INVENTORY_MANAGE. */
public final class SupplyRequestService {
    private final DesktopSession session;
    private final AuthorizationService authorization = new AuthorizationService(new RolePermissionPolicy());
    private final SupplyRequestDao dao = new SupplyRequestDao();

    public SupplyRequestService(DesktopSession session) {
        if (session == null) throw new IllegalArgumentException("Cần phiên đăng nhập.");
        this.session = session;
    }

    public boolean canRequest() {
        return authorization.can(session, Permission.SUPPLY_REQUEST);
    }

    public boolean canManage() {
        return authorization.can(session, Permission.INVENTORY_MANAGE);
    }

    public List<Object[]> danhSachCuaToi() throws SQLException {
        authorization.require(session, Permission.SUPPLY_REQUEST);
        return dao.danhSachCuaToi(session.accountId());
    }

    public List<Object[]> danhSachChoKho() throws SQLException {
        authorization.require(session, Permission.INVENTORY_READ);
        return dao.danhSachChoKho();
    }

    public List<Object[]> chiTiet(String requestId) throws SQLException {
        if (canManage()) return dao.chiTiet(code(requestId, "Mã yêu cầu"), session.accountId(), true);
        authorization.require(session, Permission.SUPPLY_REQUEST);
        return dao.chiTiet(code(requestId, "Mã yêu cầu"), session.accountId(), false);
    }

    public void taoYeuCau(String requestId, String reason, List<DongYeuCauCungUng> lines) throws SQLException {
        authorization.require(session, Permission.SUPPLY_REQUEST);
        String id = code(requestId, "Mã yêu cầu");
        required(reason, 300, "Lý do");
        if (lines == null || lines.isEmpty() || lines.size() > 30) throw new IllegalArgumentException("Yêu cầu cần từ 1 đến 30 dòng hàng.");

        for (DongYeuCauCungUng line : lines) {
            if (line == null || !("THUOC".equals(line.itemType()) || "VATTU".equals(line.itemType())) || line.quantity() <= 0) {
                throw new IllegalArgumentException("Loại hàng và số lượng từng dòng không hợp lệ.");
            }
            code(line.itemCode(), "Mã thuốc/vật tư");
        }
        transaction(con -> dao.taoYeuCau(con, session.accountId(), id, reason.trim(), lines));
    }

    public void duyet(String requestId) throws SQLException {
        authorization.require(session, Permission.INVENTORY_MANAGE);
        String id = code(requestId, "Mã yêu cầu");
        transaction(con -> dao.duyet(con, session.accountId(), id));
    }

    public void tuChoi(String requestId, String reason) throws SQLException {
        authorization.require(session, Permission.INVENTORY_MANAGE);
        String id = code(requestId, "Mã yêu cầu");
        required(reason, 500, "Lý do từ chối");
        transaction(con -> dao.tuChoi(con, session.accountId(), id, reason.trim()));
    }

    public void capPhat(String requestId, String voucherId, String department, String receiver, String reason, List<PhanBoLo> allocations) throws SQLException {
        authorization.require(session, Permission.INVENTORY_MANAGE);
        String request = code(requestId, "Mã yêu cầu"), voucher = code(voucherId, "Mã phiếu xuất");
        required(department, 100, "Bộ phận nhận");
        optional(receiver, 100, "Người nhận");
        optional(reason, 300, "Lý do");
        if (allocations == null || allocations.isEmpty()) throw new IllegalArgumentException("Chưa phân bổ lô cấp phát.");

        Set<Integer> lineNumbers = new HashSet<>();
        for (PhanBoLo a : allocations) {
            if (a == null || a.lineNo() < 1 || !lineNumbers.add(a.lineNo())) throw new IllegalArgumentException("Số dòng phân bổ không hợp lệ hoặc bị trùng.");
            code(a.lotCode(), "Mã lô");
        }
        transaction(con -> dao.capPhat(con, session.accountId(), request, voucher, department.trim(), clean(receiver), clean(reason), allocations));
    }

    private static void transaction(SqlAction action) throws SQLException {
        try (Connection con = TeamDevConnection.open()) {
            con.setAutoCommit(false);
            try {
                action.run(con);
                con.commit();
            } catch (SQLException | RuntimeException failure) {
                try {
                    con.rollback();
                } catch (SQLException rollback) {
                    failure.addSuppressed(rollback);
                }
                throw failure;
            }
        }
    }

    private static String code(String value, String label) {
        String s = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!s.matches("[A-Z0-9_-]{1,10}")) throw new IllegalArgumentException(label + " bắt buộc, tối đa 10 ký tự A-Z, 0-9, _ hoặc -.");
        return s;
    }

    private static void required(String value, int max, String label) {
        if (value == null || value.trim().isEmpty() || value.trim().length() > max) throw new IllegalArgumentException(label + " bắt buộc và tối đa " + max + " ký tự.");
    }

    private static void optional(String value, int max, String label) {
        if (value != null && value.trim().length() > max) throw new IllegalArgumentException(label + " tối đa " + max + " ký tự.");
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @FunctionalInterface
    private interface SqlAction {
        void run(Connection con) throws SQLException;
    }



}
