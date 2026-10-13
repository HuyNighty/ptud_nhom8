package vn.n08.nhakhoa.service;

import vn.n08.nhakhoa.dao.DieuChinhDao;
import vn.n08.nhakhoa.dao.LoHangDao;
import vn.n08.nhakhoa.dao.MaDao;
import vn.n08.nhakhoa.dto.DieuChinhDong;
import vn.n08.nhakhoa.dto.DieuChinhView;
import vn.n08.nhakhoa.dto.XuatKhoForm;
import vn.n08.nhakhoa.model.ChiTietDieuChinhKho;
import vn.n08.nhakhoa.model.LoHang;
import vn.n08.nhakhoa.model.LoaiDieuChinh;
import vn.n08.nhakhoa.model.PhieuDieuChinhKho;
import vn.n08.nhakhoa.model.TrangThaiDuyet;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.util.Tx;
import vn.n08.nhakhoa.util.Validator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * UC28 Kiểm kê kho, UC35 Hủy / hao hụt — một loại phiếu điều chỉnh, tồn chỉ đổi khi được tài khoản KHÁC duyệt (Figma QL-10).
 * Dòng phiếu: XuatKhoForm.Dong(maLo, soLuongThucTe).
 */
public class DieuChinhKhoService {
    private final DieuChinhDao dao = new DieuChinhDao();
    private final LoHangDao loDao = new LoHangDao();
    private final MaDao maDao = new MaDao();

    public List<DieuChinhView> danhSach(TrangThaiDuyet tt) {
        Session.requireAny(Quyen.KIEM_KE, Quyen.DUYET_KHO);
        return Tx.read(c -> dao.search(c, tt));
    }

    public List<DieuChinhDong> chiTiet(String ma) {
        Session.requireAny(Quyen.KIEM_KE, Quyen.DUYET_KHO);
        return Tx.read(c -> dao.lines(c, ma));
    }

    public String lap(LoaiDieuChinh loai, String lyDo, List<XuatKhoForm.Dong> thucTe) {
        Session.require(Quyen.KIEM_KE);
        if (loai == null) throw new BusinessException("loai", "Chọn loại phiếu.");
        String ly = Validator.maxLen(Validator.required(lyDo, "lyDo", "Lý do / ghi chú"), 500, "lyDo", "Lý do");
        if (thucTe == null || thucTe.isEmpty()) throw new BusinessException("dong", "Thêm ít nhất một lô cần điều chỉnh.");
        return Tx.write(c -> {
            String ma = maDao.next(c, "DC");
            dao.insert(c, new PhieuDieuChinhKho(ma, loai, Session.user().maTaiKhoan(), Now.now(), ly, TrangThaiDuyet.CHO_DUYET, null, null, null));
            Set<String> seen = new HashSet<>();
            for (XuatKhoForm.Dong d : thucTe) {
                LoHang lo = loDao.find(c, d.maLo()).orElseThrow(() -> new BusinessException("dong", "Lô " + d.maLo() + " không tồn tại."));
                if (!seen.add(lo.maLo())) throw new BusinessException("dong", "Lô " + lo.soLo() + " bị lặp.");
                if (d.soLuong() < 0) throw new BusinessException("dong", "Số lượng thực tế không âm.");
                if (d.soLuong() == lo.soLuongTon()) throw new BusinessException("dong", "Lô " + lo.soLo() + " không chênh lệch (thực tế = hệ thống " + lo.soLuongTon() + ").");
                if (loai != LoaiDieuChinh.KIEM_KE && d.soLuong() > lo.soLuongTon()) {
                    throw new BusinessException("dong", "Phiếu hủy / hao hụt chỉ được giảm tồn (lô " + lo.soLo() + ").");
                }
                if (loai == LoaiDieuChinh.HET_HAN && !lo.daHetHan(Now.today())) throw new BusinessException("dong", "Lô " + lo.soLo() + " chưa hết hạn; chọn loại Hư hỏng hoặc Kiểm kê.");
                if (dao.lotPending(c, lo.maLo())) throw new BusinessException("dong", "Lô " + lo.soLo() + " đang nằm trong phiếu điều chỉnh khác chờ duyệt.");
                dao.insertLine(c, new ChiTietDieuChinhKho(ma, lo.maLo(), lo.soLuongTon(), d.soLuong()));
            }
            Audit.log(c, "Kho", "Lập phiếu điều chỉnh (" + loai.label() + ")", ma, ly);
            return ma;
        });
    }

    /** Duyệt / từ chối. Người duyệt khác người lập. Khi duyệt: tồn lô phải còn bằng số lúc lập phiếu. */
    public void xuLy(String ma, boolean dongY, String ghiChu) {
        Session.require(Quyen.DUYET_KHO);
        String gc = Validator.optional(ghiChu);
        if (!dongY && gc == null) throw new BusinessException("ghiChu", "Nhập lý do từ chối.");
        Session.User u = Session.user();
        Tx.run(c -> {
            PhieuDieuChinhKho p = dao.find(c, ma).orElseThrow(() -> new BusinessException("Không tìm thấy phiếu " + ma + "."));
            if (p.trangThai() != TrangThaiDuyet.CHO_DUYET) throw new BusinessException("Phiếu đã được xử lý.");
            if (p.maTaiKhoanTao().equals(u.maTaiKhoan())) {
                throw new BusinessException("Phiếu " + ma + " do chính tài khoản " + u.tenDangNhap() + " lập nên không được duyệt hay từ chối. Cần tài khoản khác có quyền duyệt.");
            }
            if (dongY) {
                for (DieuChinhDong d : dao.lines(c, ma)) {
                    if (loDao.setStock(c, d.ct().maLo(), d.ct().soLuongHeThong(), d.ct().soLuongThucTe()) == 0) {
                        throw new BusinessException("Tồn lô " + d.soLo() + " đã thay đổi từ khi lập phiếu (" + d.ct().soLuongHeThong() + " → " + d.tonHienTai()
                                + "). Từ chối phiếu này và lập phiếu mới.");
                    }
                }
            }
            if (dao.decide(c, ma, dongY ? TrangThaiDuyet.DA_DUYET : TrangThaiDuyet.TU_CHOI, u.maTaiKhoan(), Now.now(), gc) == 0) throw TaiKhoanService.stale();
            Audit.log(c, "Kho", dongY ? "Duyệt phiếu điều chỉnh" : "Từ chối phiếu điều chỉnh", ma, gc);
        });
    }
}
