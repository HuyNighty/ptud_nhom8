package vn.n08.nhakhoa.service;

import vn.n08.nhakhoa.dao.BenhNhanDao;
import vn.n08.nhakhoa.dao.DichVuDao;
import vn.n08.nhakhoa.dao.KeHoachDao;
import vn.n08.nhakhoa.dao.LichHenDao;
import vn.n08.nhakhoa.dao.MaDao;
import vn.n08.nhakhoa.dao.PhieuKhamDao;
import vn.n08.nhakhoa.dao.ThamSoDao;
import vn.n08.nhakhoa.dto.HangMucForm;
import vn.n08.nhakhoa.dto.KeHoachChiTiet;
import vn.n08.nhakhoa.dto.KeHoachView;
import vn.n08.nhakhoa.dto.TaiKhamItem;
import vn.n08.nhakhoa.model.ChiTietDieuTri;
import vn.n08.nhakhoa.model.DichVu;
import vn.n08.nhakhoa.model.HoSoBenhAn;
import vn.n08.nhakhoa.model.KeHoachDieuTri;
import vn.n08.nhakhoa.model.LichHen;
import vn.n08.nhakhoa.model.PhieuKham;
import vn.n08.nhakhoa.model.TrangThaiDieuTri;
import vn.n08.nhakhoa.model.TrangThaiKeHoach;
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
import java.util.ArrayList;
import java.util.List;

/** UC12 Lập kế hoạch, UC13 Cập nhật quá trình điều trị, UC15 Ghi nhận kết quả điều trị, UC14 Quản lý tái khám. */
public class KeHoachService {
    private final KeHoachDao dao = new KeHoachDao();
    private final BenhNhanDao bnDao = new BenhNhanDao();
    private final DichVuDao dvDao = new DichVuDao();
    private final PhieuKhamDao pkDao = new PhieuKhamDao();
    private final LichHenDao lhDao = new LichHenDao();
    private final ThamSoDao tsDao = new ThamSoDao();
    private final MaDao maDao = new MaDao();

    public List<KeHoachView> danhSach(TrangThaiKeHoach tt, String tuKhoa) {
        Session.requireAny(Quyen.KE_HOACH, Quyen.TAI_KHAM, Quyen.BENH_NHAN_XEM);
        return Tx.read(c -> dao.search(c, tt, tuKhoa));
    }

    public KeHoachChiTiet chiTiet(String ma) {
        Session.requireAny(Quyen.KE_HOACH, Quyen.TAI_KHAM, Quyen.BENH_NHAN_XEM);
        return Tx.read(c -> new KeHoachChiTiet(dao.findView(c, ma).orElseThrow(() -> new BusinessException("Không tìm thấy kế hoạch " + ma + ".")),
                dao.items(c, ma)));
    }

    public List<DichVu> dichVuDieuTri() {
        Session.user();
        return Tx.read(c -> dvDao.all(c, true).stream().filter(d -> !d.laCanLamSang()).toList());
    }

    public String lap(String maBn, String maPk, String mucTieu, List<HangMucForm> hangMuc, String maBsChon) {
        Session.require(Quyen.KE_HOACH);
        String mt = Validator.maxLen(Validator.required(mucTieu, "mucTieu", "Mục tiêu điều trị"), 300, "mucTieu", "Mục tiêu");
        if (hangMuc == null || hangMuc.isEmpty()) throw new BusinessException("hangMuc", "Kế hoạch cần ít nhất một hạng mục.");
        return Tx.write(c -> {
            String bs = BacSiPhuTrach.resolve(c, maBsChon);
            HoSoBenhAn hs = bnDao.hoSoOf(c, maBn).orElseThrow(() -> new BusinessException("benhNhan", "Chọn bệnh nhân có hồ sơ bệnh án."));
            if (maPk != null) {
                PhieuKham pk = pkDao.find(c, maPk).orElseThrow(() -> new BusinessException("Không tìm thấy phiếu khám."));
                if (!pk.maBenhNhan().equals(maBn)) throw new BusinessException("Phiếu khám không thuộc bệnh nhân này.");
            }
            String ma = maDao.next(c, "KH");
            dao.insert(c, new KeHoachDieuTri(ma, hs.maHoSo(), maPk, Now.today(), mt, TrangThaiKeHoach.DANG_THUC_HIEN, bs, null));
            for (HangMucForm h : hangMuc) themItem(c, ma, h);
            Audit.log(c, "Điều trị", "Lập kế hoạch điều trị", ma, maBn + " · " + mt + " · " + hangMuc.size() + " hạng mục", bs);
            return ma;
        });
    }

