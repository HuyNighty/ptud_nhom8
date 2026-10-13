package vn.n08.nhakhoa.service;

import vn.n08.nhakhoa.dao.BenhNhanDao;
import vn.n08.nhakhoa.dao.DichVuDao;
import vn.n08.nhakhoa.dao.DonThuocDao;
import vn.n08.nhakhoa.dao.HangHoaDao;
import vn.n08.nhakhoa.dao.LichHenDao;
import vn.n08.nhakhoa.dao.MaDao;
import vn.n08.nhakhoa.dao.PhieuKhamDao;
import vn.n08.nhakhoa.dto.DonThuocForm;
import vn.n08.nhakhoa.dto.PhieuKhamChiTiet;
import vn.n08.nhakhoa.dto.PhieuKhamView;
import vn.n08.nhakhoa.model.ChiDinhCanLamSang;
import vn.n08.nhakhoa.model.ChiTietDonThuoc;
import vn.n08.nhakhoa.model.DichVu;
import vn.n08.nhakhoa.model.DonThuoc;
import vn.n08.nhakhoa.model.HoSoBenhAn;
import vn.n08.nhakhoa.model.PhieuKham;
import vn.n08.nhakhoa.model.Thuoc;
import vn.n08.nhakhoa.model.TrangThaiCLS;
import vn.n08.nhakhoa.model.TrangThaiLichHen;
import vn.n08.nhakhoa.model.TrangThaiPhieuKham;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.CanhBaoException;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.util.Tx;
import vn.n08.nhakhoa.util.Validator;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * UC10 Ghi nhận kết quả khám, UC11 Chỉ định cận lâm sàng, UC16 Kê đơn.
 * Bác sĩ chịu trách nhiệm = bác sĩ của phiếu (gán khi bắt đầu khám); Admin phải chọn bác sĩ khi bắt đầu khám.
 */
public class KhamService {
    private final PhieuKhamDao dao = new PhieuKhamDao();
    private final DonThuocDao dtDao = new DonThuocDao();
    private final DichVuDao dvDao = new DichVuDao();
    private final HangHoaDao hhDao = new HangHoaDao();
    private final BenhNhanDao bnDao = new BenhNhanDao();
    private final LichHenDao lhDao = new LichHenDao();
    private final MaDao maDao = new MaDao();

    public List<PhieuKhamView> danhSach(LocalDate tu, LocalDate den, String maBs, TrangThaiPhieuKham tt, String tuKhoa) {
        Session.requireAny(Quyen.KHAM, Quyen.TIEP_NHAN);
        return Tx.read(c -> dao.search(c, Now.startOf(tu), Now.endOf(den), maBs, tt, tuKhoa));
    }

    public PhieuKhamChiTiet chiTiet(String maPk) {
        Session.requireAny(Quyen.KHAM, Quyen.BENH_NHAN_XEM, Quyen.THU_NGAN, Quyen.TIEP_NHAN);
        return Tx.read(c -> new PhieuKhamChiTiet(dao.findView(c, maPk).orElseThrow(() -> new BusinessException("Không tìm thấy phiếu khám " + maPk + ".")),
                dao.chiDinhOf(c, maPk), dtDao.viewByPhieuKham(c, maPk).orElse(null)));
    }

    public List<DichVu> dichVuCanLamSang() {
        Session.user();
        return Tx.read(c -> dvDao.all(c, true).stream().filter(DichVu::laCanLamSang).toList());
    }

    public List<Thuoc> danhMucThuoc() {
        Session.user();
        return Tx.read(hhDao::allThuoc);
    }

    /** Bắt đầu khám: gán bác sĩ chịu trách nhiệm (bác sĩ đăng nhập, hoặc bác sĩ do Admin chọn). */
    public void batDauKham(String maPk, String maBsChon) {
        Session.require(Quyen.KHAM);
        Tx.run(c -> {
            PhieuKham pk = load(c, maPk);
            String bs = BacSiPhuTrach.resolve(c, maBsChon);
            if (pk.trangThai() != TrangThaiPhieuKham.CHO_KHAM) throw new BusinessException("Phiếu không ở trạng thái chờ khám.");
            BacSiPhuTrach.requireOwner(pk.maNhanVien());
            if (dao.startExam(c, maPk, bs) == 0) throw TaiKhoanService.stale();
            Audit.log(c, "Khám bệnh", "Bắt đầu khám", maPk, pk.maNhanVien().equals(bs) ? null : "Đổi bác sĩ " + pk.maNhanVien() + " → " + bs, bs);
        });
    }

