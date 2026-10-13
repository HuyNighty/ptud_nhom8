package vn.n08.nhakhoa.service;

import vn.n08.nhakhoa.dao.BenhNhanDao;
import vn.n08.nhakhoa.dao.DichVuDao;
import vn.n08.nhakhoa.dao.HangHoaDao;
import vn.n08.nhakhoa.dao.HoaDonDao;
import vn.n08.nhakhoa.dao.MaDao;
import vn.n08.nhakhoa.dao.PhieuKhamDao;
import vn.n08.nhakhoa.dao.ThanhToanDao;
import vn.n08.nhakhoa.dao.YeuCauHuyDao;
import vn.n08.nhakhoa.dto.ChiDinhView;
import vn.n08.nhakhoa.dto.CongNoView;
import vn.n08.nhakhoa.dto.DongHoaDonForm;
import vn.n08.nhakhoa.dto.HoaDonChiTiet;
import vn.n08.nhakhoa.dto.HoaDonView;
import vn.n08.nhakhoa.dto.PhieuKhamView;
import vn.n08.nhakhoa.dto.ThanhToanForm;
import vn.n08.nhakhoa.dto.YeuCauHuyView;
import vn.n08.nhakhoa.model.BenhNhan;
import vn.n08.nhakhoa.model.ChiTietHoaDon;
import vn.n08.nhakhoa.model.DichVu;
import vn.n08.nhakhoa.model.HoaDon;
import vn.n08.nhakhoa.model.PhieuKham;
import vn.n08.nhakhoa.model.PhuongThucThanhToan;
import vn.n08.nhakhoa.model.ThanhToan;
import vn.n08.nhakhoa.model.TrangThaiDuyet;
import vn.n08.nhakhoa.model.TrangThaiHoaDon;
import vn.n08.nhakhoa.model.TrangThaiPhieuKham;
import vn.n08.nhakhoa.model.TrangThaiThanhToan;
import vn.n08.nhakhoa.model.YeuCauHuyHoaDon;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.util.Tx;
import vn.n08.nhakhoa.util.Validator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UC04 Quản lý thanh toán, UC05 Theo dõi công nợ, yêu cầu hủy hóa đơn và duyệt (Figma QL-08/09).
 * - Mỗi lần thu có phương thức riêng (Tiền mặt / Chuyển khoản / Thẻ POS); không thu vượt số còn nợ.
 * - Mã giao dịch bắt buộc với CK/POS; tiền mặt tính tiền thối (không lưu).
 * - Không tài khoản nào duyệt/từ chối yêu cầu do chính mình tạo, kể cả khi gọi Service trực tiếp.
 */
public class HoaDonService {
    private final HoaDonDao dao = new HoaDonDao();
    private final ThanhToanDao ttDao = new ThanhToanDao();
    private final YeuCauHuyDao ycDao = new YeuCauHuyDao();
    private final PhieuKhamDao pkDao = new PhieuKhamDao();
    private final BenhNhanDao bnDao = new BenhNhanDao();
    private final DichVuDao dvDao = new DichVuDao();
    private final HangHoaDao hhDao = new HangHoaDao();
    private final MaDao maDao = new MaDao();

    public List<HoaDonView> danhSach(LocalDate tu, LocalDate den, TrangThaiHoaDon tt, String tuKhoa) {
        Session.requireAny(Quyen.THU_NGAN, Quyen.DUYET_HUY, Quyen.HUY_GIAO_DICH);
        return Tx.read(c -> dao.search(c, Now.startOf(tu), Now.endOf(den), tt, tuKhoa));
    }

    public HoaDonChiTiet chiTiet(String ma) {
        Session.requireAny(Quyen.THU_NGAN, Quyen.DUYET_HUY, Quyen.HUY_GIAO_DICH, Quyen.BENH_NHAN_XEM);
        return Tx.read(c -> new HoaDonChiTiet(dao.findView(c, ma).orElseThrow(() -> new BusinessException("Không tìm thấy hóa đơn " + ma + ".")),
                dao.lines(c, ma), ttDao.byInvoice(c, ma), ycDao.byInvoice(c, ma)));
    }

    /** Lượt khám đã kết luận chưa lập hóa đơn. */
    public List<PhieuKhamView> phieuKhamChoLapHoaDon() {
        Session.require(Quyen.THU_NGAN);
        return Tx.read(pkDao::concludedWithoutInvoice);
    }

