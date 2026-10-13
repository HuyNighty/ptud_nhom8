package vn.n08.nhakhoa.service;

import vn.n08.nhakhoa.dao.DichVuDao;
import vn.n08.nhakhoa.dao.MaDao;
import vn.n08.nhakhoa.model.DichVu;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.util.Tx;
import vn.n08.nhakhoa.util.Validator;

import java.util.List;

/**
 * Bảng giá dịch vụ (DichVu.capNhatDonGia trong class diagram). Đổi giá không ảnh hưởng hóa đơn đã lập
 * vì ChiTietHoaDon lưu donGiaApDung tại thời điểm lập.
 */
public class DichVuService {
    private final DichVuDao dao = new DichVuDao();
    private final MaDao maDao = new MaDao();

    public List<DichVu> danhSach() {
        Session.requireAny(Quyen.BANG_GIA, Quyen.THU_NGAN, Quyen.KHAM, Quyen.KE_HOACH);
        return Tx.read(c -> dao.all(c, false));
    }

    public String luu(DichVu d) {
        Session.require(Quyen.BANG_GIA);
        String ten = Validator.maxLen(Validator.required(d.tenDichVu(), "ten", "Tên dịch vụ"), 100, "ten", "Tên dịch vụ");
        if (d.donGia() == null || d.donGia().signum() < 0) throw new BusinessException("donGia", "Đơn giá không âm.");
        String mo = Validator.maxLen(Validator.optional(d.moTa()), 300, "moTa", "Mô tả");
        return Tx.write(c -> {
            if (dao.nameUsed(c, ten, d.maDichVu())) throw new BusinessException("ten", "Đã có dịch vụ \"" + ten + "\".");
            if (d.maDichVu() == null) {
                String ma = maDao.next(c, "DV");
                dao.insert(c, new DichVu(ma, ten, d.donGia(), mo, d.laCanLamSang(), true));
                Audit.log(c, "Bảng giá", "Thêm dịch vụ", ma, ten + " · " + Fmt.money(d.donGia()));
                return ma;
            }
            DichVu cu = dao.find(c, d.maDichVu()).orElseThrow(() -> new BusinessException("Không tìm thấy dịch vụ."));
            dao.update(c, new DichVu(d.maDichVu(), ten, d.donGia(), mo, d.laCanLamSang(), d.dangApDung()));
            Audit.log(c, "Bảng giá", cu.donGia().compareTo(d.donGia()) == 0 ? "Sửa dịch vụ" : "Cập nhật đơn giá", d.maDichVu(),
                    ten + " · " + Fmt.money(cu.donGia()) + " → " + Fmt.money(d.donGia()) + (d.dangApDung() ? "" : " · ngừng áp dụng"));
            return d.maDichVu();
        });
    }
}
