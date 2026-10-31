package com.quanlynhakhoa.inventory.nhacungcap.service;

import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.inventory.nhacungcap.dao.NhaCungCapDao;
import com.quanlynhakhoa.inventory.nhacungcap.model.NhaCungCap;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/** Service là biên bắt buộc kiểm tra quyền và quy tắc nghiệp vụ cho nhà cung cấp. */
public class NhaCungCapService {
    private final DesktopSession phien;
    private final AuthorizationService phanQuyen = new AuthorizationService(new RolePermissionPolicy());
    private final NhaCungCapDao dao;

    public NhaCungCapService(DesktopSession phien) {
        this(phien, new NhaCungCapDao());
    }

    public NhaCungCapService(DesktopSession phien, NhaCungCapDao dao) {
        if (phien == null) throw new IllegalArgumentException("Cần phiên đăng nhập.");
        if (dao == null) throw new IllegalArgumentException("Cần DAO nhà cung cấp.");
        this.phien = phien;
        this.dao = dao;
    }

    public boolean coQuyen(Permission quyen) { return phanQuyen.can(phien, quyen); }

    public List<NhaCungCap> timKiem(String tuKhoa, String trangThai) throws SQLException {
        phanQuyen.require(phien, Permission.INVENTORY_READ);
        String keyword = tuKhoa == null ? "" : tuKhoa.trim();
        if (keyword.length() > 100) throw new IllegalArgumentException("Từ khóa tìm kiếm tối đa 100 ký tự.");
        String state = trangThai == null ? "" : trangThai.trim();
        if (!state.isEmpty() && !NhaCungCap.DANG_HOP_TAC.equals(state)
                && !NhaCungCap.NGUNG_HOP_TAC.equals(state)) {
            throw new IllegalArgumentException("Trạng thái lọc không hợp lệ.");
        }
        return dao.timKiem(keyword, state);
    }

    public void them(NhaCungCap ncc) throws SQLException {
        phanQuyen.require(phien, Permission.INVENTORY_MANAGE);
        kiemTraVaChuanHoa(ncc);
        try (Connection con = TeamDevConnection.open()) {
            con.setAutoCommit(false);
            try {
                if (dao.tonTai(con, ncc.getMaNhaCungCap())) {
                    throw new IllegalArgumentException("Mã nhà cung cấp đã tồn tại.");
                }
                dao.them(con, ncc);
                dao.ghiNhatKy(con, phien.accountId(), "Thêm nhà cung cấp", ncc.getMaNhaCungCap());
                con.commit();
            } catch (SQLException | RuntimeException loi) {
                rollback(con, loi);
                if (loi instanceof SQLException sql && (sql.getErrorCode() == 2601 || sql.getErrorCode() == 2627)) {
                    throw new IllegalArgumentException("Mã nhà cung cấp đã tồn tại.", sql);
                }
                throw loi;
            }
        }
    }

    public void capNhatThongTin(NhaCungCap ncc) throws SQLException {
        phanQuyen.require(phien, Permission.INVENTORY_MANAGE);
        kiemTraVaChuanHoa(ncc);
        try (Connection con = TeamDevConnection.open()) {
            con.setAutoCommit(false);
            try {
                if (!dao.capNhatThongTin(con, ncc)) {
                    throw new IllegalArgumentException("Không tìm thấy nhà cung cấp " + ncc.getMaNhaCungCap() + ".");
                }
                dao.ghiNhatKy(con, phien.accountId(), "Cập nhật nhà cung cấp", ncc.getMaNhaCungCap());
                con.commit();
            } catch (SQLException | RuntimeException loi) {
                rollback(con, loi);
                throw loi;
            }
        }
    }

