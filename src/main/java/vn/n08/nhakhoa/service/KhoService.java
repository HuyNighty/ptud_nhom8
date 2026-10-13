package vn.n08.nhakhoa.service;

import vn.n08.nhakhoa.dao.HangHoaDao;
import vn.n08.nhakhoa.dao.LoHangDao;
import vn.n08.nhakhoa.dao.MaDao;
import vn.n08.nhakhoa.dao.NhaCungCapDao;
import vn.n08.nhakhoa.dao.PhieuKhoDao;
import vn.n08.nhakhoa.dao.ThamSoDao;
import vn.n08.nhakhoa.dto.LoHangView;
import vn.n08.nhakhoa.dto.NhapKhoForm;
import vn.n08.nhakhoa.dto.PhieuKhoDong;
import vn.n08.nhakhoa.dto.PhieuKhoView;
import vn.n08.nhakhoa.dto.SoKsdbRow;
import vn.n08.nhakhoa.dto.TonKhoItem;
import vn.n08.nhakhoa.dto.XuatKhoForm;
import vn.n08.nhakhoa.model.LoHang;
import vn.n08.nhakhoa.model.LoaiNhap;
import vn.n08.nhakhoa.model.NhaCungCap;
import vn.n08.nhakhoa.model.PhieuNhapKho;
import vn.n08.nhakhoa.model.PhieuXuatKho;
import vn.n08.nhakhoa.model.Thuoc;
import vn.n08.nhakhoa.model.TrangThaiNCC;
import vn.n08.nhakhoa.model.VatTu;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * UC27 Tìm kiếm, UC29 Quản lý vật tư/thuốc, UC30 Nhập kho, UC31 Xuất kho (FEFO, chặn lô hết hạn),
 * UC32 Hoàn trả (nguyên seal), UC33 Thuốc kiểm soát đặc biệt, UC36 Tồn kho, UC37 Hạn sử dụng.
 * Tồn lô chỉ thay đổi trong transaction cùng với phiếu.
 */
public class KhoService {
    public static final String BO_PHAN_CAP_CUU = "Hộp cấp cứu";
    private final HangHoaDao hhDao = new HangHoaDao();
    private final LoHangDao loDao = new LoHangDao();
    private final PhieuKhoDao pDao = new PhieuKhoDao();
    private final NhaCungCapDao nccDao = new NhaCungCapDao();
    private final ThamSoDao tsDao = new ThamSoDao();
    private final MaDao maDao = new MaDao();

    public List<TonKhoItem> tonKho(String tuKhoa) {
        Session.requireAny(Quyen.KHO, Quyen.KIEM_KE, Quyen.DUYET_KHO, Quyen.CAP_CUU);
        return Tx.read(c -> hhDao.tonKho(c, Now.today(), tuKhoa));
    }

    public List<LoHangView> loCua(boolean laThuoc, String maHang) {
        Session.requireAny(Quyen.KHO, Quyen.KIEM_KE, Quyen.DUYET_KHO, Quyen.CAP_CUU);
        return Tx.read(c -> loDao.byItem(c, laThuoc, maHang));
    }

    public List<LoHangView> loConTon() {
        Session.requireAny(Quyen.KHO, Quyen.KIEM_KE);
        return Tx.read(loDao::inStock);
    }

    /** UC37: lô còn tồn sắp hết hạn trong HSD_CANH_BAO_NGAY ngày hoặc đã hết hạn. */
    public List<LoHangView> canhBaoHanDung() {
        Session.requireAny(Quyen.KHO, Quyen.KIEM_KE, Quyen.DUYET_KHO);
        return Tx.read(c -> loDao.expiringBy(c, Now.today().plusDays(tsDao.getInt(c, "HSD_CANH_BAO_NGAY", 30))));
    }

    public int nguongCanhBaoHsd() {
        Session.user();
        return Tx.read(c -> tsDao.getInt(c, "HSD_CANH_BAO_NGAY", 30));
    }

    public List<Thuoc> danhSachThuoc() {
        Session.user();
        return Tx.read(hhDao::allThuoc);
    }

    public List<VatTu> danhSachVatTu() {
        Session.user();
        return Tx.read(hhDao::allVatTu);
    }

    public List<NhaCungCap> nhaCungCapDangHopTac() {
        Session.requireAny(Quyen.KHO, Quyen.CAP_CUU);
        return Tx.read(nccDao::active);
    }

