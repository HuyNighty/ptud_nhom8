package vn.n08.nhakhoa.service;

import vn.n08.nhakhoa.dao.MaDao;
import vn.n08.nhakhoa.dao.NhanVienDao;
import vn.n08.nhakhoa.dao.TaiKhoanDao;
import vn.n08.nhakhoa.dto.TaiKhoanView;
import vn.n08.nhakhoa.model.NhanVien;
import vn.n08.nhakhoa.model.TaiKhoan;
import vn.n08.nhakhoa.model.TrangThaiTaiKhoan;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.PasswordHasher;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.util.Tx;
import vn.n08.nhakhoa.util.Validator;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * UC21 Quản lý tài khoản và phân quyền.
 * BI_KHOA = khóa tạm thời (mở lại được); VO_HIEU = vô hiệu vĩnh viễn (không khôi phục, không xóa để giữ lịch sử).
 */
public class TaiKhoanService {
    private final TaiKhoanDao dao = new TaiKhoanDao();
    private final NhanVienDao nvDao = new NhanVienDao();
    private final MaDao maDao = new MaDao();

    public List<TaiKhoanView> danhSach(String tuKhoa, VaiTro vaiTro, TrangThaiTaiKhoan trangThai) {
        Session.require(Quyen.TAI_KHOAN);
        return Tx.read(c -> dao.search(c, tuKhoa, vaiTro, trangThai));
    }

    /** Nhân viên đang làm việc chưa có tài khoản. */
    public List<NhanVien> nhanVienChuaCoTaiKhoan() {
        Session.require(Quyen.TAI_KHOAN);
        return Tx.read(c -> nvDao.search(c, null, null, vn.n08.nhakhoa.model.TrangThaiNhanVien.DANG_LAM_VIEC).stream()
                .filter(v -> v.maTaiKhoan() == null).map(v -> v.nv()).toList());
    }

    public String tao(String maNhanVien, String tenDangNhap, VaiTro vaiTro, String matKhauTam) {
        Session.require(Quyen.TAI_KHOAN);
        String ten = Validator.username(tenDangNhap == null ? null : tenDangNhap.trim().toLowerCase(), "tenDangNhap");
        if (vaiTro == null) throw new BusinessException("vaiTro", "Chọn vai trò.");
        PasswordHasher.checkStrength(matKhauTam);
        return Tx.write(c -> {
            NhanVien nv = nvDao.find(c, maNhanVien).orElseThrow(() -> new BusinessException("nhanVien", "Chọn nhân viên."));
            if (!nv.dangLamViec()) throw new BusinessException("nhanVien", "Nhân viên đã nghỉ việc, không cấp tài khoản.");
            if (dao.findByNhanVien(c, nv.maNhanVien()).isPresent()) throw new BusinessException("nhanVien", nv.hoTen() + " đã có tài khoản.");
            if (dao.usernameUsed(c, ten)) throw new BusinessException("tenDangNhap", "Tên đăng nhập \"" + ten + "\" đã tồn tại.");
            if (vaiTro == VaiTro.AD && Session.user().vaiTro() != VaiTro.AD) {
                throw new BusinessException("vaiTro", "Chỉ quản trị hệ thống được cấp vai trò quản trị.");
            }
            String ma = maDao.next(c, "TK");
            dao.insert(c, new TaiKhoan(ma, ten, PasswordHasher.hash(matKhauTam), TrangThaiTaiKhoan.HOAT_DONG, vaiTro, nv.maNhanVien(), 0, null, null));
            Audit.log(c, "Tài khoản", "Tạo tài khoản (" + vaiTro.label() + ")", ten, "Nhân viên " + nv.maNhanVien() + " — " + nv.hoTen());
            return ma;
        });
    }

    public void khoa(String maTaiKhoan, String lyDo) {
        Session.require(Quyen.TAI_KHOAN);
        String ly = Validator.required(lyDo, "lyDo", "Lý do khóa");
        Tx.run(c -> {
            TaiKhoan tk = load(c, maTaiKhoan);
            notSelf(tk, "khóa");
            if (tk.trangThai() != TrangThaiTaiKhoan.HOAT_DONG) throw new BusinessException("Chỉ khóa được tài khoản đang hoạt động.");
            guardAdmin(tk);
            if (dao.setTrangThai(c, tk.maTaiKhoan(), TrangThaiTaiKhoan.HOAT_DONG, TrangThaiTaiKhoan.BI_KHOA, ly) == 0) throw stale();
            Audit.log(c, "Tài khoản", "Khóa tài khoản", tk.tenDangNhap(), ly);
        });
    }