    private void themItem(Connection c, String maKh, HangMucForm h) throws SQLException {
        DichVu dv = dvDao.find(c, h.maDichVu()).orElseThrow(() -> new BusinessException("hangMuc", "Chọn dịch vụ cho hạng mục."));
        if (!dv.dangApDung()) throw new BusinessException("hangMuc", dv.tenDichVu() + " không còn áp dụng.");
        String vt = Validator.maxLen(Validator.required(h.viTriRang(), "hangMuc", "Vị trí răng"), 50, "hangMuc", "Vị trí răng");
        if (h.ngayDuKien() == null || h.ngayDuKien().isBefore(Now.today())) throw new BusinessException("hangMuc", "Ngày dự kiến phải từ hôm nay trở đi.");
        dao.insertItem(c, new ChiTietDieuTri(null, maKh, dv.maDichVu(), vt, h.ngayDuKien(), null, null, TrangThaiDieuTri.CHUA_THUC_HIEN, null, null));
    }

    public void themHangMuc(String maKh, HangMucForm h) {
        Session.require(Quyen.KE_HOACH);
        Tx.run(c -> {
            KeHoachDieuTri kh = dangThucHien(c, maKh);
            themItem(c, maKh, h);
            Audit.log(c, "Điều trị", "Thêm hạng mục", maKh, h.maDichVu() + " · răng " + h.viTriRang() + " · " + Fmt.date(h.ngayDuKien()), kh.maNhanVien());
        });
    }

    public void doiNgayDuKien(int maChiTiet, LocalDate ngay) {
        Session.require(Quyen.KE_HOACH);
        if (ngay == null || ngay.isBefore(Now.today())) throw new BusinessException("ngay", "Ngày dự kiến phải từ hôm nay trở đi.");
        Tx.run(c -> {
            ChiTietDieuTri ct = item(c, maChiTiet);
            KeHoachDieuTri kh = dangThucHien(c, ct.maKeHoach());
            if (dao.setNgayDuKien(c, maChiTiet, ngay) == 0) throw new BusinessException("Chỉ đổi ngày cho hạng mục chưa thực hiện.");
            Audit.log(c, "Điều trị", "Đổi ngày dự kiến", ct.maKeHoach(), "Hạng mục #" + maChiTiet + ": " + Fmt.date(ct.ngayDuKien()) + " → " + Fmt.date(ngay), kh.maNhanVien());
        });
    }

    public void huyHangMuc(int maChiTiet, String lyDo) {
        Session.require(Quyen.KE_HOACH);
        String ly = Validator.required(lyDo, "lyDo", "Lý do hủy hạng mục");
        Tx.run(c -> {
            ChiTietDieuTri ct = item(c, maChiTiet);
            KeHoachDieuTri kh = dangThucHien(c, ct.maKeHoach());
            if (dao.cancelItem(c, maChiTiet, ly) == 0) throw new BusinessException("Chỉ hủy được hạng mục chưa thực hiện.");
            Audit.log(c, "Điều trị", "Hủy hạng mục", ct.maKeHoach(), "Hạng mục #" + maChiTiet + ": " + ly, kh.maNhanVien());
        });
    }

    /** UC15: ghi kết quả một hạng mục; bác sĩ thực hiện = bác sĩ đăng nhập hoặc bác sĩ Admin chọn. */
    public void ghiKetQua(int maChiTiet, LocalDate ngayThucHien, String ketQua, String maBsChon) {
        Session.require(Quyen.KE_HOACH);
        String kq = Validator.maxLen(Validator.required(ketQua, "ketQua", "Kết quả điều trị"), 500, "ketQua", "Kết quả");
        if (ngayThucHien == null || ngayThucHien.isAfter(Now.today())) throw new BusinessException("ngay", "Ngày thực hiện không được sau hôm nay.");
        Tx.run(c -> {
            String bs = BacSiPhuTrach.resolve(c, maBsChon);
            ChiTietDieuTri ct = item(c, maChiTiet);
            KeHoachDieuTri kh = dao.find(c, ct.maKeHoach()).orElseThrow();
            if (kh.trangThai() != TrangThaiKeHoach.DANG_THUC_HIEN) throw new BusinessException("Kế hoạch không còn ở trạng thái đang thực hiện.");
            if (ngayThucHien.isBefore(kh.ngayLap())) throw new BusinessException("ngay", "Ngày thực hiện trước ngày lập kế hoạch.");
            if (dao.recordResult(c, maChiTiet, ngayThucHien, kq, bs) == 0) throw new BusinessException("Hạng mục đã thực hiện hoặc đã hủy.");
            Audit.log(c, "Điều trị", "Ghi nhận kết quả điều trị", ct.maKeHoach(), "Hạng mục #" + maChiTiet + ": " + kq, bs);
        });
    }

