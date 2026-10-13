package vn.n08.nhakhoa.service;

import vn.n08.nhakhoa.dao.MaDao;
import vn.n08.nhakhoa.dao.NhaCungCapDao;
import vn.n08.nhakhoa.model.NhaCungCap;
import vn.n08.nhakhoa.model.TrangThaiNCC;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.util.Tx;
import vn.n08.nhakhoa.util.Validator;

import java.util.List;

/** Danh mục nhà cung cấp (hỗ trợ UC30). Không xóa; ngừng hợp tác thì không chọn được khi nhập. */
public class NhaCungCapService {
    private final NhaCungCapDao dao = new NhaCungCapDao();
    private final MaDao maDao = new MaDao();

    public List<NhaCungCap> danhSach(String tuKhoa) {
        Session.require(Quyen.KHO);
        return Tx.read(c -> dao.search(c, tuKhoa));
    }

    public String luu(NhaCungCap n) {
        Session.require(Quyen.KHO);
        String ten = Validator.maxLen(Validator.required(n.tenNhaCungCap(), "ten", "Tên nhà cung cấp"), 150, "ten", "Tên");
        String sdt = Validator.anyPhone(n.soDienThoai(), "soDienThoai");
        String email = Validator.email(n.email(), "email");
        String dc = Validator.maxLen(Validator.optional(n.diaChi()), 255, "diaChi", "Địa chỉ");
        return Tx.write(c -> {
            if (n.maNhaCungCap() == null) {
                String ma = maDao.next(c, "NCC");
                dao.insert(c, new NhaCungCap(ma, ten, dc, sdt, email, TrangThaiNCC.DANG_HOP_TAC));
                Audit.log(c, "Kho", "Thêm nhà cung cấp", ma, ten);
                return ma;
            }
            if (dao.update(c, new NhaCungCap(n.maNhaCungCap(), ten, dc, sdt, email, n.trangThai())) == 0) throw new BusinessException("Không tìm thấy nhà cung cấp.");
            Audit.log(c, "Kho", "Sửa nhà cung cấp", n.maNhaCungCap(), ten);
            return n.maNhaCungCap();
        });
    }

    public void doiTrangThai(String ma, TrangThaiNCC tt) {
        Session.require(Quyen.KHO);
        Tx.run(c -> {
            if (dao.setTrangThai(c, ma, tt) == 0) throw new BusinessException("Không tìm thấy nhà cung cấp.");
            Audit.log(c, "Kho", tt == TrangThaiNCC.DANG_HOP_TAC ? "Hợp tác lại với nhà cung cấp" : "Ngừng hợp tác nhà cung cấp", ma, null);
        });
    }
}
