package vn.n08.nhakhoa.service;

import vn.n08.nhakhoa.dao.BenhNhanDao;
import vn.n08.nhakhoa.dao.KeHoachDao;
import vn.n08.nhakhoa.dao.LichHenDao;
import vn.n08.nhakhoa.dao.MaDao;
import vn.n08.nhakhoa.dao.NhatKyDao;
import vn.n08.nhakhoa.dao.ThamSoDao;
import vn.n08.nhakhoa.dto.LichHenForm;
import vn.n08.nhakhoa.dto.LichHenView;
import vn.n08.nhakhoa.dto.NhatKyView;
import vn.n08.nhakhoa.model.BenhNhan;
import vn.n08.nhakhoa.model.KeHoachDieuTri;
import vn.n08.nhakhoa.model.LichHen;
import vn.n08.nhakhoa.model.NhanVien;
import vn.n08.nhakhoa.model.TrangThaiKeHoach;
import vn.n08.nhakhoa.model.TrangThaiLichHen;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * UC03 Quản lý lịch hẹn, UC06 Hủy lịch hẹn, UC07 Thay đổi lịch hẹn (State Machine tuần 7).
 * Đổi lịch không sửa bản ghi cũ: bản cũ → DA_DOI_LICH, tạo bản mới trỏ maLichHenGoc (Q&A #4).
 */
public class LichHenService {
    private final LichHenDao dao = new LichHenDao();
    private final BenhNhanDao bnDao = new BenhNhanDao();
    private final KeHoachDao khDao = new KeHoachDao();
    private final ThamSoDao tsDao = new ThamSoDao();
    private final NhatKyDao nkDao = new NhatKyDao();
    private final MaDao maDao = new MaDao();

    public List<LichHenView> danhSach(LocalDate tu, LocalDate den, String maBs, TrangThaiLichHen tt, String tuKhoa) {
        Session.requireAny(Quyen.LICH_HEN, Quyen.TAI_KHAM, Quyen.KHAM, Quyen.NHAN_VIEN);
        return Tx.read(c -> dao.search(c, Now.startOf(tu), Now.endOf(den), maBs, tt, tuKhoa));
    }

    public List<LocalTime> khungGio() {
        Session.user();
        return Tx.read(this::slots);
    }

    private List<LocalTime> slots(Connection c) throws SQLException {
        List<LocalTime> out = new ArrayList<>();
        for (String s : tsDao.get(c, "KHUNG_GIO_HEN", "08:00").split(",")) {
            if (!s.isBlank()) out.add(LocalTime.parse(s.trim()));
        }
        return out;
    }

    /** Khung giờ còn trống của bác sĩ trong ngày (bỏ giờ đã qua nếu là hôm nay). */
    public List<LocalTime> khungGioTrong(String maBs, LocalDate ngay, String boQuaMaLichHen) {
        Session.user();
        if (maBs == null || ngay == null) return List.of();
        return Tx.read(c -> {
            Set<LocalTime> ban = new HashSet<>();
            for (LichHen l : dao.openOfDoctor(c, maBs, Now.startOf(ngay), Now.endOf(ngay))) {
                if (!l.maLichHen().equals(boQuaMaLichHen)) ban.add(l.thoiGianHen().toLocalTime());
            }
            LocalDateTime now = Now.now();
            return slots(c).stream().filter(t -> !ban.contains(t) && ngay.atTime(t).isAfter(now)).toList();
        });
    }

    public String tao(LichHenForm f) {
        Session.requireAny(Quyen.LICH_HEN, Quyen.TAI_KHAM);
        String lyDo = Validator.maxLen(Validator.optional(f.lyDoHen()), 300, "lyDoHen", "Lý do hẹn");
        return Tx.write(c -> {
            BenhNhan bn = bnDao.find(c, f.maBenhNhan()).orElseThrow(() -> new BusinessException("benhNhan", "Chọn bệnh nhân."));
            kiemTraKhungGio(c, f.maBenhNhan(), f.maBacSi(), f.thoiGianHen(), null);
            if (f.maKeHoach() != null) {
                KeHoachDieuTri kh = khDao.find(c, f.maKeHoach()).orElseThrow(() -> new BusinessException("Không tìm thấy kế hoạch."));
                if (kh.trangThai() != TrangThaiKeHoach.DANG_THUC_HIEN) throw new BusinessException("Kế hoạch " + kh.maKeHoach() + " không còn thực hiện.");
                String maBnKh = bnDao.hoSoOf(c, bn.maBenhNhan()).map(h -> h.maHoSo()).orElse("");
                if (!kh.maHoSo().equals(maBnKh)) throw new BusinessException("Kế hoạch không thuộc bệnh nhân này.");
            }
            String ma = maDao.next(c, "LH");
            dao.insert(c, new LichHen(ma, bn.maBenhNhan(), f.maBacSi(), f.thoiGianHen(), lyDo, TrangThaiLichHen.CHO_XAC_NHAN, null, f.maKeHoach(),
                    null, Session.user().maTaiKhoan(), Now.now()));
            Audit.log(c, "Lịch hẹn", f.maKeHoach() == null ? "Tạo lịch hẹn" : "Tạo lịch tái khám", ma,
                    bn.hoTen() + " · " + Fmt.dateTime(f.thoiGianHen()) + (f.maKeHoach() == null ? "" : " · kế hoạch " + f.maKeHoach()), f.maBacSi());
            return ma;
        });
    }

    private void kiemTraKhungGio(Connection c, String maBn, String maBs, LocalDateTime t, String boQua) throws SQLException {
        if (maBs == null) throw new BusinessException("bacSi", "Chọn bác sĩ.");
        NhanVien bs = BacSiPhuTrach.requireActiveDoctor(c, maBs);
        if (t == null) throw new BusinessException("thoiGian", "Chọn ngày và khung giờ hẹn.");
        if (!t.isAfter(Now.now())) throw new BusinessException("thoiGian", "Thời gian hẹn phải sau thời điểm hiện tại.");
        if (t.toLocalDate().isAfter(Now.today().plusDays(180))) throw new BusinessException("thoiGian", "Chỉ đặt hẹn trong vòng 180 ngày.");
        if (!slots(c).contains(t.toLocalTime())) throw new BusinessException("thoiGian", "Giờ hẹn phải là một khung giờ của phòng khám.");
        if (dao.doctorBusy(c, maBs, t, boQua)) {
            throw new BusinessException("thoiGian", "Khung giờ đã được đặt: " + bs.hoTen() + " đã có lịch lúc " + Fmt.dateTime(t) + ".");
        }
        if (dao.patientBusy(c, maBn, t, boQua)) throw new BusinessException("thoiGian", "Bệnh nhân đã có lịch hẹn khác lúc " + Fmt.dateTime(t) + ".");
    }

    public void xacNhan(String ma) {
        Session.require(Quyen.LICH_HEN);
        Tx.run(c -> {
            if (dao.transition(c, ma, TrangThaiLichHen.DA_XAC_NHAN, null, TrangThaiLichHen.CHO_XAC_NHAN) == 0) {
                throw new BusinessException("Chỉ xác nhận được lịch hẹn đang chờ xác nhận.");
            }
            Audit.log(c, "Lịch hẹn", "Xác nhận lịch hẹn", ma, null);
        });
    }

    /** UC06: hủy lịch, bắt buộc lý do. */
    public void huy(String ma, String lyDo) {
        Session.requireAny(Quyen.LICH_HEN, Quyen.NHAN_VIEN);
        String ly = Validator.maxLen(Validator.required(lyDo, "lyDo", "Lý do hủy"), 300, "lyDo", "Lý do hủy");
        Tx.run(c -> {
            if (dao.transition(c, ma, TrangThaiLichHen.DA_HUY, ly, TrangThaiLichHen.CHO_XAC_NHAN, TrangThaiLichHen.DA_XAC_NHAN) == 0) {
                throw new BusinessException("Chỉ hủy được lịch hẹn chưa diễn ra (chờ xác nhận / đã xác nhận).");
            }
            Audit.log(c, "Lịch hẹn", "Hủy lịch hẹn", ma, ly);
        });
    }

    /** UC07: đổi giờ và/hoặc bác sĩ. Lịch cũ giữ nguyên với trạng thái DA_DOI_LICH. */
    public String doiLich(String ma, String maBsMoi, LocalDateTime thoiGianMoi, String lyDo) {
        Session.requireAny(Quyen.LICH_HEN, Quyen.NHAN_VIEN);
        String ly = Validator.required(lyDo, "lyDo", "Lý do đổi lịch");
        return Tx.write(c -> {
            LichHen cu = dao.find(c, ma).orElseThrow(() -> new BusinessException("Không tìm thấy lịch hẹn " + ma + "."));
            if (!cu.dangMo()) throw new BusinessException("Chỉ đổi được lịch hẹn chưa diễn ra (chờ xác nhận / đã xác nhận).");
            if (cu.maNhanVien().equals(maBsMoi) && cu.thoiGianHen().equals(thoiGianMoi)) {
                throw new BusinessException("thoiGian", "Chọn bác sĩ hoặc khung giờ khác với lịch hiện tại.");
            }
            kiemTraKhungGio(c, cu.maBenhNhan(), maBsMoi, thoiGianMoi, ma);
            if (dao.transition(c, ma, TrangThaiLichHen.DA_DOI_LICH, null, TrangThaiLichHen.CHO_XAC_NHAN, TrangThaiLichHen.DA_XAC_NHAN) == 0) {
                throw TaiKhoanService.stale();
            }
            String moi = maDao.next(c, "LH");
            dao.insert(c, new LichHen(moi, cu.maBenhNhan(), maBsMoi, thoiGianMoi, cu.lyDoHen(), TrangThaiLichHen.CHO_XAC_NHAN, ma, cu.maKeHoach(),
                    null, Session.user().maTaiKhoan(), Now.now()));
            Audit.log(c, "Lịch hẹn", "Đổi lịch hẹn", ma, Fmt.dateTime(cu.thoiGianHen()) + " → " + Fmt.dateTime(thoiGianMoi)
                    + (cu.maNhanVien().equals(maBsMoi) ? "" : ", đổi bác sĩ " + cu.maNhanVien() + " → " + maBsMoi) + " (lịch mới " + moi + "): " + ly, maBsMoi);
            Audit.log(c, "Lịch hẹn", "Tạo lịch hẹn (từ đổi lịch)", moi, "Đổi từ " + ma, maBsMoi);
            return moi;
        });
    }

    /** Đánh dấu vắng mặt: chỉ sau giờ hẹn. */
    public void danhDauVang(String ma) {
        Session.require(Quyen.LICH_HEN);
        Tx.run(c -> {
            LichHen l = dao.find(c, ma).orElseThrow(() -> new BusinessException("Không tìm thấy lịch hẹn."));
            if (l.thoiGianHen().isAfter(Now.now())) throw new BusinessException("Chưa đến giờ hẹn, chưa thể đánh dấu vắng mặt.");
            if (dao.transition(c, ma, TrangThaiLichHen.VANG_MAT, null, TrangThaiLichHen.CHO_XAC_NHAN, TrangThaiLichHen.DA_XAC_NHAN) == 0) {
                throw new BusinessException("Chỉ đánh dấu vắng cho lịch hẹn chưa check-in.");
            }
            Audit.log(c, "Lịch hẹn", "Đánh dấu vắng mặt", ma, null);
        });
    }

    /** Chuỗi lịch hẹn liên quan (bản gốc ← → các bản đổi) và nhật ký. */
    public List<LichHenView> chuoiDoiLich(String ma) {
        Session.user();
        return Tx.read(c -> {
            LichHen l = dao.find(c, ma).orElseThrow();
            String goc = l.maLichHen();
            while (true) {
                LichHen x = dao.find(c, goc).orElseThrow();
                if (x.maLichHenGoc() == null) break;
                goc = x.maLichHenGoc();
            }
            List<LichHenView> out = new ArrayList<>();
            List<String> queue = new ArrayList<>(List.of(goc));
            while (!queue.isEmpty()) {
                String m = queue.remove(0);
                dao.findView(c, m).ifPresent(out::add);
                for (LichHenView v : dao.chainFrom(c, m)) queue.add(v.lh().maLichHen());
            }
            return out;
        });
    }

    public List<NhatKyView> nhatKy(String ma) {
        Session.user();
        return Tx.read(c -> nkDao.byObject(c, ma));
    }
}
