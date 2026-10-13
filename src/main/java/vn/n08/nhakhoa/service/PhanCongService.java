package vn.n08.nhakhoa.service;

import vn.n08.nhakhoa.dao.CaLamViecDao;
import vn.n08.nhakhoa.dao.MaDao;
import vn.n08.nhakhoa.dao.NhanVienDao;
import vn.n08.nhakhoa.dao.NhatKyDao;
import vn.n08.nhakhoa.dao.PhanCongDao;
import vn.n08.nhakhoa.dao.ThamSoDao;
import vn.n08.nhakhoa.dto.NhatKyView;
import vn.n08.nhakhoa.dto.PhanCongView;
import vn.n08.nhakhoa.model.CaLamViec;
import vn.n08.nhakhoa.model.NhanVien;
import vn.n08.nhakhoa.model.PhanCong;
import vn.n08.nhakhoa.model.TrangThaiPhanCong;
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

/**
 * UC24 Quản lý lịch làm việc (phân công ca), UC17/UC20 Xem lịch làm việc, UC18 Nhiệm vụ được phân công.
 * Bỏ người khỏi ca = hủy phân công có lý do; thay người = hủy bản cũ + tạo bản mới trỏ maPhanCongGoc.
 */
public class PhanCongService {
    private final PhanCongDao dao = new PhanCongDao();
    private final CaLamViecDao caDao = new CaLamViecDao();
    private final NhanVienDao nvDao = new NhanVienDao();
    private final ThamSoDao tsDao = new ThamSoDao();
    private final NhatKyDao nkDao = new NhatKyDao();
    private final MaDao maDao = new MaDao();

    public List<CaLamViec> danhSachCa() {
        Session.user();
        return Tx.read(caDao::all);
    }

    /** CaLamViec.capNhatCa: tên, giờ và định mức nhân sự tối thiểu của ca (dùng để cảnh báo ca thiếu người). */
    public void capNhatCa(CaLamViec ca) {
        Session.require(Quyen.PHAN_CONG);
        String ten = Validator.maxLen(Validator.required(ca.tenCa(), "tenCa", "Tên ca"), 50, "tenCa", "Tên ca");
        if (ca.gioBatDau() == null || ca.gioKetThuc() == null || !ca.gioKetThuc().isAfter(ca.gioBatDau())) {
            throw new BusinessException("gio", "Giờ kết thúc phải sau giờ bắt đầu.");
        }
        if (ca.soBacSi() < 0 || ca.soYTa() < 0 || ca.soLeTan() < 0) throw new BusinessException("dinhMuc", "Định mức không âm.");
        Tx.run(c -> {
            CaLamViec cu = caDao.find(c, ca.maCa()).orElseThrow(() -> new BusinessException("Không tìm thấy ca."));
            caDao.update(c, new CaLamViec(ca.maCa(), ten, ca.gioBatDau(), ca.gioKetThuc(), ca.soBacSi(), ca.soYTa(), ca.soLeTan()));
            Audit.log(c, "Phân công", "Cập nhật ca làm việc", ca.maCa(), cu + " → " + ten + " (" + ca.gioBatDau() + "–" + ca.gioKetThuc()
                    + "), định mức BS " + ca.soBacSi() + " · YT " + ca.soYTa() + " · LT " + ca.soLeTan());
        });
    }

    /** Lịch làm việc trong khoảng ngày (mọi trạng thái); maNv = null để xem toàn bộ. */
    public List<PhanCongView> lich(LocalDate tu, LocalDate den, String maNv) {
        Session.require(Quyen.LICH_LAM_VIEC);
        return Tx.read(c -> dao.range(c, tu, den, maNv));
    }

    /** Lịch sử của một phân công: bản thay thế và nhật ký thao tác. */
    public List<NhatKyView> lichSu(String maPhanCong) {
        Session.require(Quyen.LICH_LAM_VIEC);
        return Tx.read(c -> nkDao.byObject(c, maPhanCong));
    }

    public List<PhanCongView> banThayThe(String maPhanCong) {
        Session.require(Quyen.LICH_LAM_VIEC);
        return Tx.read(c -> dao.replacements(c, maPhanCong));
    }

