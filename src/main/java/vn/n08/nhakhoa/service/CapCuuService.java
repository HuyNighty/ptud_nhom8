package vn.n08.nhakhoa.service;

import vn.n08.nhakhoa.dao.CapCuuDao;
import vn.n08.nhakhoa.dao.HangHoaDao;
import vn.n08.nhakhoa.dao.LoHangDao;
import vn.n08.nhakhoa.dao.MaDao;
import vn.n08.nhakhoa.dao.ThamSoDao;
import vn.n08.nhakhoa.dto.CapCuuView;
import vn.n08.nhakhoa.dto.ChiTietKiemTraView;
import vn.n08.nhakhoa.dto.KiemTraCapCuuForm;
import vn.n08.nhakhoa.dto.KiemTraCapCuuView;
import vn.n08.nhakhoa.dto.XuatKhoForm;
import vn.n08.nhakhoa.model.ChiTietKiemTraCapCuu;
import vn.n08.nhakhoa.model.DanhMucCapCuu;
import vn.n08.nhakhoa.model.KetQuaKiemTra;
import vn.n08.nhakhoa.model.KiemTraCapCuu;
import vn.n08.nhakhoa.model.LoHang;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.util.Tx;
import vn.n08.nhakhoa.util.Validator;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UC34 Kiểm tra hộp thuốc cấp cứu. Danh mục cấu hình được; dữ liệu cài sẵn là DỮ LIỆU MẪU, không phải danh mục chuyên môn chính thức.
 * Một mục đạt khi số lượng ≥ tối thiểu và hạn dùng còn hơn CC_NGUONG_HSD_NGAY ngày (ngưỡng cấu hình, giả định).
 */
public class CapCuuService {
    private final CapCuuDao dao = new CapCuuDao();
    private final HangHoaDao hhDao = new HangHoaDao();
    private final LoHangDao loDao = new LoHangDao();
    private final ThamSoDao tsDao = new ThamSoDao();
    private final MaDao maDao = new MaDao();
    private final KhoService kho = new KhoService();

    public record CauHinh(int nguongHsdNgay, int chuKyNgay) {}

    public CauHinh cauHinh() {
        Session.user();
        return Tx.read(c -> new CauHinh(tsDao.getInt(c, "CC_NGUONG_HSD_NGAY", 30), tsDao.getInt(c, "CC_CHU_KY_KIEM_TRA_NGAY", 7)));
    }

    public void luuCauHinh(int nguongHsd, int chuKy) {
        Session.require(Quyen.CAP_CUU);
        if (nguongHsd < 0 || nguongHsd > 365) throw new BusinessException("nguong", "Ngưỡng hạn dùng từ 0 đến 365 ngày.");
        if (chuKy < 1 || chuKy > 90) throw new BusinessException("chuKy", "Chu kỳ kiểm tra từ 1 đến 90 ngày.");
        Tx.run(c -> {
            tsDao.set(c, "CC_NGUONG_HSD_NGAY", String.valueOf(nguongHsd));
            tsDao.set(c, "CC_CHU_KY_KIEM_TRA_NGAY", String.valueOf(chuKy));
            Audit.log(c, "Hộp cấp cứu", "Đổi cấu hình kiểm tra", "ThamSoHeThong", "Ngưỡng HSD " + nguongHsd + " ngày, chu kỳ " + chuKy + " ngày");
        });
    }

    public List<CapCuuView> danhMuc() {
        Session.requireAny(Quyen.CAP_CUU, Quyen.KHO);
        return Tx.read(c -> {
            int nguong = tsDao.getInt(c, "CC_NGUONG_HSD_NGAY", 30);
            return dao.listWithLink(c).stream().map(m -> new CapCuuView(m.muc(), m.tenLienKet(), tinhTrang(m.muc(), nguong))).toList();
        });
    }