    // ---------- UC29 danh mục ----------
    public String luuThuoc(Thuoc t) {
        Session.require(Quyen.KHO);
        String ten = Validator.maxLen(Validator.required(t.tenThuoc(), "ten", "Tên thuốc"), 150, "ten", "Tên thuốc");
        String dvt = Validator.maxLen(Validator.required(t.donViTinh(), "donViTinh", "Đơn vị tính"), 30, "donViTinh", "Đơn vị tính");
        if (t.dinhMucToiThieu() < 0) throw new BusinessException("dinhMuc", "Định mức tối thiểu không âm.");
        return Tx.write(c -> {
            if (hhDao.nameUsed(c, true, ten, t.maThuoc())) throw new BusinessException("ten", "Đã có thuốc tên \"" + ten + "\".");
            Thuoc x = new Thuoc(t.maThuoc(), ten, dvt, t.kiemSoatDacBiet(), Validator.optional(t.hamLuong()), Validator.optional(t.hoatChat()),
                    t.dinhMucToiThieu(), false);
            if (t.maThuoc() == null) {
                String ma = maDao.next(c, "T");
                hhDao.insertThuoc(c, new Thuoc(ma, x.tenThuoc(), x.donViTinh(), x.kiemSoatDacBiet(), x.hamLuong(), x.hoatChat(), x.dinhMucToiThieu(), false));
                Audit.log(c, "Kho", "Thêm thuốc" + (x.kiemSoatDacBiet() ? " (kiểm soát đặc biệt)" : ""), ma, ten);
                return ma;
            }
            Thuoc cu = hhDao.findThuoc(c, t.maThuoc()).orElseThrow(() -> new BusinessException("Không tìm thấy thuốc."));
            hhDao.updateThuoc(c, x);
            Audit.log(c, "Kho", "Sửa thuốc", t.maThuoc(), ten + (cu.kiemSoatDacBiet() != x.kiemSoatDacBiet() ? " · đổi cờ kiểm soát đặc biệt → " + x.kiemSoatDacBiet() : ""));
            return t.maThuoc();
        });
    }

    public String luuVatTu(VatTu v) {
        Session.require(Quyen.KHO);
        String ten = Validator.maxLen(Validator.required(v.tenVatTu(), "ten", "Tên vật tư"), 150, "ten", "Tên vật tư");
        String dvt = Validator.maxLen(Validator.required(v.donViTinh(), "donViTinh", "Đơn vị tính"), 30, "donViTinh", "Đơn vị tính");
        if (v.dinhMucToiThieu() < 0) throw new BusinessException("dinhMuc", "Định mức tối thiểu không âm.");
        return Tx.write(c -> {
            if (hhDao.nameUsed(c, false, ten, v.maVatTu())) throw new BusinessException("ten", "Đã có vật tư tên \"" + ten + "\".");
            if (v.maVatTu() == null) {
                String ma = maDao.next(c, "V");
                hhDao.insertVatTu(c, new VatTu(ma, ten, dvt, Validator.optional(v.moTa()), v.dinhMucToiThieu(), false));
                Audit.log(c, "Kho", "Thêm vật tư", ma, ten);
                return ma;
            }
            hhDao.findVatTu(c, v.maVatTu()).orElseThrow(() -> new BusinessException("Không tìm thấy vật tư."));
            hhDao.updateVatTu(c, new VatTu(v.maVatTu(), ten, dvt, Validator.optional(v.moTa()), v.dinhMucToiThieu(), v.daDatHang()));
            Audit.log(c, "Kho", "Sửa vật tư", v.maVatTu(), ten);
            return v.maVatTu();
        });
    }

    /** UC36: đánh dấu đã đặt hàng (bổ sung) cho mặt hàng dưới định mức; tự bỏ khi nhập kho. */
    public void danhDauDaDatHang(boolean laThuoc, String ma, boolean daDat) {
        Session.require(Quyen.KHO);
        Tx.run(c -> {
            if (hhDao.setDaDatHang(c, laThuoc, ma, daDat) == 0) throw new BusinessException("Không tìm thấy mặt hàng " + ma + ".");
            Audit.log(c, "Kho", daDat ? "Đánh dấu đã đặt hàng" : "Bỏ đánh dấu đã đặt hàng", ma, null);
        });
    }