    /** Ngừng/khôi phục hợp tác bằng trạng thái, giữ lịch sử lô hàng và chứng từ tham chiếu. */
    public void capNhatTrangThai(String maNhaCungCap, String trangThaiMoi) throws SQLException {
        phanQuyen.require(phien, Permission.INVENTORY_MANAGE);
        String ma = maNhaCungCap == null ? "" : maNhaCungCap.trim().toUpperCase(Locale.ROOT);
        if (ma.isEmpty() || ma.length() > 10 || !ma.matches("[A-Z0-9_-]+")) {
            throw new IllegalArgumentException("Mã nhà cung cấp không hợp lệ.");
        }
        if (!NhaCungCap.DANG_HOP_TAC.equals(trangThaiMoi)
                && !NhaCungCap.NGUNG_HOP_TAC.equals(trangThaiMoi)) {
            throw new IllegalArgumentException("Trạng thái nhà cung cấp không hợp lệ.");
        }
        try (Connection con = TeamDevConnection.open()) {
            con.setAutoCommit(false);
            try {
                if (!dao.capNhatTrangThai(con, ma, trangThaiMoi)) {
                    throw new IllegalArgumentException("Không tìm thấy nhà cung cấp " + ma + ".");
                }
                String action = NhaCungCap.NGUNG_HOP_TAC.equals(trangThaiMoi)
                        ? "Ngừng hợp tác nhà cung cấp" : "Khôi phục hợp tác nhà cung cấp";
                dao.ghiNhatKy(con, phien.accountId(), action, ma);
                con.commit();
            } catch (SQLException | RuntimeException loi) {
                rollback(con, loi);
                throw loi;
            }
        }
    }

    public static void kiemTraVaChuanHoa(NhaCungCap ncc) {
        if (ncc == null) throw new IllegalArgumentException("Thiếu dữ liệu nhà cung cấp.");
        String ma = ncc.getMaNhaCungCap() == null ? "" : ncc.getMaNhaCungCap().trim().toUpperCase(Locale.ROOT);
        if (ma.isEmpty() || ma.length() > 10 || !ma.matches("[A-Z0-9_-]+")) {
            throw new IllegalArgumentException("Mã nhà cung cấp bắt buộc, tối đa 10 ký tự A-Z, 0-9, _ hoặc -.");
        }
        ncc.setMaNhaCungCap(ma);

        String ten = ncc.getTenNhaCungCap() == null ? "" : ncc.getTenNhaCungCap().trim().replaceAll("\\s+", " ");
        if (ten.isEmpty() || ten.length() > 150) {
            throw new IllegalArgumentException("Tên nhà cung cấp bắt buộc và tối đa 150 ký tự.");
        }
        ncc.setTenNhaCungCap(ten);

        ncc.setDiaChi(chuanHoaOptional(ncc.getDiaChi(), 255, "Địa chỉ"));
        String phone = ncc.getSoDienThoai() == null ? "" : ncc.getSoDienThoai().trim();
        if (phone.isEmpty() || phone.length() > 20 || !phone.matches("[0-9+(). -]+")) {
            throw new IllegalArgumentException("Số điện thoại bắt buộc, tối đa 20 ký tự; chỉ dùng số và + ( ) . - hoặc khoảng trắng.");
        }
        ncc.setSoDienThoai(phone);

        String email = ncc.getEmail() == null ? "" : ncc.getEmail().trim();
        if (email.isEmpty()) {
            ncc.setEmail(null);
        } else {
            if (email.length() > 100 || !email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")) {
                throw new IllegalArgumentException("Email không đúng định dạng hoặc dài quá 100 ký tự.");
            }
            ncc.setEmail(email);
        }
    }

    private static String chuanHoaOptional(String value, int max, String label) {
        if (value == null || value.trim().isEmpty()) return null;
        String trimmed = value.trim();
        if (trimmed.length() > max) throw new IllegalArgumentException(label + " tối đa " + max + " ký tự.");
        return trimmed;
    }

    private static void rollback(Connection con, Throwable error) {
        try { con.rollback(); }
        catch (SQLException rollbackError) { error.addSuppressed(rollbackError); }
    }
}