    /** Gợi ý dòng hóa đơn từ chỉ định cận lâm sàng của lượt khám (người dùng có thể sửa/thêm). */
    public List<DongHoaDonForm> goiYDong(String maPk) {
        Session.require(Quyen.THU_NGAN);
        return Tx.read(c -> {
            List<DongHoaDonForm> out = new ArrayList<>();
            for (ChiDinhView cd : pkDao.chiDinhOf(c, maPk)) out.add(new DongHoaDonForm(DongHoaDonForm.Loai.DICH_VU, cd.cd().maDichVu(), 1, cd.donGia()));
            return out;
        });
    }

    public List<DichVu> bangGia() {
        Session.user();
        return Tx.read(c -> dvDao.all(c, true));
    }

    /** Lập hóa đơn. Dịch vụ lấy đơn giá hiện hành; thuốc/vật tư nhập đơn giá. Tổng tiền phải lớn hơn 0. */
    public String lap(String maBn, String maPk, List<DongHoaDonForm> dong) {
        Session.require(Quyen.THU_NGAN);
        if (dong == null || dong.isEmpty()) throw new BusinessException("dong", "Hóa đơn cần ít nhất một dòng.");
        return Tx.write(c -> {
            BenhNhan bn = bnDao.find(c, maBn).orElseThrow(() -> new BusinessException("benhNhan", "Chọn bệnh nhân."));
            if (maPk != null) {
                PhieuKham pk = pkDao.find(c, maPk).orElseThrow(() -> new BusinessException("Không tìm thấy phiếu khám."));
                if (!pk.maBenhNhan().equals(maBn)) throw new BusinessException("Phiếu khám không thuộc bệnh nhân này.");
                if (pk.trangThai() == TrangThaiPhieuKham.DA_HUY) throw new BusinessException("Phiếu khám đã hủy.");
                if (dao.phieuKhamHasInvoice(c, maPk)) throw new BusinessException("Lượt khám " + maPk + " đã có hóa đơn còn hiệu lực.");
            }
            String ma = maDao.next(c, "HD");
            dao.insert(c, new HoaDon(ma, Session.user().maNhanVien(), maBn, maPk, Now.now(), TrangThaiHoaDon.CHUA_THANH_TOAN));
            BigDecimal tong = BigDecimal.ZERO;
            for (DongHoaDonForm d : dong) {
                if (d.soLuong() <= 0) throw new BusinessException("dong", "Số lượng phải lớn hơn 0.");
                ChiTietHoaDon l = switch (d.loai()) {
                    case DICH_VU -> {
                        DichVu dv = dvDao.find(c, d.ma()).orElseThrow(() -> new BusinessException("dong", "Dịch vụ " + d.ma() + " không tồn tại."));
                        if (!dv.dangApDung()) throw new BusinessException("dong", dv.tenDichVu() + " không còn áp dụng.");
                        yield new ChiTietHoaDon(null, ma, dv.maDichVu(), null, null, d.soLuong(), dv.donGia());
                    }
                    case THUOC -> {
                        hhDao.findThuoc(c, d.ma()).orElseThrow(() -> new BusinessException("dong", "Thuốc " + d.ma() + " không tồn tại."));
                        yield new ChiTietHoaDon(null, ma, null, d.ma(), null, d.soLuong(), donGia(d));
                    }
                    case VAT_TU -> {
                        hhDao.findVatTu(c, d.ma()).orElseThrow(() -> new BusinessException("dong", "Vật tư " + d.ma() + " không tồn tại."));
                        yield new ChiTietHoaDon(null, ma, null, null, d.ma(), d.soLuong(), donGia(d));
                    }
                };
                dao.insertLine(c, l);
                tong = tong.add(l.thanhTien());
            }
            if (tong.signum() <= 0) throw new BusinessException("dong", "Tổng tiền hóa đơn phải lớn hơn 0.");
            Audit.log(c, "Thu ngân", "Lập hóa đơn", ma, bn.hoTen() + " · " + Fmt.money(tong) + (maPk == null ? "" : " · lượt khám " + maPk));
            return ma;
        });
    }

    private static BigDecimal donGia(DongHoaDonForm d) {
        if (d.donGia() == null || d.donGia().signum() < 0) throw new BusinessException("dong", "Nhập đơn giá cho thuốc / vật tư.");
        return d.donGia();
    }