    /** Hoàn thành kế hoạch: không còn hạng mục chưa thực hiện và có ít nhất một hạng mục đã thực hiện. */
    public void hoanThanh(String maKh) {
        Session.require(Quyen.KE_HOACH);
        Tx.run(c -> {
            KeHoachDieuTri kh = dangThucHien(c, maKh);
            long con = dao.countItems(c, maKh, TrangThaiDieuTri.CHUA_THUC_HIEN);
            if (con > 0) throw new BusinessException("Còn " + con + " hạng mục chưa thực hiện. Ghi kết quả hoặc hủy hạng mục trước.");
            if (dao.countItems(c, maKh, TrangThaiDieuTri.DA_THUC_HIEN) == 0) throw new BusinessException("Chưa có hạng mục nào được thực hiện; hãy hủy kế hoạch nếu không điều trị.");
            if (dao.setTrangThai(c, maKh, TrangThaiKeHoach.HOAN_THANH, null) == 0) throw TaiKhoanService.stale();
            Audit.log(c, "Điều trị", "Hoàn thành kế hoạch", maKh, null, kh.maNhanVien());
        });
    }

    /** Hủy kế hoạch: các hạng mục chưa thực hiện chuyển DA_HUY; hạng mục đã thực hiện giữ nguyên. */
    public void huyKeHoach(String maKh, String lyDo) {
        Session.require(Quyen.KE_HOACH);
        String ly = Validator.required(lyDo, "lyDo", "Lý do hủy kế hoạch");
        Tx.run(c -> {
            KeHoachDieuTri kh = dangThucHien(c, maKh);
            int n = dao.cancelPendingItems(c, maKh, "Hủy theo kế hoạch: " + ly);
            if (dao.setTrangThai(c, maKh, TrangThaiKeHoach.DA_HUY, ly) == 0) throw TaiKhoanService.stale();
            Audit.log(c, "Điều trị", "Hủy kế hoạch", maKh, ly + " (" + n + " hạng mục chưa thực hiện được hủy)", kh.maNhanVien());
        });
    }

    /** UC14: hạng mục đến hạn trong TAI_KHAM_NGUONG_NGAY ngày tới hoặc đã quá hạn, kèm lịch hẹn đang mở của kế hoạch. */
    public List<TaiKhamItem> danhSachTaiKham() {
        Session.require(Quyen.TAI_KHAM);
        return Tx.read(c -> {
            int n = tsDao.getInt(c, "TAI_KHAM_NGUONG_NGAY", 14);
            List<TaiKhamItem> out = new ArrayList<>();
            for (TaiKhamItem t : dao.dueItems(c, Now.today().plusDays(n))) {
                LichHen lh = lhDao.openOfPlan(c, t.maKeHoach()).orElse(null);
                out.add(new TaiKhamItem(t.hangMuc(), t.maKeHoach(), t.mucTieu(), t.maBenhNhan(), t.tenBenhNhan(), t.soDienThoai(), t.maBacSi(),
                        t.tenBacSi(), lh == null ? null : lh.maLichHen(), lh == null ? null : lh.thoiGianHen()));
            }
            return out;
        });
    }

    public int nguongTaiKham() {
        Session.user();
        return Tx.read(c -> tsDao.getInt(c, "TAI_KHAM_NGUONG_NGAY", 14));
    }

    private ChiTietDieuTri item(Connection c, int ma) throws SQLException {
        return dao.findItem(c, ma).orElseThrow(() -> new BusinessException("Không tìm thấy hạng mục #" + ma + "."));
    }

    private KeHoachDieuTri dangThucHien(Connection c, String ma) throws SQLException {
        KeHoachDieuTri kh = dao.find(c, ma).orElseThrow(() -> new BusinessException("Không tìm thấy kế hoạch " + ma + "."));
        if (kh.trangThai() != TrangThaiKeHoach.DANG_THUC_HIEN) throw new BusinessException("Kế hoạch " + ma + " không còn ở trạng thái đang thực hiện.");
        BacSiPhuTrach.requireOwner(kh.maNhanVien());
        return kh;
    }
}
