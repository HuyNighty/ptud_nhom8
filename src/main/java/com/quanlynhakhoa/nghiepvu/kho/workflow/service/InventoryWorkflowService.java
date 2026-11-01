package com.quanlynhakhoa.nghiepvu.kho.workflow.service;

import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import com.quanlynhakhoa.hethong.phanquyen.AuthorizationService;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.hethong.phanquyen.Permission;
import com.quanlynhakhoa.hethong.phanquyen.RolePermissionPolicy;
import com.quanlynhakhoa.nghiepvu.kho.workflow.dao.InventoryWorkflowDao;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** Permission, validation and transaction boundary for inventory workflows. */
public final class InventoryWorkflowService {
    private final DesktopSession session;
    private final AuthorizationService authorization = new AuthorizationService(new RolePermissionPolicy());
    private final InventoryWorkflowDao dao = new InventoryWorkflowDao();

    public InventoryWorkflowService(DesktopSession session) {
        if (session == null) throw new IllegalArgumentException("Cần phiên đăng nhập.");
        this.session = session;
    }

    public List<Object[]> danhSachNhap() throws SQLException {
        authorization.require(session, Permission.INVENTORY_READ);
        return dao.danhSachNhap();
    }

    public List<Object[]> danhSachXuat() throws SQLException {
        authorization.require(session, Permission.INVENTORY_READ);
        return dao.danhSachXuat();
    }

    public List<Object[]> danhSachKiemKe() throws SQLException {
        authorization.require(session, Permission.INVENTORY_READ);
        return dao.danhSachKiemKe();
    }

    public void taoPhieuNhap(String maPhieu, String maLo, String soLo, String maNcc, String loaiHang,
                             String maHang, LocalDate ngaySanXuat, LocalDate hanSuDung, int soLuong, long donGia, String ghiChu) throws SQLException {
        authorization.require(session, Permission.INVENTORY_MANAGE);
        checkCode(maPhieu, "Mã phiếu nhập");
        checkCode(maLo, "Mã lô");
        checkCode(maNcc, "Mã nhà cung cấp");
        checkCode(maHang, "Mã mặt hàng");
        if (soLo == null || soLo.isBlank() || soLo.trim().length() > 30) throw new IllegalArgumentException("Số lô bắt buộc, tối đa 30 ký tự.");
        if (!"THUOC".equals(loaiHang) && !"VATTU".equals(loaiHang)) throw new IllegalArgumentException("Chọn loại hàng hợp lệ.");
        if (soLuong <= 0 || donGia < 0) throw new IllegalArgumentException("Số lượng phải lớn hơn 0; đơn giá không được âm.");
        optional(ghiChu, 300, "Ghi chú");
        if (ngaySanXuat != null && hanSuDung != null && !hanSuDung.isAfter(ngaySanXuat)) throw new IllegalArgumentException("Hạn sử dụng phải sau ngày sản xuất.");
        transaction(con -> dao.taoPhieuNhap(con, session.accountId(), norm(maPhieu), norm(maLo), soLo.trim(), norm(maNcc), loaiHang, norm(maHang), ngaySanXuat, hanSuDung, soLuong, donGia, clean(ghiChu)));
    }

    public void taoPhieuXuat(String maPhieu, String maLo, String boPhanNhan, String nguoiNhan, String lyDo, int soLuong) throws SQLException {
        authorization.require(session, Permission.INVENTORY_MANAGE);
        checkCode(maPhieu, "Mã phiếu xuất");
        checkCode(maLo, "Mã lô");
        required(boPhanNhan, 100, "Bộ phận nhận");
        optional(nguoiNhan, 100, "Người nhận");
        optional(lyDo, 300, "Lý do");
        if (soLuong <= 0) throw new IllegalArgumentException("Số lượng xuất phải lớn hơn 0.");
        transaction(con -> dao.taoPhieuXuat(con, session.accountId(), norm(maPhieu), norm(maLo), boPhanNhan.trim(), clean(nguoiNhan), clean(lyDo), soLuong));
    }

    public void guiKiemKe(String maPhieu, String maLo, int soLuongThucTe, String lyDo) throws SQLException {
        authorization.require(session, Permission.INVENTORY_MANAGE);
        checkCode(maPhieu, "Mã phiếu kiểm kê");
        checkCode(maLo, "Mã lô");
        required(lyDo, 500, "Lý do");
        if (soLuongThucTe < 0) throw new IllegalArgumentException("Số lượng thực tế không được âm.");
        transaction(con -> dao.taoYeuCauKiemKe(con, session.accountId(), norm(maPhieu), norm(maLo), soLuongThucTe, lyDo.trim()));
    }

    private void transaction(SqlAction action) throws SQLException {
        try (Connection con = TeamDevConnection.open()) {
            con.setAutoCommit(false);
            try {
                action.run(con);
                con.commit();
            } catch (SQLException | RuntimeException error) {
                try {
                    con.rollback();
                } catch (SQLException rollbackError) {
                    error.addSuppressed(rollbackError);
                }
                throw error;
            }
        }
    }

    public static void checkCode(String value, String label) {
        String s = norm(value);
        if (s.isEmpty() || s.length() > 10 || !s.matches("[A-Z0-9_-]+")) throw new IllegalArgumentException(label + " bắt buộc, tối đa 10 ký tự A-Z, 0-9, _ hoặc -.");
    }

    public static String norm(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    public static void required(String value, int max, String label) {
        if (value == null || value.trim().isEmpty() || value.trim().length() > max) throw new IllegalArgumentException(label + " bắt buộc và tối đa " + max + " ký tự.");
    }

    public static void optional(String value, int max, String label) {
        if (value != null && value.trim().length() > max) throw new IllegalArgumentException(label + " tối đa " + max + " ký tự.");
    }

    public static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @FunctionalInterface
    private interface SqlAction {
        void run(Connection con) throws SQLException;
    }
}