    /** Thu tiền một lần. Trả mã giao dịch thanh toán. */
    public String thuTien(String maHd, ThanhToanForm f) {
        Session.require(Quyen.THU_NGAN);
        if (f.phuongThuc() == null) throw new BusinessException("phuongThuc", "Chọn phương thức thanh toán.");
        if (f.soTien() == null || f.soTien().signum() <= 0) throw new BusinessException("soTien", "Số tiền thu phải lớn hơn 0.");
        String maGd = null;
        BigDecimal khachDua = null;
        if (f.phuongThuc() == PhuongThucThanhToan.TIEN_MAT) {
            if (f.tienKhachDua() == null) throw new BusinessException("tienKhachDua", "Nhập số tiền khách đưa.");
            if (f.tienKhachDua().compareTo(f.soTien()) < 0) throw new BusinessException("tienKhachDua", "Tiền khách đưa nhỏ hơn số tiền thu.");
            khachDua = f.tienKhachDua();
        } else {
            maGd = Validator.maxLen(Validator.required(f.maGiaoDich(), "maGiaoDich", "Mã giao dịch"), 50, "maGiaoDich", "Mã giao dịch");
        }
        String gd = maGd;
        BigDecimal kd = khachDua;
        return Tx.write(c -> {
            if (dao.lock(c, maHd) == 0) throw new BusinessException("Không tìm thấy hóa đơn " + maHd + ".");
            HoaDon hd = dao.find(c, maHd).orElseThrow();
            if (hd.trangThai() == TrangThaiHoaDon.DA_HUY) throw new BusinessException("Hóa đơn đã hủy.");
            if (ycDao.hasPending(c, maHd)) throw new BusinessException("Hóa đơn đang có yêu cầu hủy chờ duyệt; không thu thêm.");
            BigDecimal conNo = dao.tongTien(c, maHd).subtract(dao.daThanhToan(c, maHd));
            if (conNo.signum() <= 0) throw new BusinessException("Hóa đơn đã thanh toán đủ.");
            if (f.soTien().compareTo(conNo) > 0) throw new BusinessException("soTien", "Không thu vượt số còn nợ (" + Fmt.money(conNo) + ").");
            if (gd != null && ttDao.transactionCodeUsed(c, f.phuongThuc(), gd)) {
                throw new BusinessException("maGiaoDich", "Mã giao dịch " + gd + " đã được ghi nhận cho một lần thu khác.");
            }
            String ma = maDao.next(c, "TT");
            ttDao.insert(c, new ThanhToan(ma, maHd, Now.now(), f.soTien(), f.phuongThuc(), gd, kd, TrangThaiThanhToan.THANH_CONG,
                    Session.user().maNhanVien(), null, null, null));
            TrangThaiHoaDon moi = capNhatTrangThai(c, maHd);
            Audit.log(c, "Thu ngân", "Thu tiền " + Fmt.money(f.soTien()) + " · " + f.phuongThuc().label(), maHd + " · " + ma,
                    (gd == null ? "Tiền thối " + Fmt.money(kd.subtract(f.soTien())) : "Mã GD " + gd) + " · " + moi.label());
            return ma;
        });
    }

    /** Hủy một lần thu nhập sai (quản lý); lần thu giữ trong lịch sử với trạng thái DA_HUY. */
    public void huyGiaoDich(String maTt, String lyDo) {
        Session.require(Quyen.HUY_GIAO_DICH);
        String ly = Validator.required(lyDo, "lyDo", "Lý do hủy giao dịch");
        Tx.run(c -> {
            ThanhToan tt = ttDao.find(c, maTt).orElseThrow(() -> new BusinessException("Không tìm thấy giao dịch."));
            dao.lock(c, tt.maHoaDon());
            HoaDon hd = dao.find(c, tt.maHoaDon()).orElseThrow();
            if (hd.trangThai() == TrangThaiHoaDon.DA_HUY) throw new BusinessException("Hóa đơn đã hủy.");
            if (ttDao.voidPayment(c, maTt, Session.user().maTaiKhoan(), Now.now(), ly) == 0) throw new BusinessException("Giao dịch đã hủy trước đó.");
            TrangThaiHoaDon moi = capNhatTrangThai(c, tt.maHoaDon());
            Audit.log(c, "Thu ngân", "Hủy giao dịch " + Fmt.money(tt.soTien()), tt.maHoaDon() + " · " + maTt, ly + " → hóa đơn " + moi.label());
        });
    }

    private TrangThaiHoaDon capNhatTrangThai(Connection c, String maHd) throws SQLException {
        BigDecimal tong = dao.tongTien(c, maHd);
        BigDecimal tra = dao.daThanhToan(c, maHd);
        TrangThaiHoaDon tt = tra.signum() == 0 ? TrangThaiHoaDon.CHUA_THANH_TOAN
                : tra.compareTo(tong) >= 0 ? TrangThaiHoaDon.DA_THANH_TOAN : TrangThaiHoaDon.THANH_TOAN_MOT_PHAN;
        dao.setTrangThai(c, maHd, tt);
        return tt;
    }