    public String them(String maNv, String maCa, LocalDate ngay, String nhiemVu) {
        Session.require(Quyen.PHAN_CONG);
        String nv = Validator.required(nhiemVu, "nhiemVu", "Nhiệm vụ");
        return Tx.write(c -> {
            String ma = taoMoi(c, maNv, maCa, ngay, nv, null);
            Audit.log(c, "Phân công", "Tạo phân công", ma, maNv + " · " + maCa + " · " + Fmt.date(ngay) + " · " + nv);
            return ma;
        });
    }

    private String taoMoi(Connection c, String maNv, String maCa, LocalDate ngay, String nhiemVu, String maGoc) throws SQLException {
        kiemTraGan(c, maNv, maCa, ngay);
        String ma = maDao.next(c, "PC");
        dao.insert(c, new PhanCong(ma, maNv, maCa, ngay, nhiemVu, TrangThaiPhanCong.CHO_HIEU_LUC, null, maGoc));
        return ma;
    }

    private void kiemTraGan(Connection c, String maNv, String maCa, LocalDate ngay) throws SQLException {
        if (ngay == null) throw new BusinessException("ngay", "Chọn ngày làm.");
        if (ngay.isBefore(Now.today())) throw new BusinessException("ngay", "Không phân công cho ngày đã qua.");
        NhanVien nv = nvDao.find(c, maNv).orElseThrow(() -> new BusinessException("nhanVien", "Chọn nhân viên."));
        if (!nv.dangLamViec()) throw new BusinessException("nhanVien", nv.hoTen() + " đã nghỉ việc.");
        if (nv.chucDanh() == VaiTro.AD) throw new BusinessException("nhanVien", "Không phân ca cho tài khoản quản trị hệ thống.");
        CaLamViec ca = caDao.find(c, maCa).orElseThrow(() -> new BusinessException("ca", "Chọn ca làm việc."));
        if (dao.existsActive(c, maNv, ngay, maCa)) {
            throw new BusinessException("ca", nv.hoTen() + " đã có phân công " + ca.tenCa() + " ngày " + Fmt.date(ngay) + ".");
        }
        int max = tsDao.getInt(c, "SO_CA_TOI_DA_NGAY", 2);
        if (dao.countActive(c, maNv, ngay) >= max) {
            throw new BusinessException("ca", nv.hoTen() + " đã đủ " + max + " ca trong ngày " + Fmt.date(ngay) + ".");
        }
    }

    public void huy(String maPhanCong, String lyDo) {
        Session.require(Quyen.PHAN_CONG);
        String ly = Validator.required(lyDo, "lyDo", "Lý do hủy");
        Tx.run(c -> {
            PhanCong pc = load(c, maPhanCong);
            if (!pc.conHieuLuc()) throw new BusinessException("Chỉ hủy được phân công chờ hiệu lực hoặc đang thực hiện.");
            if (dao.cancel(c, maPhanCong, ly) == 0) throw TaiKhoanService.stale();
            Audit.log(c, "Phân công", "Hủy phân công", maPhanCong, ly);
        });
    }

    /** Thay người: hủy phân công cũ (lý do) và tạo phân công mới cùng ca/ngày/nhiệm vụ cho người thay. */
    public String thayNguoi(String maPhanCong, String maNvMoi, String lyDo) {
        Session.require(Quyen.PHAN_CONG);
        String ly = Validator.required(lyDo, "lyDo", "Lý do thay người");
        return Tx.write(c -> {
            PhanCong pc = load(c, maPhanCong);
            if (pc.trangThai() != TrangThaiPhanCong.CHO_HIEU_LUC) throw new BusinessException("Chỉ thay người cho phân công đang chờ hiệu lực.");
            if (pc.maNhanVien().equals(maNvMoi)) throw new BusinessException("nhanVien", "Chọn người thay khác người hiện tại.");
            NhanVien cu = nvDao.find(c, pc.maNhanVien()).orElseThrow();
            NhanVien moi = nvDao.find(c, maNvMoi).orElseThrow(() -> new BusinessException("nhanVien", "Chọn người thay."));
            if (moi.chucDanh() != cu.chucDanh()) throw new BusinessException("nhanVien", "Người thay phải cùng chức danh (" + cu.chucDanh().label() + ").");
            if (dao.cancel(c, maPhanCong, "Thay người: " + ly) == 0) throw TaiKhoanService.stale();
            String ma = taoMoi(c, maNvMoi, pc.maCa(), pc.ngayLam(), pc.nhiemVu(), maPhanCong);
            Audit.log(c, "Phân công", "Thay người", maPhanCong, cu.hoTen() + " → " + moi.hoTen() + " (" + ma + "): " + ly);
            Audit.log(c, "Phân công", "Tạo phân công thay thế", ma, "Thay cho " + maPhanCong);
            return ma;
        });
    }