    static CapCuuView.TinhTrang tinhTrang(DanhMucCapCuu m, int nguong) {
        LocalDate h = Now.today();
        if (!m.dangApDung()) return CapCuuView.TinhTrang.NGUNG;
        if (m.hanSuDungGanNhat() != null && !m.hanSuDungGanNhat().isAfter(h)) return CapCuuView.TinhTrang.HET_HAN;
        if (m.soLuongHienCo() < m.soLuongToiThieu()) return CapCuuView.TinhTrang.THIEU;
        if (m.hanSuDungGanNhat() != null && !m.hanSuDungGanNhat().isAfter(h.plusDays(nguong))) return CapCuuView.TinhTrang.SAP_HET_HAN;
        return CapCuuView.TinhTrang.DU;
    }

    /** Thêm (maMuc null) hoặc sửa mục danh mục. */
    public String luuMuc(DanhMucCapCuu m) {
        Session.require(Quyen.CAP_CUU);
        String ten = Validator.maxLen(Validator.required(m.tenMuc(), "ten", "Tên mục"), 150, "ten", "Tên mục");
        String dvt = Validator.maxLen(Validator.required(m.donViTinh(), "donViTinh", "Đơn vị tính"), 30, "donViTinh", "Đơn vị tính");
        if (m.soLuongToiThieu() <= 0) throw new BusinessException("toiThieu", "Số lượng tối thiểu phải lớn hơn 0.");
        if (m.maThuoc() != null && m.maVatTu() != null) throw new BusinessException("lienKet", "Chỉ liên kết một thuốc hoặc một vật tư.");
        return Tx.write(c -> {
            if (m.maThuoc() != null) hhDao.findThuoc(c, m.maThuoc()).orElseThrow(() -> new BusinessException("lienKet", "Thuốc không tồn tại."));
            if (m.maVatTu() != null) hhDao.findVatTu(c, m.maVatTu()).orElseThrow(() -> new BusinessException("lienKet", "Vật tư không tồn tại."));
            String gc = Validator.maxLen(Validator.optional(m.ghiChu()), 300, "ghiChu", "Ghi chú");
            if (m.maMuc() == null) {
                String ma = maDao.next(c, "CC");
                dao.insert(c, new DanhMucCapCuu(ma, ten, dvt, m.soLuongToiThieu(), 0, null, m.maThuoc(), m.maVatTu(), true, false, gc));
                Audit.log(c, "Hộp cấp cứu", "Thêm mục danh mục", ma, ten);
                return ma;
            }
            if (dao.update(c, new DanhMucCapCuu(m.maMuc(), ten, dvt, m.soLuongToiThieu(), 0, null, m.maThuoc(), m.maVatTu(), true, false, gc)) == 0) {
                throw new BusinessException("Không tìm thấy mục.");
            }
            Audit.log(c, "Hộp cấp cứu", "Sửa mục danh mục", m.maMuc(), ten);
            return m.maMuc();
        });
    }

    public void datApDung(String maMuc, boolean apDung) {
        Session.require(Quyen.CAP_CUU);
        Tx.run(c -> {
            if (dao.setActive(c, maMuc, apDung) == 0) throw new BusinessException("Không tìm thấy mục.");
            Audit.log(c, "Hộp cấp cứu", apDung ? "Áp dụng lại mục" : "Ngừng áp dụng mục", maMuc, null);
        });
    }