    public String guiYeuCauHuy(String maHd, String lyDo) {
        Session.require(Quyen.YEU_CAU_HUY);
        String ly = Validator.maxLen(Validator.required(lyDo, "lyDo", "Lý do hủy hóa đơn"), 500, "lyDo", "Lý do");
        return Tx.write(c -> {
            dao.lock(c, maHd);
            HoaDon hd = dao.find(c, maHd).orElseThrow(() -> new BusinessException("Không tìm thấy hóa đơn."));
            if (hd.trangThai() == TrangThaiHoaDon.DA_HUY) throw new BusinessException("Hóa đơn đã hủy.");
            if (ycDao.hasPending(c, maHd)) throw new BusinessException("Hóa đơn đã có yêu cầu hủy đang chờ duyệt.");
            String ma = maDao.next(c, "YC");
            ycDao.insert(c, new YeuCauHuyHoaDon(ma, maHd, Session.user().maTaiKhoan(), Now.now(), ly, TrangThaiDuyet.CHO_DUYET, null, null, null));
            Audit.log(c, "Thu ngân", "Gửi yêu cầu hủy hóa đơn", ma + " · " + maHd, ly);
            return ma;
        });
    }

    public List<YeuCauHuyView> danhSachYeuCau(TrangThaiDuyet tt) {
        Session.requireAny(Quyen.DUYET_HUY, Quyen.YEU_CAU_HUY);
        return Tx.read(c -> ycDao.search(c, tt));
    }

    /**
     * Duyệt / từ chối yêu cầu hủy. Người duyệt phải khác người tạo (Service + điều kiện UPDATE + CHECK CSDL).
     * Không duyệt hủy khi hóa đơn còn lần thu thành công: phải hủy (hoàn) các lần thu trước.
     */
    public void xuLyYeuCau(String maYc, boolean dongY, String lyDo) {
        Session.require(Quyen.DUYET_HUY);
        String ly = Validator.maxLen(Validator.required(lyDo, "lyDoDuyet", dongY ? "Ghi chú duyệt" : "Lý do từ chối"), 500, "lyDoDuyet", "Lý do");
        Session.User u = Session.user();
        Tx.run(c -> {
            YeuCauHuyHoaDon yc = ycDao.find(c, maYc).orElseThrow(() -> new BusinessException("Không tìm thấy yêu cầu " + maYc + "."));
            if (yc.trangThai() != TrangThaiDuyet.CHO_DUYET) throw new BusinessException("Yêu cầu đã được xử lý.");
            if (yc.maTaiKhoanTao().equals(u.maTaiKhoan())) {
                throw new BusinessException("Yêu cầu " + maYc + " do chính tài khoản " + u.tenDangNhap() + " tạo nên không được duyệt hay từ chối. Cần tài khoản khác có quyền duyệt.");
            }
            dao.lock(c, yc.maHoaDon());
            if (dongY && dao.daThanhToan(c, yc.maHoaDon()).signum() > 0) {
                throw new BusinessException("Hóa đơn còn " + Fmt.money(dao.daThanhToan(c, yc.maHoaDon())) + " đã thu. Hủy (hoàn) các lần thu trước khi duyệt hủy hóa đơn.");
            }
            if (ycDao.decide(c, maYc, dongY ? TrangThaiDuyet.DA_DUYET : TrangThaiDuyet.TU_CHOI, u.maTaiKhoan(), Now.now(), ly) == 0) throw TaiKhoanService.stale();
            if (dongY) dao.setTrangThai(c, yc.maHoaDon(), TrangThaiHoaDon.DA_HUY);
            Audit.log(c, "Duyệt hủy hóa đơn", dongY ? "Duyệt yêu cầu hủy" : "Từ chối yêu cầu hủy", maYc + " · " + yc.maHoaDon(), ly);
        });
    }

    /** UC05: công nợ theo bệnh nhân (tính từ hóa đơn chưa hủy còn nợ). */
    public List<CongNoView> congNo() {
        Session.requireAny(Quyen.THU_NGAN, Quyen.DUYET_HUY);
        return Tx.read(c -> {
            Map<String, CongNoView> m = new LinkedHashMap<>();
            for (HoaDonView h : dao.withDebt(c)) {
                String k = h.hd().maBenhNhan();
                CongNoView o = m.get(k);
                m.put(k, o == null ? new CongNoView(k, h.tenBenhNhan(), h.soDienThoai(), 1, h.conNo(), h.hd().ngayLap())
                        : new CongNoView(k, o.tenBenhNhan(), o.soDienThoai(), o.soHoaDon() + 1, o.tongNo().add(h.conNo()), o.hoaDonCuNhat()));
            }
            return m.values().stream().sorted((a, b) -> b.tongNo().compareTo(a.tongNo())).toList();
        });
    }

    public List<HoaDonView> hoaDonConNo() {
        Session.requireAny(Quyen.THU_NGAN, Quyen.DUYET_HUY);
        return Tx.read(dao::withDebt);
    }
}