    /** UC10: lưu triệu chứng, chẩn đoán, kết luận (khi đang khám). */
    public void luuKetQua(String maPk, String trieuChung, String chanDoan, String ketLuan) {
        Session.require(Quyen.KHAM);
        String tc = Validator.maxLen(Validator.optional(trieuChung), 500, "trieuChung", "Triệu chứng");
        String cd = Validator.maxLen(Validator.optional(chanDoan), 1000, "chanDoan", "Chẩn đoán");
        String kl = Validator.maxLen(Validator.optional(ketLuan), 1000, "ketLuan", "Kết luận");
        Tx.run(c -> {
            PhieuKham pk = dangKham(c, maPk);
            if (dao.saveResult(c, maPk, tc, cd, kl) == 0) throw TaiKhoanService.stale();
            Audit.log(c, "Khám bệnh", "Ghi nhận kết quả khám", maPk, cd, pk.maNhanVien());
        });
    }

    /** Kết luận lượt khám: cần chẩn đoán, kết luận và mọi chỉ định CLS đã có kết quả. Lịch hẹn gắn kèm → HOAN_THANH. */
    public void ketLuan(String maPk) {
        Session.require(Quyen.KHAM);
        Tx.run(c -> {
            PhieuKham pk = dangKham(c, maPk);
            if (pk.chanDoan() == null || pk.ketLuan() == null) throw new BusinessException("chanDoan", "Nhập và lưu chẩn đoán, kết luận trước khi kết thúc khám.");
            long cho = dao.countChiDinhCho(c, maPk);
            if (cho > 0) throw new BusinessException("Còn " + cho + " chỉ định cận lâm sàng chưa có kết quả.");
            if (dao.conclude(c, maPk) == 0) throw TaiKhoanService.stale();
            if (pk.maLichHen() != null) lhDao.transition(c, pk.maLichHen(), TrangThaiLichHen.HOAN_THANH, null, TrangThaiLichHen.DA_DEN);
            Audit.log(c, "Khám bệnh", "Kết luận lượt khám", maPk, pk.chanDoan(), pk.maNhanVien());
        });
    }

    /** UC11: lập chỉ định cận lâm sàng. */
    public String chiDinh(String maPk, String maDichVu, String yeuCau) {
        Session.require(Quyen.KHAM);
        String yc = Validator.maxLen(Validator.required(yeuCau, "yeuCau", "Yêu cầu chỉ định"), 300, "yeuCau", "Yêu cầu");
        return Tx.write(c -> {
            PhieuKham pk = dangKham(c, maPk);
            DichVu dv = dvDao.find(c, maDichVu).orElseThrow(() -> new BusinessException("dichVu", "Chọn dịch vụ cận lâm sàng."));
            if (!dv.laCanLamSang() || !dv.dangApDung()) throw new BusinessException("dichVu", dv.tenDichVu() + " không phải dịch vụ cận lâm sàng đang áp dụng.");
            String ma = maDao.next(c, "CD");
            dao.insertChiDinh(c, new ChiDinhCanLamSang(ma, maPk, maDichVu, yc, null, pk.maNhanVien(), Now.now(), null, TrangThaiCLS.CHO_THUC_HIEN));
            Audit.log(c, "Khám bệnh", "Lập chỉ định cận lâm sàng", ma, maPk + " · " + dv.tenDichVu(), pk.maNhanVien());
            return ma;
        });
    }

    public void nhapKetQuaCls(String maChiDinh, String ketQua) {
        Session.require(Quyen.KHAM);
        String kq = Validator.maxLen(Validator.required(ketQua, "ketQua", "Kết quả"), 1000, "ketQua", "Kết quả");
        Tx.run(c -> {
            ChiDinhCanLamSang cd = dao.findChiDinh(c, maChiDinh).orElseThrow(() -> new BusinessException("Không tìm thấy chỉ định."));
            PhieuKham pk = dangKham(c, cd.maPhieuKham());
            if (dao.setKetQuaChiDinh(c, maChiDinh, kq, Now.now()) == 0) throw new BusinessException("Chỉ định đã có kết quả.");
            Audit.log(c, "Khám bệnh", "Nhập kết quả cận lâm sàng", maChiDinh, kq, pk.maNhanVien());
        });
    }

