package vn.n08.nhakhoa.util;

import vn.n08.nhakhoa.model.VaiTro;

/** Phiên đăng nhập hiện tại. Mọi Service kiểm tra quyền qua lớp này (không chỉ ẩn nút trên giao diện). */
public final class Session {
    public record User(String maTaiKhoan, String tenDangNhap, VaiTro vaiTro, String maNhanVien, String hoTen) {}

    private static volatile User current;

    private Session() {}

    public static void login(User u) { current = u; }
    public static void logout() { current = null; }
    public static boolean isLoggedIn() { return current != null; }

    public static User user() {
        if (current == null) throw new BusinessException("Phiên làm việc đã kết thúc. Vui lòng đăng nhập lại.");
        return current;
    }

    public static boolean can(Quyen q) {
        return current != null && q.allows(current.vaiTro());
    }

    /** Cần ít nhất một trong các quyền. */
    public static void requireAny(Quyen... qs) {
        User u = user();
        for (Quyen q : qs) if (q.allows(u.vaiTro())) return;
        throw new BusinessException("Tài khoản " + u.tenDangNhap() + " (" + u.vaiTro().label() + ") không có quyền thực hiện thao tác này.");
    }

    public static boolean canAny(Quyen... qs) {
        for (Quyen q : qs) if (can(q)) return true;
        return false;
    }

    public static void require(Quyen q) {
        User u = user();
        if (!q.allows(u.vaiTro())) {
            throw new BusinessException("Tài khoản " + u.tenDangNhap() + " (" + u.vaiTro().label() + ") không có quyền thực hiện thao tác này.");
        }
    }
}
