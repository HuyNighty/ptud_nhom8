package vn.n08.nhakhoa.service;

import vn.n08.nhakhoa.dao.NhatKyDao;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.util.Session;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Ghi nhật ký thao tác trong cùng transaction với nghiệp vụ.
 * maTaiKhoan luôn là tài khoản đang đăng nhập (không mạo danh);
 * maNguoiChiuTrachNhiem là bác sĩ/nhân viên chịu trách nhiệm chuyên môn khi khác người thao tác.
 */
final class Audit {
    private static final NhatKyDao DAO = new NhatKyDao();

    private Audit() {}

    static void log(Connection c, String chucNang, String hanhDong, String doiTuong, String noiDung) throws SQLException {
        log(c, chucNang, hanhDong, doiTuong, noiDung, null);
    }

    static void log(Connection c, String chucNang, String hanhDong, String doiTuong, String noiDung, String maNguoiChiuTrachNhiem) throws SQLException {
        DAO.insert(c, Now.now(), Session.user().maTaiKhoan(), chucNang, hanhDong, doiTuong, noiDung, maNguoiChiuTrachNhiem);
    }

    /** Dùng khi chưa có phiên (đăng nhập sai, tự khóa). */
    static void logAs(Connection c, String maTaiKhoan, String chucNang, String hanhDong, String doiTuong, String noiDung) throws SQLException {
        DAO.insert(c, Now.now(), maTaiKhoan, chucNang, hanhDong, doiTuong, noiDung, null);
    }
}