    // ---------- UC30 nhập ----------
    public String nhapKho(NhapKhoForm f) {
        Session.require(Quyen.KHO);
        if (f.dong() == null || f.dong().isEmpty()) throw new BusinessException("dong", "Phiếu nhập cần ít nhất một dòng.");
        String ghiChu = Validator.maxLen(Validator.optional(f.ghiChu()), 300, "ghiChu", "Ghi chú");
        return Tx.write(c -> {
            NhaCungCap ncc = nccDao.find(c, f.maNhaCungCap()).orElseThrow(() -> new BusinessException("ncc", "Chọn nhà cung cấp."));
            if (ncc.trangThai() != TrangThaiNCC.DANG_HOP_TAC) throw new BusinessException("ncc", ncc.tenNhaCungCap() + " đã ngừng hợp tác.");
            String ma = maDao.next(c, "PN");
            pDao.insertNhap(c, new PhieuNhapKho(ma, Now.now(), "HOAN_TAT", LoaiNhap.NHAP_NCC, ncc.maNhaCungCap(), null, Session.user().maNhanVien(), ghiChu));
            Set<String> seen = new HashSet<>();
            int tong = 0;
            for (NhapKhoForm.Dong d : f.dong()) {
                String ten = tenHang(c, d.laThuoc(), d.maHang());
                String soLo = Validator.maxLen(Validator.required(d.soLo(), "dong", "Số lô của " + ten), 30, "dong", "Số lô");
                if (d.soLuong() <= 0) throw new BusinessException("dong", ten + ": số lượng nhập phải lớn hơn 0.");
                if (d.donGia() == null || d.donGia().signum() < 0) throw new BusinessException("dong", ten + ": nhập đơn giá nhập.");
                if (d.laThuoc() && d.hanSuDung() == null) throw new BusinessException("dong", ten + ": thuốc phải có hạn sử dụng.");
                if (d.hanSuDung() != null && !d.hanSuDung().isAfter(Now.today())) throw new BusinessException("dong", ten + " lô " + soLo + " đã hết hạn, không nhập kho.");
                if (d.ngaySanXuat() != null && d.ngaySanXuat().isAfter(Now.today())) throw new BusinessException("dong", ten + ": ngày sản xuất sau hôm nay.");
                if (d.ngaySanXuat() != null && d.hanSuDung() != null && !d.hanSuDung().isAfter(d.ngaySanXuat())) {
                    throw new BusinessException("dong", ten + ": hạn sử dụng phải sau ngày sản xuất.");
                }
                LoHang lo = loDao.findBySoLo(c, d.laThuoc(), d.maHang(), soLo, ncc.maNhaCungCap()).orElse(null);
                String maLo;
                if (lo == null) {
                    maLo = maDao.next(c, "LO");
                    loDao.insert(c, new LoHang(maLo, soLo, d.ngaySanXuat(), d.hanSuDung(), d.laThuoc() ? d.maHang() : null, d.laThuoc() ? null : d.maHang(),
                            ncc.maNhaCungCap(), d.soLuong()));
                } else {
                    if (d.hanSuDung() != null && !d.hanSuDung().equals(lo.hanSuDung())) {
                        throw new BusinessException("dong", "Lô " + soLo + " đã có với hạn " + Fmt.date(lo.hanSuDung()) + "; hạn nhập khác — kiểm tra lại số lô.");
                    }
                    maLo = lo.maLo();
                    loDao.addStock(c, maLo, d.soLuong());
                }
                if (!seen.add(maLo)) throw new BusinessException("dong", ten + " lô " + soLo + " bị lặp trong phiếu; gộp thành một dòng.");
                pDao.insertNhapLine(c, ma, maLo, d.soLuong(), d.donGia());
                hhDao.setDaDatHang(c, d.laThuoc(), d.maHang(), false);
                tong += d.soLuong();
            }
            Audit.log(c, "Kho", "Nhập kho từ nhà cung cấp", ma, ncc.tenNhaCungCap() + " · " + f.dong().size() + " dòng · " + tong + " đơn vị");
            return ma;
        });
    }

    private String tenHang(Connection c, boolean laThuoc, String ma) throws SQLException {
        if (laThuoc) return hhDao.findThuoc(c, ma).map(Thuoc::tenThuoc).orElseThrow(() -> new BusinessException("dong", "Thuốc " + ma + " không tồn tại."));
        return hhDao.findVatTu(c, ma).map(VatTu::tenVatTu).orElseThrow(() -> new BusinessException("dong", "Vật tư " + ma + " không tồn tại."));
    }

