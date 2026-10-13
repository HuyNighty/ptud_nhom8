package vn.n08.nhakhoa.service;

import vn.n08.nhakhoa.dao.BanGiaoDao;
import vn.n08.nhakhoa.dao.LichHenDao;
import vn.n08.nhakhoa.dao.MaDao;
import vn.n08.nhakhoa.dao.NhanVienDao;
import vn.n08.nhakhoa.dao.PhanCongDao;
import vn.n08.nhakhoa.dao.PhieuKhamDao;
import vn.n08.nhakhoa.dao.TaiKhoanDao;
import vn.n08.nhakhoa.dto.NghiViecCheck;
import vn.n08.nhakhoa.dto.NhanVienForm;
import vn.n08.nhakhoa.dto.NhanVienView;
import vn.n08.nhakhoa.model.NhanVien;
import vn.n08.nhakhoa.model.TaiKhoan;
import vn.n08.nhakhoa.model.TrangThaiNhanVien;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.util.Tx;
import vn.n08.nhakhoa.util.Validator;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** UC22 Quản lý nhân viên, UC26 Hồ sơ nhân sự, thủ tục nghỉ việc có điều kiện. */
public class NhanVienService {
    private final NhanVienDao dao = new NhanVienDao();
    private final TaiKhoanDao tkDao = new TaiKhoanDao();
    private final PhanCongDao pcDao = new PhanCongDao();
    private final LichHenDao lhDao = new LichHenDao();
    private final PhieuKhamDao pkDao = new PhieuKhamDao();
    private final BanGiaoDao bgDao = new BanGiaoDao();
    private final MaDao maDao = new MaDao();

    public List<NhanVienView> danhSach(String tuKhoa, VaiTro chucDanh, TrangThaiNhanVien trangThai) {
        Session.require(Quyen.NHAN_VIEN);
        return Tx.read(c -> dao.search(c, tuKhoa, chucDanh, trangThai));
    }

    public NhanVien chiTiet(String ma) {
        Session.require(Quyen.NHAN_VIEN);
        return Tx.read(c -> dao.find(c, ma)).orElseThrow(() -> new BusinessException("Không tìm thấy nhân viên " + ma + "."));
    }

    /** Nhân viên đang làm việc theo chức danh (danh sách chọn) — mọi người dùng đã đăng nhập. */
    public List<NhanVien> dangLamViec(VaiTro chucDanh) {
        Session.user();
        return Tx.read(c -> dao.active(c, chucDanh));
    }

    public String them(NhanVienForm f) {
        Session.require(Quyen.NHAN_VIEN);
        NhanVien n = validate(null, f);
        if (f.chucDanh() == null) throw new BusinessException("chucDanh", "Chọn chức danh.");
        if (f.chucDanh() == VaiTro.AD) throw new BusinessException("chucDanh", "Không tạo thêm nhân viên quản trị hệ thống từ màn hình này.");
        return Tx.write(c -> {
            if (dao.phoneUsed(c, n.soDienThoai(), null)) throw new BusinessException("soDienThoai", "Số điện thoại đã thuộc nhân viên khác.");
            String ma = maDao.next(c, "NV");
            dao.insert(c, new NhanVien(ma, n.hoTen(), n.soDienThoai(), f.chucDanh(), TrangThaiNhanVien.DANG_LAM_VIEC, n.ngaySinh(), n.gioiTinh(),
                    n.email(), n.ngayVaoLam() == null ? Now.today() : n.ngayVaoLam(), n.chuyenMon(), null));
            Audit.log(c, "Nhân viên", "Thêm nhân viên (" + f.chucDanh().label() + ")", ma, n.hoTen());
            return ma;
        });
    }

    /** Sửa hồ sơ; chức danh không đổi tại đây (gắn với phân công, lịch hẹn đã có). */
    public void capNhat(String ma, NhanVienForm f) {
        Session.require(Quyen.NHAN_VIEN);
        NhanVien n = validate(ma, f);
        Tx.run(c -> {
            NhanVien cu = dao.find(c, ma).orElseThrow(() -> new BusinessException("Không tìm thấy nhân viên."));
            if (!cu.dangLamViec()) throw new BusinessException("Nhân viên đã nghỉ việc; hồ sơ chỉ còn để tra cứu.");
            if (dao.phoneUsed(c, n.soDienThoai(), ma)) throw new BusinessException("soDienThoai", "Số điện thoại đã thuộc nhân viên khác.");
            if (dao.update(c, n) == 0) throw TaiKhoanService.stale();
            Audit.log(c, "Nhân viên", "Cập nhật hồ sơ nhân viên", ma, n.hoTen());
        });
    }