    public void suaNhiemVu(String maPhanCong, String nhiemVu) {
        Session.require(Quyen.PHAN_CONG);
        String nv = Validator.required(nhiemVu, "nhiemVu", "Nhiệm vụ");
        Tx.run(c -> {
            PhanCong pc = load(c, maPhanCong);
            if (dao.setNhiemVu(c, maPhanCong, nv) == 0) throw new BusinessException("Phân công đã kết thúc hoặc đã hủy.");
            Audit.log(c, "Phân công", "Sửa nhiệm vụ", maPhanCong, pc.nhiemVu() + " → " + nv);
        });
    }

    /** Bắt đầu thực hiện (UC18): người được phân công hoặc quản lý, chỉ trong ngày làm. */
    public void batDau(String maPhanCong) {
        Session.require(Quyen.LICH_LAM_VIEC);
        Tx.run(c -> {
            PhanCong pc = load(c, maPhanCong);
            requireSelfOrManager(pc);
            if (!pc.ngayLam().equals(Now.today())) throw new BusinessException("Chỉ bắt đầu nhiệm vụ trong đúng ngày làm (" + Fmt.date(pc.ngayLam()) + ").");
            if (dao.setTrangThai(c, maPhanCong, TrangThaiPhanCong.CHO_HIEU_LUC, TrangThaiPhanCong.DANG_THUC_HIEN) == 0) {
                throw new BusinessException("Phân công không ở trạng thái chờ hiệu lực.");
            }
            Audit.log(c, "Phân công", "Bắt đầu nhiệm vụ", maPhanCong, null);
        });
    }

    public void hoanTat(String maPhanCong) {
        Session.require(Quyen.LICH_LAM_VIEC);
        Tx.run(c -> {
            PhanCong pc = load(c, maPhanCong);
            requireSelfOrManager(pc);
            if (dao.setTrangThai(c, maPhanCong, TrangThaiPhanCong.DANG_THUC_HIEN, TrangThaiPhanCong.HOAN_THANH) == 0) {
                throw new BusinessException("Chỉ hoàn tất được nhiệm vụ đang thực hiện.");
            }
            Audit.log(c, "Phân công", "Hoàn tất nhiệm vụ", maPhanCong, null);
        });
    }

    /** Sao chép phân công còn hiệu lực của tuần trước sang tuần có ngày đầu tuần cho trước; bỏ qua ngày đã qua và chỗ trùng. */
    public int[] saoChepTuanTruoc(LocalDate thuHai) {
        Session.require(Quyen.PHAN_CONG);
        return Tx.write(c -> {
            int tao = 0, boQua = 0;
            for (var v : dao.range(c, thuHai.minusDays(7), thuHai.minusDays(1), null)) {
                PhanCong p = v.pc();
                if (p.trangThai() == TrangThaiPhanCong.DA_HUY) continue;
                LocalDate ngay = p.ngayLam().plusDays(7);
                try {
                    taoMoi(c, p.maNhanVien(), p.maCa(), ngay, p.nhiemVu(), null);
                    tao++;
                } catch (BusinessException e) {
                    boQua++;
                }
            }
            Audit.log(c, "Phân công", "Sao chép tuần trước", "Tuần " + Fmt.date(thuHai), "Tạo " + tao + ", bỏ qua " + boQua);
            return new int[]{tao, boQua};
        });
    }

    private PhanCong load(Connection c, String ma) throws SQLException {
        return dao.find(c, ma).orElseThrow(() -> new BusinessException("Không tìm thấy phân công " + ma + "."));
    }

    private static void requireSelfOrManager(PhanCong pc) {
        Session.User u = Session.user();
        if (!u.maNhanVien().equals(pc.maNhanVien()) && !Session.can(Quyen.PHAN_CONG)) {
            throw new BusinessException("Chỉ người được phân công hoặc quản lý mới cập nhật nhiệm vụ này.");
        }
    }
}