    // ---------- UC31 xuất ----------
    /** Gợi ý phân bổ theo FEFO (hết hạn trước xuất trước) từ các lô còn hạn. */
    public List<XuatKhoForm.Dong> goiYFefo(boolean laThuoc, String maHang, int soLuong) {
        Session.requireAny(Quyen.KHO, Quyen.CAP_CUU);
        if (soLuong <= 0) throw new BusinessException("soLuong", "Số lượng xuất phải lớn hơn 0.");
        return Tx.read(c -> {
            List<XuatKhoForm.Dong> out = new ArrayList<>();
            int con = soLuong;
            for (LoHangView l : loDao.availableFefo(c, laThuoc, maHang, Now.today())) {
                if (con == 0) break;
                int lay = Math.min(con, l.lo().soLuongTon());
                out.add(new XuatKhoForm.Dong(l.lo().maLo(), lay));
                con -= lay;
            }
            if (con > 0) throw new BusinessException("soLuong", "Tồn khả dụng (lô còn hạn) không đủ: thiếu " + con + ".");
            return out;
        });
    }

    public String xuatKho(XuatKhoForm f) {
        Session.require(Quyen.KHO);
        return Tx.write(c -> xuat(c, f));
    }

    /** Dùng chung cho xuất kho và bổ sung hộp cấp cứu (trong transaction của nơi gọi). */
    String xuat(Connection c, XuatKhoForm f) throws SQLException {
        String bp = Validator.maxLen(Validator.required(f.boPhanNhan(), "boPhanNhan", "Bộ phận nhận"), 100, "boPhanNhan", "Bộ phận nhận");
        String nguoi = Validator.maxLen(Validator.optional(f.nguoiNhan()), 100, "nguoiNhan", "Người nhận");
        String lyDo = Validator.maxLen(Validator.optional(f.lyDo()), 300, "lyDo", "Lý do");
        if (f.dong() == null || f.dong().isEmpty()) throw new BusinessException("dong", "Phiếu xuất cần ít nhất một dòng.");
        String ma = maDao.next(c, "PX");
        pDao.insertXuat(c, new PhieuXuatKho(ma, Now.now(), bp, nguoi, lyDo, "HOAN_TAT", Session.user().maNhanVien()));
        boolean coKsdb = false;
        Set<String> seen = new HashSet<>();
        for (XuatKhoForm.Dong d : f.dong()) {
            LoHangView lv = loDao.findView(c, d.maLo()).orElseThrow(() -> new BusinessException("dong", "Lô " + d.maLo() + " không tồn tại."));
            if (!seen.add(d.maLo())) throw new BusinessException("dong", "Lô " + d.maLo() + " bị lặp trong phiếu.");
            if (d.soLuong() <= 0) throw new BusinessException("dong", "Số lượng xuất phải lớn hơn 0.");
            if (lv.lo().daHetHan(Now.today())) throw new BusinessException("dong", lv.tenHang() + " lô " + lv.lo().soLo() + " đã hết hạn — không xuất; lập phiếu hủy.");
            if (loDao.takeStock(c, d.maLo(), d.soLuong()) == 0) {
                throw new BusinessException("dong", lv.tenHang() + " lô " + lv.lo().soLo() + " chỉ còn " + lv.lo().soLuongTon() + ", không đủ xuất " + d.soLuong() + ".");
            }
            pDao.insertXuatLine(c, ma, d.maLo(), d.soLuong());
            coKsdb |= lv.kiemSoatDacBiet();
        }
        String bs = null;
        if (coKsdb) {
            if (nguoi == null) throw new BusinessException("nguoiNhan", "Xuất thuốc kiểm soát đặc biệt: bắt buộc ghi người nhận.");
            if (f.maBacSiChiuTrachNhiem() == null) throw new BusinessException("bacSi", "Xuất thuốc kiểm soát đặc biệt: chọn bác sĩ chịu trách nhiệm.");
            bs = BacSiPhuTrach.requireActiveDoctor(c, f.maBacSiChiuTrachNhiem()).maNhanVien();
        }
        Audit.log(c, "Kho", "Xuất kho" + (coKsdb ? " (có thuốc kiểm soát đặc biệt)" : ""), ma, bp + (nguoi == null ? "" : " · " + nguoi)
                + " · " + f.dong().size() + " dòng", bs);
        return ma;
    }

