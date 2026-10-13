package vn.n08.nhakhoa.service;

import vn.n08.nhakhoa.dao.BenhNhanDao;
import vn.n08.nhakhoa.dao.HoaDonDao;
import vn.n08.nhakhoa.dao.KeHoachDao;
import vn.n08.nhakhoa.dao.LichHenDao;
import vn.n08.nhakhoa.dao.MaDao;
import vn.n08.nhakhoa.dao.PhieuKhamDao;
import vn.n08.nhakhoa.dto.BenhNhanChiTiet;
import vn.n08.nhakhoa.dto.BenhNhanForm;
import vn.n08.nhakhoa.dto.BenhNhanView;
import vn.n08.nhakhoa.model.BenhNhan;
import vn.n08.nhakhoa.model.HoSoBenhAn;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.CanhBaoException;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.util.Tx;
import vn.n08.nhakhoa.util.Validator;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * UC01 Tìm kiếm, UC08 Tạo hồ sơ (lễ tân tạo chính thức trực tiếp), UC09 Xem, UC23 Quản lý hồ sơ bệnh nhân.
 * Không có chức năng xóa hồ sơ bệnh nhân. Lễ tân sửa thông tin hành chính; bác sĩ cập nhật tiền sử / dị ứng.
 */
public class BenhNhanService {
    private final BenhNhanDao dao = new BenhNhanDao();
    private final PhieuKhamDao pkDao = new PhieuKhamDao();
    private final KeHoachDao khDao = new KeHoachDao();
    private final LichHenDao lhDao = new LichHenDao();
    private final HoaDonDao hdDao = new HoaDonDao();
    private final MaDao maDao = new MaDao();

    public List<BenhNhanView> tim(String q) {
        Session.requireAny(Quyen.BENH_NHAN_XEM, Quyen.TIEP_NHAN, Quyen.LICH_HEN, Quyen.THU_NGAN, Quyen.KHAM);
        String s = q == null ? "" : q.trim();
        String digits = Validator.normalizePhone(s);
        if (!s.isEmpty() && digits.matches("\\d+") && (digits.length() > 10 || (digits.length() == 10 && !digits.startsWith("0")))) {
            throw new BusinessException("q", "Số điện thoại không hợp lệ: gồm 10 chữ số và bắt đầu bằng 0.");
        }
        return Tx.read(c -> dao.search(c, s, 200));
    }

    public BenhNhanChiTiet chiTiet(String ma) {
        Session.requireAny(Quyen.BENH_NHAN_XEM, Quyen.TIEP_NHAN, Quyen.THU_NGAN, Quyen.KHAM);
        return Tx.read(c -> {
            BenhNhan bn = dao.find(c, ma).orElseThrow(() -> new BusinessException("Không tìm thấy bệnh nhân " + ma + "."));
            return new BenhNhanChiTiet(bn, dao.hoSoOf(c, ma).orElse(null), pkDao.byPatient(c, ma), khDao.byPatient(c, ma), lhDao.byPatient(c, ma),
                    hdDao.byPatient(c, ma));
        });
    }

    /** UC08: tạo hồ sơ bệnh nhân + hồ sơ bệnh án. Nếu có hồ sơ nghi trùng mà chưa xác nhận → CanhBaoException. */
    public String tao(BenhNhanForm f, boolean daXacNhanTrung) {
        Session.require(Quyen.BENH_NHAN_TAO);
        BenhNhan b = validate(null, f);
        return Tx.write(c -> {
            canhBaoTrung(c, b, daXacNhanTrung);
            String ma = maDao.next(c, "BN");
            String maHs = maDao.next(c, "HS");
            dao.insert(c, new BenhNhan(ma, b.hoTen(), b.ngaySinh(), b.gioiTinh(), b.soDienThoai(), b.diaChi(), b.nguoiLienHe(), Now.now(),
                    Session.user().maTaiKhoan()));
            dao.insertHoSo(c, new HoSoBenhAn(maHs, ma, Now.today(), null, null));
            Audit.log(c, "Bệnh nhân", "Tạo hồ sơ bệnh nhân" + (daXacNhanTrung ? " (đã xác nhận không trùng)" : ""), ma, b.hoTen() + " · " + b.soDienThoai());
            return ma;
        });
    }