    public void moKhoa(String maTaiKhoan) {
        Session.require(Quyen.TAI_KHOAN);
        Tx.run(c -> {
            TaiKhoan tk = load(c, maTaiKhoan);
            if (tk.trangThai() != TrangThaiTaiKhoan.BI_KHOA) throw new BusinessException("Chỉ mở khóa được tài khoản đang bị khóa.");
            NhanVien nv = nvDao.find(c, tk.maNhanVien()).orElseThrow();
            if (!nv.dangLamViec()) throw new BusinessException("Nhân viên đã nghỉ việc, không mở khóa.");
            if (dao.setTrangThai(c, tk.maTaiKhoan(), TrangThaiTaiKhoan.BI_KHOA, TrangThaiTaiKhoan.HOAT_DONG, null) == 0) throw stale();
            Audit.log(c, "Tài khoản", "Mở khóa tài khoản", tk.tenDangNhap(), "Đặt lại số lần sai về 0");
        });
    }

    /** Vô hiệu vĩnh viễn; không xóa tài khoản để giữ quan hệ lịch sử. */
    public void voHieu(String maTaiKhoan, String lyDo) {
        Session.require(Quyen.TAI_KHOAN);
        String ly = Validator.required(lyDo, "lyDo", "Lý do vô hiệu");
        Tx.run(c -> {
            TaiKhoan tk = load(c, maTaiKhoan);
            notSelf(tk, "vô hiệu");
            guardAdmin(tk);
            if (tk.trangThai() == TrangThaiTaiKhoan.VO_HIEU) throw new BusinessException("Tài khoản đã vô hiệu.");
            if (dao.voHieu(c, tk.maTaiKhoan(), ly) == 0) throw stale();
            Audit.log(c, "Tài khoản", "Vô hiệu tài khoản", tk.tenDangNhap(), ly);
        });
    }

    public void datLaiMatKhau(String maTaiKhoan, String matKhauTam) {
        Session.require(Quyen.TAI_KHOAN);
        PasswordHasher.checkStrength(matKhauTam);
        Tx.run(c -> {
            TaiKhoan tk = load(c, maTaiKhoan);
            if (tk.trangThai() == TrangThaiTaiKhoan.VO_HIEU) throw new BusinessException("Tài khoản đã vô hiệu, không đặt lại mật khẩu.");
            guardAdmin(tk);
            dao.setPassword(c, tk.maTaiKhoan(), PasswordHasher.hash(matKhauTam));
            Audit.log(c, "Tài khoản", "Đặt lại mật khẩu", tk.tenDangNhap(), "Mật khẩu tạm do quản lý cấp");
        });
    }

    public void doiVaiTro(String maTaiKhoan, VaiTro vaiTro) {
        Session.require(Quyen.TAI_KHOAN);
        if (vaiTro == null) throw new BusinessException("vaiTro", "Chọn vai trò.");
        Tx.run(c -> {
            TaiKhoan tk = load(c, maTaiKhoan);
            notSelf(tk, "đổi vai trò");
            guardAdmin(tk);
            if (vaiTro == VaiTro.AD && Session.user().vaiTro() != VaiTro.AD) throw new BusinessException("Chỉ quản trị hệ thống được cấp vai trò quản trị.");
            if (tk.trangThai() == TrangThaiTaiKhoan.VO_HIEU) throw new BusinessException("Tài khoản đã vô hiệu.");
            if (tk.vaiTro() == vaiTro) return;
            dao.setVaiTro(c, tk.maTaiKhoan(), vaiTro);
            Audit.log(c, "Tài khoản", "Đổi vai trò", tk.tenDangNhap(), tk.vaiTro().label() + " → " + vaiTro.label());
        });
    }

    private TaiKhoan load(Connection c, String ma) throws SQLException {
        return dao.find(c, ma).orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản " + ma + "."));
    }

    private static void notSelf(TaiKhoan tk, String hanhDong) {
        if (tk.maTaiKhoan().equals(Session.user().maTaiKhoan())) {
            throw new BusinessException("Không thể tự " + hanhDong + " tài khoản đang đăng nhập.");
        }
    }

    /** Tài khoản quản trị chỉ được tài khoản quản trị khác thay đổi. */
    private static void guardAdmin(TaiKhoan tk) {
        if (tk.vaiTro() == VaiTro.AD && Session.user().vaiTro() != VaiTro.AD) {
            throw new BusinessException("Tài khoản quản trị hệ thống chỉ do quản trị viên khác thay đổi.");
        }
    }

    static BusinessException stale() {
        return new BusinessException("Dữ liệu đã được người khác thay đổi. Tải lại danh sách rồi thử lại.");
    }
}
