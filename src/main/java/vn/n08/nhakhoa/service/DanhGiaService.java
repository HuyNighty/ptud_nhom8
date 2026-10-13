package vn.n08.nhakhoa.service;

import vn.n08.nhakhoa.dao.DanhGiaDao;
import vn.n08.nhakhoa.dao.MaDao;
import vn.n08.nhakhoa.dao.NhanVienDao;
import vn.n08.nhakhoa.dto.DanhGiaView;
import vn.n08.nhakhoa.model.ChiTietDanhGia;
import vn.n08.nhakhoa.model.DanhGiaNhanSu;
import vn.n08.nhakhoa.model.NhanVien;
import vn.n08.nhakhoa.model.TrangThaiDanhGia;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.util.Tx;
import vn.n08.nhakhoa.util.Validator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** UC25 Đánh giá nhân sự theo kỳ (quý), 5 tiêu chí thang 1–5 (Figma QL-07). Lưu nháp hoặc gửi; đã gửi thì không sửa. */
public class DanhGiaService {
    public static final List<String> TIEU_CHI = List.of("Chuyên môn, tay nghề", "Tuân thủ quy trình vô khuẩn", "Thái độ với bệnh nhân",
            "Phối hợp đồng nghiệp", "Chuyên cần, đúng giờ");

    private final DanhGiaDao dao = new DanhGiaDao();
    private final NhanVienDao nvDao = new NhanVienDao();
    private final MaDao maDao = new MaDao();

    public static String ky(LocalDate d) {
        return d.getYear() + "-Q" + ((d.getMonthValue() - 1) / 3 + 1);
    }

    /** Kỳ hiện tại và 3 kỳ trước. */
    public static List<String> cacKy() {
        List<String> out = new ArrayList<>();
        LocalDate d = Now.today();
        for (int i = 0; i < 4; i++) out.add(ky(d.minusMonths(3L * i)));
        return out;
    }

    public List<DanhGiaView> theoKy(String ky) {
        Session.require(Quyen.DANH_GIA);
        return Tx.read(c -> dao.byKy(c, ky));
    }

    public List<ChiTietDanhGia> chiTiet(String maDanhGia) {
        Session.require(Quyen.DANH_GIA);
        return Tx.read(c -> dao.details(c, maDanhGia));
    }

    /** Lưu đánh giá. diem: 5 số nguyên 1–5 theo thứ tự TIEU_CHI. gui = true để gửi (khóa sửa). */
    public String luu(String maNv, String ky, List<Integer> diem, String nhanXet, boolean gui) {
        Session.require(Quyen.DANH_GIA);
        if (diem == null || diem.size() != TIEU_CHI.size()) throw new BusinessException("diem", "Chấm đủ " + TIEU_CHI.size() + " tiêu chí.");
        for (int i = 0; i < diem.size(); i++) {
            Integer d = diem.get(i);
            if (d == null || d < 1 || d > 5) throw new BusinessException("diem", "Điểm tiêu chí \"" + TIEU_CHI.get(i) + "\" phải từ 1 đến 5.");
        }
        if (!cacKy().contains(ky)) throw new BusinessException("ky", "Chỉ đánh giá kỳ hiện tại hoặc 3 kỳ gần nhất.");
        String nx = Validator.maxLen(Validator.optional(nhanXet), 1000, "nhanXet", "Nhận xét");
        Session.User u = Session.user();
        if (u.maNhanVien().equals(maNv)) throw new BusinessException("Không tự đánh giá chính mình.");
        BigDecimal tb = BigDecimal.valueOf(diem.stream().mapToInt(Integer::intValue).sum()).divide(BigDecimal.valueOf(diem.size()), 2, RoundingMode.HALF_UP);
        TrangThaiDanhGia tt = gui ? TrangThaiDanhGia.DA_GUI : TrangThaiDanhGia.NHAP;
        return Tx.write(c -> {
            NhanVien nv = nvDao.find(c, maNv).orElseThrow(() -> new BusinessException("Không tìm thấy nhân viên."));
            if (nv.chucDanh() == VaiTro.AD) throw new BusinessException("Không đánh giá tài khoản quản trị hệ thống.");
            DanhGiaNhanSu cu = dao.find(c, maNv, ky).orElse(null);
            if (cu == null && !nv.dangLamViec()) throw new BusinessException(nv.hoTen() + " đã nghỉ việc.");
            String ma;
            if (cu == null) {
                ma = maDao.next(c, "DG");
                dao.insert(c, new DanhGiaNhanSu(ma, maNv, u.maNhanVien(), ky, Now.today(), tb, nx, tt));
            } else {
                if (cu.trangThai() == TrangThaiDanhGia.DA_GUI) throw new BusinessException("Đánh giá kỳ " + ky + " của " + nv.hoTen() + " đã gửi, không sửa được.");
                ma = cu.maDanhGia();
                if (dao.updateDraft(c, new DanhGiaNhanSu(ma, maNv, u.maNhanVien(), ky, Now.today(), tb, nx, tt)) == 0) throw TaiKhoanService.stale();
            }
            List<ChiTietDanhGia> items = new ArrayList<>();
            for (int i = 0; i < TIEU_CHI.size(); i++) items.add(new ChiTietDanhGia(ma, i + 1, TIEU_CHI.get(i), diem.get(i)));
            dao.replaceDetails(c, ma, items);
            Audit.log(c, "Đánh giá", gui ? "Gửi đánh giá" : "Lưu nháp đánh giá", ma, nv.hoTen() + " · kỳ " + ky + " · điểm " + tb);
            return ma;
        });
    }
}