    /**
     * UC16 Kê đơn (một đơn cho một lượt khám; lưu lại thì thay toàn bộ dòng).
     * Cảnh báo cần xác nhận: thuốc trùng nội dung dị ứng trong hồ sơ, thuốc kiểm soát đặc biệt.
     */
    public String keDon(String maPk, DonThuocForm f, boolean daXacNhanCanhBao) {
        Session.require(Quyen.KHAM);
        if (f.dong() == null || f.dong().isEmpty()) throw new BusinessException("dong", "Đơn thuốc cần ít nhất một thuốc.");
        String loiDan = Validator.maxLen(Validator.optional(f.loiDan()), 500, "loiDan", "Lời dặn");
        return Tx.write(c -> {
            PhieuKham pk = dangKham(c, maPk);
            HoSoBenhAn hs = bnDao.hoSoOf(c, pk.maBenhNhan()).orElseThrow();
            List<ChiTietDonThuoc> lines = new ArrayList<>();
            List<String> canhBao = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            String diUng = hs.diUng() == null ? "" : hs.diUng().toLowerCase(Locale.ROOT);
            for (DonThuocForm.Dong d : f.dong()) {
                Thuoc t = hhDao.findThuoc(c, d.maThuoc()).orElseThrow(() -> new BusinessException("dong", "Thuốc " + d.maThuoc() + " không tồn tại."));
                if (!seen.add(t.maThuoc())) throw new BusinessException("dong", t.tenThuoc() + " bị lặp trong đơn; gộp thành một dòng.");
                if (d.soLuong() <= 0 || d.soNgayDung() <= 0) throw new BusinessException("dong", t.tenThuoc() + ": số lượng và số ngày dùng phải lớn hơn 0.");
                String lieu = Validator.required(d.lieuDung(), "dong", "Liều dùng của " + t.tenThuoc());
                String cach = Validator.required(d.cachDung(), "dong", "Cách dùng của " + t.tenThuoc());
                lines.add(new ChiTietDonThuoc(null, t.maThuoc(), d.soLuong(), Validator.maxLen(lieu, 100, "dong", "Liều dùng"),
                        Validator.maxLen(cach, 200, "dong", "Cách dùng"), d.soNgayDung()));
                if (!diUng.isBlank() && (matches(diUng, t.tenThuoc()) || matches(diUng, t.hoatChat()))) {
                    canhBao.add("DỊ ỨNG: hồ sơ ghi \"" + hs.diUng() + "\" — trùng với " + t.tenThuoc() + (t.hoatChat() == null ? "" : " (" + t.hoatChat() + ")"));
                }
                if (t.kiemSoatDacBiet()) canhBao.add("KIỂM SOÁT ĐẶC BIỆT: " + t.tenThuoc() + " — được ghi vào sổ theo dõi thuốc kiểm soát đặc biệt.");
            }
            if (!canhBao.isEmpty() && !daXacNhanCanhBao) throw new CanhBaoException("Kiểm tra cảnh báo trước khi lưu đơn:", canhBao);
            DonThuoc cu = dtDao.byPhieuKham(c, maPk).orElse(null);
            String ma;
            if (cu == null) {
                ma = maDao.next(c, "DT");
                dtDao.insert(c, new DonThuoc(ma, maPk, pk.maBenhNhan(), pk.maNhanVien(), Now.today(), loiDan));
            } else {
                ma = cu.maDonThuoc();
                dtDao.updateHeader(c, ma, loiDan, pk.maNhanVien(), Now.today());
            }
            dtDao.replaceLines(c, ma, lines);
            Audit.log(c, "Khám bệnh", cu == null ? "Kê đơn thuốc" : "Sửa đơn thuốc", ma, maPk + " · " + lines.size() + " thuốc"
                    + (canhBao.isEmpty() ? "" : " · đã xác nhận " + canhBao.size() + " cảnh báo"), pk.maNhanVien());
            return ma;
        });
    }

    private static boolean matches(String diUng, String s) {
        if (s == null || s.isBlank()) return false;
        String k = s.toLowerCase(Locale.ROOT).trim();
        String first = k.split("[\\s(]")[0];
        return diUng.contains(k) || (first.length() >= 4 && diUng.contains(first));
    }

    private PhieuKham load(Connection c, String ma) throws SQLException {
        return dao.find(c, ma).orElseThrow(() -> new BusinessException("Không tìm thấy phiếu khám " + ma + "."));
    }

    /** Phiếu phải đang khám và (với tài khoản bác sĩ) thuộc bác sĩ đăng nhập. */
    private PhieuKham dangKham(Connection c, String ma) throws SQLException {
        PhieuKham pk = load(c, ma);
        if (pk.trangThai() != TrangThaiPhieuKham.DANG_KHAM) throw new BusinessException("Phiếu khám " + ma + " không ở trạng thái đang khám.");
        BacSiPhuTrach.requireOwner(pk.maNhanVien());
        return pk;
    }
}