    /** Ghi kết quả một lần kiểm tra: phải nhập đủ mọi mục đang áp dụng; cập nhật số hiện có theo thực tế. */
    public String kiemTra(KiemTraCapCuuForm f) {
        Session.require(Quyen.CAP_CUU);
        return Tx.write(c -> {
            int nguong = tsDao.getInt(c, "CC_NGUONG_HSD_NGAY", 30);
            List<DanhMucCapCuu> active = dao.active(c);
            if (active.isEmpty()) throw new BusinessException("Danh mục chưa có mục nào đang áp dụng.");
            Map<String, KiemTraCapCuuForm.Dong> input = new HashMap<>();
            for (KiemTraCapCuuForm.Dong d : f.dong()) input.put(d.maMuc(), d);
            String ma = maDao.next(c, "KT");
            int dat = 0;
            StringBuilder thieu = new StringBuilder();
            LocalDate gioiHan = Now.today().plusDays(nguong);
            dao.insertKiemTra(c, new KiemTraCapCuu(ma, Now.now(), Session.user().maNhanVien(), KetQuaKiemTra.DAT, null));
            for (DanhMucCapCuu m : active) {
                KiemTraCapCuuForm.Dong d = input.get(m.maMuc());
                if (d == null) throw new BusinessException("dong", "Chưa nhập kết quả cho mục " + m.tenMuc() + ".");
                if (d.soLuongThucTe() < 0) throw new BusinessException("dong", m.tenMuc() + ": số lượng không âm.");
                boolean ok = d.soLuongThucTe() >= m.soLuongToiThieu() && (d.hanSuDung() == null || d.hanSuDung().isAfter(gioiHan));
                if (ok) dat++; else thieu.append(thieu.length() == 0 ? "" : ", ").append(m.tenMuc());
                dao.insertKiemTraLine(c, new ChiTietKiemTraCapCuu(ma, m.maMuc(), d.soLuongThucTe(), d.hanSuDung(), ok));
                dao.setHienCo(c, m.maMuc(), d.soLuongThucTe(), d.hanSuDung());
            }
            KetQuaKiemTra kq = dat == active.size() ? KetQuaKiemTra.DAT : KetQuaKiemTra.CAN_BO_SUNG;
            String gc = Validator.optional(f.ghiChu());
            String ghiChu = kq == KetQuaKiemTra.DAT ? gc : "Cần bổ sung/thay: " + thieu + (gc == null ? "" : ". " + gc);
            dao.setKetQua(c, ma, kq, cut(ghiChu));
            Audit.log(c, "Hộp cấp cứu", "Kiểm tra hộp cấp cứu — " + kq.label(), ma, dat + "/" + active.size() + " mục đạt");
            return ma;
        });
    }

    private static String cut(String s) { return s == null || s.length() <= 500 ? s : s.substring(0, 499); }

    public List<KiemTraCapCuuView> lichSu() {
        Session.requireAny(Quyen.CAP_CUU, Quyen.KHO);
        return Tx.read(c -> dao.history(c, 100));
    }

    public List<ChiTietKiemTraView> chiTietKiemTra(String ma) {
        Session.requireAny(Quyen.CAP_CUU, Quyen.KHO);
        return Tx.read(c -> dao.lines(c, ma));
    }

    /** Bổ sung từ kho: xuất kho lô còn hạn của thuốc/vật tư liên kết, cộng vào số hiện có của mục. */
    public String boSungTuKho(String maMuc, String maLo, int soLuong) {
        Session.require(Quyen.CAP_CUU);
        Session.require(Quyen.KHO);
        return Tx.write(c -> {
            DanhMucCapCuu m = dao.find(c, maMuc).orElseThrow(() -> new BusinessException("Không tìm thấy mục."));
            if (m.maThuoc() == null && m.maVatTu() == null) throw new BusinessException("Mục chưa liên kết thuốc/vật tư trong kho; cập nhật danh mục trước.");
            LoHang lo = loDao.find(c, maLo).orElseThrow(() -> new BusinessException("lo", "Chọn lô."));
            boolean dungHang = m.maThuoc() != null ? m.maThuoc().equals(lo.maThuoc()) : m.maVatTu().equals(lo.maVatTu());
            if (!dungHang) throw new BusinessException("lo", "Lô không thuộc mặt hàng liên kết của mục.");
            String px = kho.xuat(c, new XuatKhoForm(KhoService.BO_PHAN_CAP_CUU, Session.user().hoTen(), "Bổ sung mục " + m.maMuc() + " — " + m.tenMuc(),
                    null, List.of(new XuatKhoForm.Dong(maLo, soLuong))));
            LocalDate hsd = m.soLuongHienCo() == 0 || m.hanSuDungGanNhat() == null ? lo.hanSuDung()
                    : lo.hanSuDung() == null ? m.hanSuDungGanNhat() : (lo.hanSuDung().isBefore(m.hanSuDungGanNhat()) ? lo.hanSuDung() : m.hanSuDungGanNhat());
            dao.setHienCo(c, maMuc, m.soLuongHienCo() + soLuong, hsd);
            Audit.log(c, "Hộp cấp cứu", "Bổ sung từ kho", maMuc, px + " · lô " + lo.soLo() + " · +" + soLuong);
            return px;
        });
    }
}
