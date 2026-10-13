package vn.n08.nhakhoa.service;

import vn.n08.nhakhoa.dao.NhanVienDao;
import vn.n08.nhakhoa.dao.TaiKhoanDao;
import vn.n08.nhakhoa.dao.ThamSoDao;
import vn.n08.nhakhoa.model.NhanVien;
import vn.n08.nhakhoa.model.TaiKhoan;
import vn.n08.nhakhoa.model.TrangThaiTaiKhoan;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.util.PasswordHasher;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.util.Tx;

/** UC38 Đăng nhập, đăng xuất, đổi mật khẩu của chính mình. */
public class AuthService {
    private final TaiKhoanDao tkDao = new TaiKhoanDao();
    private final NhanVienDao nvDao = new NhanVienDao();
    private final ThamSoDao tsDao = new ThamSoDao();

    private record KetQua(Session.User user, String loi) {}

    /**
     * Đăng nhập. Sai mật khẩu → tăng số lần sai; đạt ngưỡng SO_LAN_SAI_TOI_DA → tự khóa (BI_KHOA).
     * Tài khoản BI_KHOA, VO_HIEU hoặc nhân viên đã nghỉ việc không đăng nhập được.
     */
    public Session.User login(String tenDangNhap, String matKhau) {
        String ten = tenDangNhap == null ? "" : tenDangNhap.trim().toLowerCase();
        if (ten.isEmpty()) throw new BusinessException("tenDangNhap", "Nhập tên đăng nhập.");
        if (matKhau == null || matKhau.isEmpty()) throw new BusinessException("matKhau", "Nhập mật khẩu.");
        KetQua kq = Tx.write(c -> {
            TaiKhoan tk = tkDao.findByUsername(c, ten).orElse(null);
            if (tk == null) return new KetQua(null, "Tên đăng nhập hoặc mật khẩu không đúng.");
            if (tk.trangThai() == TrangThaiTaiKhoan.VO_HIEU) {
                return new KetQua(null, "Tài khoản đã bị vô hiệu hóa và không thể đăng nhập.");
            }
            if (tk.trangThai() == TrangThaiTaiKhoan.BI_KHOA) {
                return new KetQua(null, "Tài khoản đang bị khóa" + (tk.lyDoKhoa() == null ? "" : " (" + tk.lyDoKhoa() + ")")
                        + ". Liên hệ quản lý phòng khám để mở khóa.");
            }
            NhanVien nv = nvDao.find(c, tk.maNhanVien()).orElseThrow();
            if (!nv.dangLamViec()) return new KetQua(null, "Nhân viên đã nghỉ việc, tài khoản không còn hiệu lực.");
            if (!PasswordHasher.verify(matKhau, tk.matKhau())) {
                int max = tsDao.getInt(c, "SO_LAN_SAI_TOI_DA", 5);
                int sai = tk.soLanSai() + 1;
                tkDao.loginFailed(c, tk.maTaiKhoan(), sai);
                if (sai >= max) {
                    tkDao.setTrangThai(c, tk.maTaiKhoan(), TrangThaiTaiKhoan.HOAT_DONG, TrangThaiTaiKhoan.BI_KHOA,
                            "Tự động khóa sau " + max + " lần nhập sai mật khẩu");
                    Audit.logAs(c, tk.maTaiKhoan(), "Tài khoản", "Khóa tự động sau " + max + " lần sai mật khẩu", tk.tenDangNhap(), null);
                    return new KetQua(null, "Sai mật khẩu " + max + " lần liên tiếp. Tài khoản đã bị khóa, liên hệ quản lý để mở khóa.");
                }
                return new KetQua(null, "Tên đăng nhập hoặc mật khẩu không đúng. Còn " + (max - sai) + " lần thử trước khi tài khoản bị khóa.");
            }
            tkDao.loginOk(c, tk.maTaiKhoan(), Now.now());
            Audit.logAs(c, tk.maTaiKhoan(), "Hệ thống", "Đăng nhập", tk.tenDangNhap(), null);
            return new KetQua(new Session.User(tk.maTaiKhoan(), tk.tenDangNhap(), tk.vaiTro(), nv.maNhanVien(), nv.hoTen()), null);
        });
        if (kq.loi() != null) throw new BusinessException(kq.loi());
        Session.login(kq.user());
        return kq.user();
    }

    public void logout() {
        if (!Session.isLoggedIn()) return;
        Session.User u = Session.user();
        try {
            Tx.run(c -> Audit.logAs(c, u.maTaiKhoan(), "Hệ thống", "Đăng xuất", u.tenDangNhap(), null));
        } finally {
            Session.logout();
        }
    }

    /** Đổi mật khẩu của chính tài khoản đang đăng nhập (cần mật khẩu cũ). */
    public void doiMatKhau(String matKhauCu, String matKhauMoi, String nhapLai) {
        Session.User u = Session.user();
        if (matKhauMoi == null || !matKhauMoi.equals(nhapLai)) throw new BusinessException("nhapLai", "Mật khẩu nhập lại không khớp.");
        PasswordHasher.checkStrength(matKhauMoi);
        Tx.run(c -> {
            TaiKhoan tk = tkDao.find(c, u.maTaiKhoan()).orElseThrow();
            if (!PasswordHasher.verify(matKhauCu, tk.matKhau())) throw new BusinessException("matKhauCu", "Mật khẩu hiện tại không đúng.");
            if (PasswordHasher.verify(matKhauMoi, tk.matKhau())) throw new BusinessException("matKhau", "Mật khẩu mới phải khác mật khẩu hiện tại.");
            tkDao.setPassword(c, tk.maTaiKhoan(), PasswordHasher.hash(matKhauMoi));
            Audit.log(c, "Tài khoản", "Đổi mật khẩu của chính mình", tk.tenDangNhap(), null);
        });
    }
}
