package vn.n08.nhakhoa.service;

import vn.n08.nhakhoa.dao.BanGiaoDao;
import vn.n08.nhakhoa.dao.CaLamViecDao;
import vn.n08.nhakhoa.dao.MaDao;
import vn.n08.nhakhoa.dao.NhanVienDao;
import vn.n08.nhakhoa.dto.BanGiaoForm;
import vn.n08.nhakhoa.dto.BanGiaoView;
import vn.n08.nhakhoa.model.BanGiaoCa;
import vn.n08.nhakhoa.model.NhanVien;
import vn.n08.nhakhoa.model.TrangThaiBanGiao;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.util.Tx;
import vn.n08.nhakhoa.util.Validator;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** UC19 Giao nhận ca. Người xác nhận phải khác tài khoản đã lập biên bản (kiểm tra ở Service, DAO và CHECK của CSDL). */
public class BanGiaoService {
    private final BanGiaoDao dao = new BanGiaoDao();
    private final NhanVienDao nvDao = new NhanVienDao();
    private final CaLamViecDao caDao = new CaLamViecDao();
    private final MaDao maDao = new MaDao();

    public List<BanGiaoView> danhSach(LocalDate tu, LocalDate den, TrangThaiBanGiao tt) {
        Session.require(Quyen.BAN_GIAO);
        return Tx.read(c -> dao.search(c, tu, den, tt));
    }

    public String tao(BanGiaoForm f) {
        Session.require(Quyen.BAN_GIAO);
        String noiDung = Validator.maxLen(Validator.required(f.noiDung(), "noiDung", "Nội dung bàn giao"), 1000, "noiDung", "Nội dung");
        if (f.ngay() == null) throw new BusinessException("ngay", "Chọn ngày bàn giao.");
        if (f.ngay().isAfter(Now.today()) || f.ngay().isBefore(Now.today().minusDays(7))) {
            throw new BusinessException("ngay", "Ngày bàn giao phải trong 7 ngày gần đây và không sau hôm nay.");
        }
        if (f.maNguoiGiao() == null || f.maNguoiNhan() == null) throw new BusinessException("nguoiNhan", "Chọn người giao và người nhận.");
        if (f.maNguoiGiao().equals(f.maNguoiNhan())) throw new BusinessException("nguoiNhan", "Người nhận phải khác người giao.");
        Session.User u = Session.user();
        if (!Session.can(Quyen.PHAN_CONG) && !u.maNhanVien().equals(f.maNguoiGiao())) {
            throw new BusinessException("nguoiGiao", "Nhân viên chỉ lập biên bản cho ca mình giao.");
        }
        return Tx.write(c -> {
            NhanVien giao = activeNv(c, f.maNguoiGiao());
            NhanVien nhan = activeNv(c, f.maNguoiNhan());
            caDao.find(c, f.maCaGiao()).orElseThrow(() -> new BusinessException("caGiao", "Chọn ca giao."));
            caDao.find(c, f.maCaNhan()).orElseThrow(() -> new BusinessException("caNhan", "Chọn ca nhận."));
            String ma = maDao.next(c, "BG");
            dao.insert(c, new BanGiaoCa(ma, Now.now(), noiDung, TrangThaiBanGiao.CHO_BAN_GIAO, f.ngay(), giao.maNhanVien(), nhan.maNhanVien(),
                    f.maCaGiao(), f.maCaNhan(), f.coChenhLech(), u.maTaiKhoan(), null, null, null, null, null, null));
            Audit.log(c, "Bàn giao ca", "Lập biên bản bàn giao", ma, giao.hoTen() + " → " + nhan.hoTen() + (f.coChenhLech() ? " (có chênh lệch)" : ""));
            return ma;
        });
    }

    /** Xác nhận nhận ca. Không được xác nhận biên bản do chính tài khoản mình lập. */
    public void xacNhan(String ma, String ghiChu) {
        Session.require(Quyen.BAN_GIAO);
        Session.User u = Session.user();
        Tx.run(c -> {
            BanGiaoCa b = load(c, ma);
            if (b.trangThai() != TrangThaiBanGiao.CHO_BAN_GIAO) throw new BusinessException("Biên bản không còn chờ xác nhận.");
            if (b.maTaiKhoanTao().equals(u.maTaiKhoan())) {
                throw new BusinessException("Biên bản do chính tài khoản " + u.tenDangNhap() + " lập nên không được tự xác nhận. Cần người nhận ca hoặc quản lý khác xác nhận.");
            }
            if (!u.maNhanVien().equals(b.maNguoiNhan()) && !Session.can(Quyen.PHAN_CONG)) {
                throw new BusinessException("Chỉ người nhận ca hoặc quản lý mới xác nhận biên bản này.");
            }
            String gc = Validator.optional(ghiChu);
            if (b.coChenhLech() && gc == null) throw new BusinessException("ghiChu", "Biên bản có chênh lệch: nhập ghi chú xác nhận (đã kiểm tra, cách xử lý).");
            if (dao.confirm(c, ma, u.maTaiKhoan(), Now.now(), gc) == 0) throw TaiKhoanService.stale();
            Audit.log(c, "Bàn giao ca", "Xác nhận bàn giao", ma, gc);
        });
    }

    public void huy(String ma, String lyDo) {
        Session.require(Quyen.BAN_GIAO);
        String ly = Validator.required(lyDo, "lyDo", "Lý do hủy");
        Session.User u = Session.user();
        Tx.run(c -> {
            BanGiaoCa b = load(c, ma);
            if (b.trangThai() != TrangThaiBanGiao.CHO_BAN_GIAO) throw new BusinessException("Chỉ hủy được biên bản đang chờ xác nhận.");
            if (!b.maTaiKhoanTao().equals(u.maTaiKhoan()) && !Session.can(Quyen.PHAN_CONG)) {
                throw new BusinessException("Chỉ người lập biên bản hoặc quản lý mới hủy được.");
            }
            if (dao.cancel(c, ma, u.maTaiKhoan(), Now.now(), ly) == 0) throw TaiKhoanService.stale();
            Audit.log(c, "Bàn giao ca", "Hủy biên bản bàn giao", ma, ly);
        });
    }

    private BanGiaoCa load(Connection c, String ma) throws SQLException {
        return dao.find(c, ma).orElseThrow(() -> new BusinessException("Không tìm thấy biên bản " + ma + "."));
    }

    private NhanVien activeNv(Connection c, String ma) throws SQLException {
        NhanVien n = nvDao.find(c, ma).orElseThrow(() -> new BusinessException("Không tìm thấy nhân viên " + ma + "."));
        if (!n.dangLamViec()) throw new BusinessException(n.hoTen() + " đã nghỉ việc.");
        return n;
    }
}