    /** Sửa thông tin hành chính (lễ tân / quản lý). */
    public void capNhat(String ma, BenhNhanForm f, boolean daXacNhanTrung) {
        Session.require(Quyen.BENH_NHAN_SUA);
        BenhNhan b = validate(ma, f);
        Tx.run(c -> {
            dao.find(c, ma).orElseThrow(() -> new BusinessException("Không tìm thấy bệnh nhân."));
            canhBaoTrung(c, b, daXacNhanTrung);
            dao.update(c, b);
            Audit.log(c, "Bệnh nhân", "Sửa thông tin hành chính", ma, b.hoTen() + " · " + b.soDienThoai());
        });
    }

    /** Bác sĩ cập nhật tiền sử bệnh và dị ứng (HoSoBenhAn.capNhatTienSu). */
    public void capNhatTienSu(String maBn, String tienSu, String diUng, String maBacSi) {
        Session.require(Quyen.TIEN_SU);
        String ts = Validator.maxLen(Validator.optional(tienSu), 500, "tienSu", "Tiền sử bệnh");
        String du = Validator.maxLen(Validator.optional(diUng), 300, "diUng", "Dị ứng");
        Tx.run(c -> {
            String bs = BacSiPhuTrach.resolve(c, maBacSi);
            HoSoBenhAn hs = dao.hoSoOf(c, maBn).orElseThrow(() -> new BusinessException("Bệnh nhân chưa có hồ sơ bệnh án."));
            dao.updateTienSu(c, hs.maHoSo(), ts, du);
            Audit.log(c, "Bệnh nhân", "Cập nhật tiền sử / dị ứng", maBn, "Dị ứng: " + Fmt.nz(du), bs);
        });
    }

    private void canhBaoTrung(Connection c, BenhNhan b, boolean daXacNhan) throws SQLException {
        if (daXacNhan) return;
        List<BenhNhan> trung = dao.duplicates(c, b.hoTen(), b.ngaySinh(), b.soDienThoai(), b.maBenhNhan());
        if (!trung.isEmpty()) {
            throw new CanhBaoException("Có " + trung.size() + " hồ sơ nghi trùng. Kiểm tra trước khi lưu:", trung.stream()
                    .map(x -> x.maBenhNhan() + " · " + x.hoTen() + " · " + Fmt.date(x.ngaySinh()) + " · " + x.soDienThoai()
                            + (x.soDienThoai().equals(b.soDienThoai()) ? " (trùng số điện thoại)" : " (trùng họ tên + ngày sinh)"))
                    .toList());
        }
    }

    private BenhNhan validate(String ma, BenhNhanForm f) {
        String hoTen = Validator.maxLen(Validator.required(f.hoTen(), "hoTen", "Họ tên"), 100, "hoTen", "Họ tên");
        if (f.ngaySinh() == null) throw new BusinessException("ngaySinh", "Nhập ngày sinh (dd/mm/yyyy).");
        if (f.ngaySinh().isAfter(Now.today())) throw new BusinessException("ngaySinh", "Ngày sinh không được sau hôm nay.");
        if (f.ngaySinh().isBefore(Now.today().minusYears(120))) throw new BusinessException("ngaySinh", "Ngày sinh không hợp lệ.");
        if (f.gioiTinh() == null) throw new BusinessException("gioiTinh", "Chọn giới tính.");
        String sdt = Validator.phone(f.soDienThoai(), "soDienThoai");
        String dc = Validator.maxLen(Validator.optional(f.diaChi()), 255, "diaChi", "Địa chỉ");
        String lh = Validator.maxLen(Validator.optional(f.nguoiLienHe()), 200, "nguoiLienHe", "Người liên hệ");
        return new BenhNhan(ma, hoTen, f.ngaySinh(), f.gioiTinh(), sdt, dc, lh, null, null);
    }
}
