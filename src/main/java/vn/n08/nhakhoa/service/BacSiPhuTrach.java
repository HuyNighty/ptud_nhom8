package vn.n08.nhakhoa.service;

import vn.n08.nhakhoa.dao.NhanVienDao;
import vn.n08.nhakhoa.model.NhanVien;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.Session;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Xác định bác sĩ chịu trách nhiệm chuyên môn cho thao tác lâm sàng.
 * - Tài khoản Bác sĩ: luôn là chính mình, không được ghi dưới tên bác sĩ khác.
 * - Tài khoản khác có quyền (Admin): bắt buộc chọn một bác sĩ đang làm việc; nhật ký vẫn ghi tài khoản thật.
 */
final class BacSiPhuTrach {
    private static final NhanVienDao NV = new NhanVienDao();

    private BacSiPhuTrach() {}

    static String resolve(Connection c, String maBsChon) throws SQLException {
        Session.User u = Session.user();
        if (u.vaiTro() == VaiTro.BS) {
            if (maBsChon != null && !maBsChon.equals(u.maNhanVien())) {
                throw new BusinessException("bacSi", "Tài khoản bác sĩ chỉ ghi nhận dưới tên chính mình.");
            }
            return u.maNhanVien();
        }
        if (maBsChon == null || maBsChon.isBlank()) throw new BusinessException("bacSi", "Chọn bác sĩ chịu trách nhiệm chuyên môn.");
        requireActiveDoctor(c, maBsChon);
        return maBsChon;
    }

    /** Với thao tác trên phiếu/kế hoạch đã gán bác sĩ: tài khoản bác sĩ chỉ thao tác trên hồ sơ của mình. */
    static void requireOwner(String maBsCuaHoSo) {
        Session.User u = Session.user();
        if (u.vaiTro() == VaiTro.BS && !u.maNhanVien().equals(maBsCuaHoSo)) {
            throw new BusinessException("Hồ sơ này do bác sĩ khác phụ trách; tài khoản bác sĩ chỉ thao tác trên hồ sơ của mình.");
        }
    }

    static NhanVien requireActiveDoctor(Connection c, String ma) throws SQLException {
        NhanVien nv = NV.find(c, ma).orElseThrow(() -> new BusinessException("bacSi", "Không tìm thấy bác sĩ " + ma + "."));
        if (nv.chucDanh() != VaiTro.BS) throw new BusinessException("bacSi", nv.hoTen() + " không phải bác sĩ.");
        if (!nv.dangLamViec()) throw new BusinessException("bacSi", nv.hoTen() + " đã nghỉ việc.");
        return nv;
    }
}
