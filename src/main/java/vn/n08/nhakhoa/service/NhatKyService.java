package vn.n08.nhakhoa.service;

import vn.n08.nhakhoa.dao.NhatKyDao;
import vn.n08.nhakhoa.dto.NhatKyView;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.util.Tx;

import java.time.LocalDate;
import java.util.List;

/** Tra cứu nhật ký thao tác (chỉ đọc). */
public class NhatKyService {
    private final NhatKyDao dao = new NhatKyDao();

    public List<NhatKyView> tim(LocalDate tu, LocalDate den, String chucNang, String tuKhoa) {
        Session.require(Quyen.NHAT_KY);
        if (tu == null || den == null || den.isBefore(tu)) throw new BusinessException("den", "Khoảng ngày không hợp lệ.");
        return Tx.read(c -> dao.search(c, Now.startOf(tu), Now.endOf(den), chucNang, tuKhoa, 1000));
    }

    public List<String> chucNang() {
        Session.require(Quyen.NHAT_KY);
        return Tx.read(dao::chucNangList);
    }

    /** Lịch sử của một đối tượng (hóa đơn, phiếu, phân công…) — người có quyền xem đối tượng đó đều xem được. */
    public List<NhatKyView> lichSu(String ma) {
        Session.user();
        return Tx.read(c -> dao.byObject(c, ma));
    }
}