    // ---------- UC32 hoàn trả ----------
    public List<PhieuKhoDong> dongPhieuXuat(String maPx) {
        Session.require(Quyen.KHO);
        return Tx.read(c -> {
            pDao.findXuat(c, maPx).orElseThrow(() -> new BusinessException("Không tìm thấy phiếu xuất " + maPx + "."));
            List<PhieuKhoDong> out = new ArrayList<>();
            for (PhieuKhoDong d : pDao.linesXuat(c, maPx)) {
                int con = d.soLuong() - pDao.qtyDaHoanTra(c, maPx, d.maLo());
                out.add(new PhieuKhoDong(d.maLo(), d.soLo(), d.maHang(), d.tenHang(), d.donViTinh(), d.hanSuDung(), con, null, d.kiemSoatDacBiet()));
            }
            return out;
        });
    }

    /** Hoàn trả hàng đã xuất về kho: chỉ hàng còn nguyên seal, không vượt số đã xuất trừ đã hoàn, lô chưa hết hạn. */
    public String hoanTra(String maPx, List<XuatKhoForm.Dong> dong, boolean xacNhanNguyenSeal, String ghiChu) {
        Session.require(Quyen.KHO);
        if (!xacNhanNguyenSeal) throw new BusinessException("seal", "Chỉ nhận hoàn trả hàng còn nguyên seal/niêm phong. Xác nhận đã kiểm tra.");
        if (dong == null || dong.isEmpty()) throw new BusinessException("dong", "Nhập số lượng hoàn trả.");
        String gc = Validator.maxLen(Validator.required(ghiChu, "ghiChu", "Lý do hoàn trả"), 300, "ghiChu", "Lý do");
        return Tx.write(c -> {
            pDao.findXuat(c, maPx).orElseThrow(() -> new BusinessException("Không tìm thấy phiếu xuất " + maPx + "."));
            String ma = maDao.next(c, "PN");
            pDao.insertNhap(c, new PhieuNhapKho(ma, Now.now(), "HOAN_TAT", LoaiNhap.HOAN_TRA, null, maPx, Session.user().maNhanVien(), "Nguyên seal · " + gc));
            int tong = 0;
            for (XuatKhoForm.Dong d : dong) {
                if (d.soLuong() <= 0) continue;
                int daXuat = pDao.qtyXuat(c, maPx, d.maLo());
                if (daXuat == 0) throw new BusinessException("dong", "Lô " + d.maLo() + " không có trong phiếu xuất " + maPx + ".");
                int con = daXuat - pDao.qtyDaHoanTra(c, maPx, d.maLo());
                if (d.soLuong() > con) throw new BusinessException("dong", "Lô " + d.maLo() + " chỉ còn hoàn trả tối đa " + con + ".");
                LoHang lo = loDao.find(c, d.maLo()).orElseThrow();
                if (lo.daHetHan(Now.today())) throw new BusinessException("dong", "Lô " + lo.soLo() + " đã hết hạn — không nhập lại kho.");
                loDao.addStock(c, d.maLo(), d.soLuong());
                pDao.insertNhapLine(c, ma, d.maLo(), d.soLuong(), BigDecimal.ZERO);
                tong += d.soLuong();
            }
            if (tong == 0) throw new BusinessException("dong", "Nhập số lượng hoàn trả lớn hơn 0 cho ít nhất một lô.");
            Audit.log(c, "Kho", "Hoàn trả về kho", ma, "Từ phiếu xuất " + maPx + " · " + tong + " đơn vị · " + gc);
            return ma;
        });
    }

    // ---------- tra cứu phiếu ----------
    public List<PhieuKhoView> danhSachPhieu(LocalDate tu, LocalDate den) {
        Session.requireAny(Quyen.KHO, Quyen.DUYET_KHO);
        return Tx.read(c -> pDao.list(c, Now.startOf(tu), Now.endOf(den)));
    }

    public List<PhieuKhoDong> dongPhieu(String ma, boolean laXuat) {
        Session.requireAny(Quyen.KHO, Quyen.DUYET_KHO);
        return Tx.read(c -> laXuat ? pDao.linesXuat(c, ma) : pDao.linesNhap(c, ma));
    }

    /** UC33: sổ theo dõi thuốc kiểm soát đặc biệt. */
    public List<SoKsdbRow> soKiemSoatDacBiet(LocalDate tu, LocalDate den) {
        Session.requireAny(Quyen.KHO, Quyen.DUYET_KHO);
        return Tx.read(c -> pDao.soKsdb(c, Now.startOf(tu), Now.endOf(den)));
    }
}
