package vn.n08.nhakhoa.service;

import vn.n08.nhakhoa.dao.BanGiaoDao;
import vn.n08.nhakhoa.dao.CaLamViecDao;
import vn.n08.nhakhoa.dao.CapCuuDao;
import vn.n08.nhakhoa.dao.DieuChinhDao;
import vn.n08.nhakhoa.dao.HangHoaDao;
import vn.n08.nhakhoa.dao.HoaDonDao;
import vn.n08.nhakhoa.dao.KeHoachDao;
import vn.n08.nhakhoa.dao.LichHenDao;
import vn.n08.nhakhoa.dao.LoHangDao;
import vn.n08.nhakhoa.dao.PhanCongDao;
import vn.n08.nhakhoa.dao.PhieuKhamDao;
import vn.n08.nhakhoa.dao.TaiKhoanDao;
import vn.n08.nhakhoa.dao.ThamSoDao;
import vn.n08.nhakhoa.dao.ThanhToanDao;
import vn.n08.nhakhoa.dao.YeuCauHuyDao;
import vn.n08.nhakhoa.dto.DashboardData;
import vn.n08.nhakhoa.dto.HoaDonView;
import vn.n08.nhakhoa.dto.KiemTraCapCuuView;
import vn.n08.nhakhoa.dto.PhanCongView;
import vn.n08.nhakhoa.model.CaLamViec;
import vn.n08.nhakhoa.model.KetQuaKiemTra;
import vn.n08.nhakhoa.model.PhuongThucThanhToan;
import vn.n08.nhakhoa.model.TrangThaiLichHen;
import vn.n08.nhakhoa.model.TrangThaiPhanCong;
import vn.n08.nhakhoa.model.TrangThaiPhieuKham;
import vn.n08.nhakhoa.model.TrangThaiTaiKhoan;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.util.Tx;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tổng quan trong ngày theo quyền của tài khoản. */
public class DashboardService {
    public DashboardData tai() {
        Session.require(Quyen.TONG_QUAN);
        LocalDate h = Now.today();
        var tu = Now.startOf(h);
        var den = Now.endOf(h);
        return Tx.read(c -> {
            Map<String, Long> lh = null;
            Long cho = null, dang = null, xong = null, tkd = null;
            if (Session.canAny(Quyen.LICH_HEN, Quyen.TIEP_NHAN, Quyen.KHAM)) {
                lh = new LinkedHashMap<>();
                LichHenDao d = new LichHenDao();
                for (TrangThaiLichHen t : TrangThaiLichHen.values()) lh.put(t.label(), d.countOn(c, tu, den, t));
                PhieuKhamDao p = new PhieuKhamDao();
                cho = p.countToday(c, tu, den, TrangThaiPhieuKham.CHO_KHAM);
                dang = p.countToday(c, tu, den, TrangThaiPhieuKham.DANG_KHAM);
                xong = p.countToday(c, tu, den, TrangThaiPhieuKham.DA_KET_LUAN);
            }
            if (Session.can(Quyen.TAI_KHAM)) {
                tkd = (long) new KeHoachDao().dueItems(c, h.plusDays(new ThamSoDao().getInt(c, "TAI_KHAM_NGUONG_NGAY", 14))).size();
            }
            Map<String, BigDecimal> dt = null;
            BigDecimal no = null;
            Long soNo = null, yc = null;
            if (Session.canAny(Quyen.THU_NGAN, Quyen.DUYET_HUY)) {
                dt = new LinkedHashMap<>();
                ThanhToanDao t = new ThanhToanDao();
                for (PhuongThucThanhToan pt : PhuongThucThanhToan.values()) dt.put(pt.label(), t.sumBetween(c, tu, den, pt));
                List<HoaDonView> ds = new HoaDonDao().withDebt(c);
                no = ds.stream().map(HoaDonView::conNo).reduce(BigDecimal.ZERO, BigDecimal::add);
                soNo = (long) ds.size();
                yc = new YeuCauHuyDao().countPending(c);
            }
            Long dc = Session.canAny(Quyen.KIEM_KE, Quyen.DUYET_KHO) ? new DieuChinhDao().countPending(c) : null;
            Long bg = Session.can(Quyen.BAN_GIAO) ? new BanGiaoDao().countPending(c) : null;
            Long duoi = null, sap = null, het = null;
            if (Session.canAny(Quyen.KHO, Quyen.DUYET_KHO)) {
                duoi = new HangHoaDao().tonKho(c, h, null).stream().filter(i -> i.duoiDinhMuc()).count();
                int n = new ThamSoDao().getInt(c, "HSD_CANH_BAO_NGAY", 30);
                LoHangDao lo = new LoHangDao();
                sap = lo.countExpiring(c, h, h.plusDays(n));
                het = lo.countExpiredInStock(c, h);
            }
            KetQuaKiemTra kq = null;
            LocalDateTime lan = null;
            Boolean quaHan = null;
            if (Session.canAny(Quyen.CAP_CUU, Quyen.KHO)) {
                List<KiemTraCapCuuView> ls = new CapCuuDao().history(c, 1);
                int chuKy = new ThamSoDao().getInt(c, "CC_CHU_KY_KIEM_TRA_NGAY", 7);
                if (!ls.isEmpty()) {
                    kq = ls.get(0).kt().ketQua();
                    lan = ls.get(0).kt().thoiGian();
                }
                quaHan = lan == null || lan.toLocalDate().plusDays(chuKy).isBefore(h);
            }
            Long khoa = null;
            List<String> thieu = null;
            if (Session.can(Quyen.TAI_KHOAN)) {
                khoa = (long) new TaiKhoanDao().search(c, null, null, TrangThaiTaiKhoan.BI_KHOA).size();
            }
            if (Session.can(Quyen.PHAN_CONG)) {
                thieu = new ArrayList<>();
                List<PhanCongView> pcs = new PhanCongDao().range(c, h, h, null);
                for (CaLamViec ca : new CaLamViecDao().all(c)) {
                    List<String> lack = new ArrayList<>();
                    for (VaiTro v : List.of(VaiTro.BS, VaiTro.YT, VaiTro.LT)) {
                        long co = pcs.stream().filter(p -> p.pc().maCa().equals(ca.maCa()) && p.chucDanh() == v && p.pc().trangThai() != TrangThaiPhanCong.DA_HUY).count();
                        if (co < ca.dinhMuc(v)) lack.add(v.label() + " thiếu " + (ca.dinhMuc(v) - co));
                    }
                    if (!lack.isEmpty()) thieu.add(ca.tenCa() + ": " + String.join(", ", lack));
                }
            }
            return new DashboardData(lh, cho, dang, xong, dt, no, soNo, yc, dc, bg, duoi, sap, het, kq, lan, quaHan, khoa, thieu, tkd);
        });
    }
}