    private NhanVien validate(String ma, NhanVienForm f) {
        String hoTen = Validator.maxLen(Validator.required(f.hoTen(), "hoTen", "Họ tên"), 100, "hoTen", "Họ tên");
        String sdt = Validator.phone(f.soDienThoai(), "soDienThoai");
        String email = Validator.email(f.email(), "email");
        if (f.ngaySinh() != null && f.ngaySinh().isAfter(Now.today().minusYears(16))) {
            throw new BusinessException("ngaySinh", "Ngày sinh không hợp lệ (nhân viên phải từ 16 tuổi).");
        }
        return new NhanVien(ma, hoTen, sdt, f.chucDanh(), TrangThaiNhanVien.DANG_LAM_VIEC, f.ngaySinh(), f.gioiTinh(), email, f.ngayVaoLam(),
                Validator.optional(f.chuyenMon()), null);
    }

    /** Liệt kê các việc còn mở cần xử lý trước khi cho nghỉ việc. */
    public NghiViecCheck kiemTraNghiViec(String ma) {
        Session.require(Quyen.NHAN_VIEN);
        return Tx.read(c -> check(c, ma));
    }

    private NghiViecCheck check(Connection c, String ma) throws SQLException {
        NhanVien nv = dao.find(c, ma).orElseThrow(() -> new BusinessException("Không tìm thấy nhân viên " + ma + "."));
        boolean bs = nv.chucDanh() == VaiTro.BS;
        return new NghiViecCheck(nv, pcDao.openFrom(c, ma, LocalDate.of(2000, 1, 1)),
                bs ? lhDao.pendingOfDoctor(c, ma) : List.of(), bs ? pkDao.openOfDoctor(c, ma) : List.of(),
                bgDao.pendingOf(c, ma), tkDao.findByNhanVien(c, ma).isPresent());
    }

    /**
     * Hoàn tất nghỉ việc: chỉ khi không còn việc mở. Nhân viên → NGHI_VIEC, tài khoản → VO_HIEU (chặn đăng nhập).
     * Không xóa nhân viên/tài khoản và không sửa lịch sử đã có.
     */
    public void hoanTatNghiViec(String ma, LocalDate ngayNghi, String lyDo) {
        Session.require(Quyen.NHAN_VIEN);
        String ly = Validator.required(lyDo, "lyDo", "Lý do nghỉ việc");
        if (ngayNghi == null) throw new BusinessException("ngayNghi", "Nhập ngày nghỉ việc.");
        if (ngayNghi.isAfter(Now.today())) throw new BusinessException("ngayNghi", "Ngày nghỉ việc không được sau hôm nay; hoàn tất thủ tục vào ngày nghỉ.");
        Tx.run(c -> {
            NghiViecCheck ck = check(c, ma);
            NhanVien nv = ck.nv();
            if (!nv.dangLamViec()) throw new BusinessException("Nhân viên đã ở trạng thái nghỉ việc.");
            if (nv.chucDanh() == VaiTro.AD) throw new BusinessException("Không làm thủ tục nghỉ việc cho tài khoản quản trị hệ thống.");
            if (ma.equals(Session.user().maNhanVien())) throw new BusinessException("Không thể tự làm thủ tục nghỉ việc cho chính mình.");
            if (!ck.duDieuKien()) {
                throw new BusinessException("Chưa đủ điều kiện: còn " + ck.phanCong().size() + " phân công, " + ck.lichHen().size() + " lịch hẹn, "
                        + ck.phieuKham().size() + " phiếu khám, " + ck.banGiao().size() + " biên bản bàn giao chưa xử lý.");
            }
            if (nv.ngayVaoLam() != null && ngayNghi.isBefore(nv.ngayVaoLam())) throw new BusinessException("ngayNghi", "Ngày nghỉ việc trước ngày vào làm.");
            if (dao.setNghiViec(c, ma, ngayNghi) == 0) throw TaiKhoanService.stale();
            TaiKhoan tk = tkDao.findByNhanVien(c, ma).orElse(null);
            if (tk != null) tkDao.voHieu(c, tk.maTaiKhoan(), "Nhân viên nghỉ việc từ " + Fmt.date(ngayNghi));
            Audit.log(c, "Nhân viên", "Hoàn tất thủ tục nghỉ việc", ma,
                    nv.hoTen() + " — " + ly + (tk == null ? "" : "; vô hiệu tài khoản " + tk.tenDangNhap()));
        });
    }
}
