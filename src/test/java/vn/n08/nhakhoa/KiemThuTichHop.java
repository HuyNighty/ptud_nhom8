package vn.n08.nhakhoa;

import vn.n08.nhakhoa.config.Database;
import vn.n08.nhakhoa.dao.DieuChinhDao;
import vn.n08.nhakhoa.dao.YeuCauHuyDao;
import vn.n08.nhakhoa.dto.*;
import vn.n08.nhakhoa.model.*;
import vn.n08.nhakhoa.service.*;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.CanhBaoException;
import vn.n08.nhakhoa.util.Jdbc;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.util.Tx;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Kiểm thử tích hợp Service + DAO + JDBC trên CSDL THAY THẾ (Apache Derby nhúng).
 * Không thay thế kiểm thử trên SQL Server thật: các chỉ mục lọc (UX_*) và kiểu NVARCHAR/DATETIME2 chỉ có trên SQL Server.
 * Chạy: xem scripts/test.sh hoặc README.
 */
public final class KiemThuTichHop {
    static final AuthService AUTH = new AuthService();
    static final String MK = "Nhakhoa@123";

    private KiemThuTichHop() {}

    static void dn(String u) { AUTH.logout(); AUTH.login(u, u.equals("admin") ? "Admin@123" : MK); }

    static String str(String sql, Object... p) {
        return Tx.read(c -> Jdbc.one(c, sql, rs -> rs.getString(1), p).orElse(null));
    }

    static long num(String sql, Object... p) {
        return Tx.read(c -> Jdbc.count(c, sql, p));
    }

    public static void main(String[] args) throws Exception {
        TestDb.init("tichhop");
        KiemThuDao.chay();
        dangNhap();
        phanQuyen();
        benhNhan();
        String lhMoi = lichHen();
        String pk = tiepNhanVaKham();
        keHoach();
        hoaDon(pk);
        kho();
        dieuChinh();
        capCuu();
        phanCongBanGiao();
        nghiViec();
        danhGia();
        bangGiaVaCa();
        taiKhoan();
        tongQuan();
        doiSoat();
        System.out.println("(lịch hẹn tạo trong kiểm thử: " + lhMoi + ")");
        System.exit(Kiem.tongKet() == 0 ? 0 : 1);
    }

    static void dangNhap() {
        Kiem.nhom("UC38 Đăng nhập");
        Kiem.chay(() -> dn("admin"), "admin đăng nhập đúng mật khẩu");
        Kiem.dung(Session.user().vaiTro() == VaiTro.AD && Session.user().maNhanVien().equals("NV0000"), "phiên = admin / NV0000");
        AUTH.logout();
        Kiem.biChan(() -> AUTH.login("tranvanan", MK), "tài khoản BI_KHOA không đăng nhập được");
        Kiem.biChan(() -> AUTH.login("dangluc", MK), "tài khoản VO_HIEU không đăng nhập được");
        for (int i = 1; i <= 4; i++) {
            int n = i;
            Kiem.biChan(() -> AUTH.login("nguyenha", "sai" + n), "sai mật khẩu lần " + i);
        }
        Kiem.biChan(() -> AUTH.login("nguyenha", "sai5"), "sai lần 5 → tự khóa");
        Kiem.dung("BI_KHOA".equals(str("SELECT trangThai FROM TaiKhoan WHERE tenDangNhap = 'nguyenha'")), "nguyenha chuyển BI_KHOA");
        Kiem.biChan(() -> AUTH.login("nguyenha", MK), "đúng mật khẩu nhưng đã bị khóa vẫn bị chặn");
        dn("minhtam");
        String ma = str("SELECT maTaiKhoan FROM TaiKhoan WHERE tenDangNhap = 'nguyenha'");
        Kiem.chay(() -> new TaiKhoanService().moKhoa(ma), "QL mở khóa nguyenha");
        Kiem.chay(() -> dn("nguyenha"), "nguyenha đăng nhập lại được");
    }

    static void phanQuyen() {
        Kiem.nhom("Phân quyền ở tầng Service (không chỉ ẩn nút)");
        dn("lethian");
        Kiem.biChan(() -> new TaiKhoanService().danhSach(null, null, null), "Lễ tân gọi TaiKhoanService.danhSach");
        Kiem.biChan(() -> new KhamService().batDauKham("PK000007", null), "Lễ tân gọi KhamService.batDauKham");
        Kiem.biChan(() -> new HoaDonService().xuLyYeuCau("YC000004", true, "ok"), "Lễ tân duyệt yêu cầu hủy");
        Kiem.biChan(() -> new DieuChinhKhoService().xuLy("DC000003", true, null), "Lễ tân duyệt điều chỉnh kho");
        dn("quockhanh");
        Kiem.biChan(() -> new HoaDonService().thuTien("HD000001", new ThanhToanForm(BigDecimal.TEN, PhuongThucThanhToan.TIEN_MAT, null, BigDecimal.TEN)),
                "QL kho thu tiền");
        Kiem.biChan(() -> new DieuChinhKhoService().xuLy("DC000003", true, null), "QL kho (người lập) duyệt phiếu điều chỉnh");
    }

    static String bnMoi;

    static void benhNhan() {
        Kiem.nhom("UC01/08/09/23 Hồ sơ bệnh nhân");
        dn("lethian");
        BenhNhanService s = new BenhNhanService();
        Kiem.biChan(() -> s.tim("09091234567"), "tìm SĐT 11 số bị báo sai định dạng");
        Kiem.dung(s.tim("0909123456").size() == 1, "tìm theo SĐT ra đúng 1 bệnh nhân");
        BenhNhanForm f = new BenhNhanForm("Nguyễn Thị Lan", Now.today().minusDays(10380), GioiTinh.NU, "0901000999", "Q.5", null);
        try {
            s.tao(f, false);
            Kiem.dung(false, "nghi trùng họ tên + ngày sinh phải cảnh báo");
        } catch (CanhBaoException e) {
            Kiem.dung(e.canhBao().size() == 1 && e.canhBao().get(0).contains("BN000001"), "cảnh báo nghi trùng: " + e.canhBao());
        }
        Kiem.biChan(() -> s.tao(new BenhNhanForm("", null, null, "123", null, null), false), "thiếu trường bắt buộc");
        Kiem.chay(() -> bnMoi = s.tao(new BenhNhanForm("Lâm Gia Huy", LocalDate.of(1995, 3, 2), GioiTinh.NAM, "0977 555 123", "Châu Đức", null), false),
                "tạo hồ sơ mới (SĐT có khoảng trắng được chuẩn hóa)");
        Kiem.dung("0977555123".equals(str("SELECT soDienThoai FROM BenhNhan WHERE maBenhNhan = ?", bnMoi)), "SĐT lưu dạng chuẩn");
        Kiem.dung(num("SELECT COUNT(*) FROM HoSoBenhAn WHERE maBenhNhan = ?", bnMoi) == 1, "tạo kèm hồ sơ bệnh án");
        Kiem.biChan(() -> s.capNhatTienSu(bnMoi, "THA", "Penicillin", null), "lễ tân không cập nhật tiền sử/dị ứng");
        dn("admin");
        Kiem.biChan(() -> s.capNhatTienSu(bnMoi, "THA", "Penicillin", null), "admin cập nhật tiền sử mà không chọn bác sĩ");
        Kiem.chay(() -> s.capNhatTienSu(bnMoi, "Tăng huyết áp", "Amoxicillin", "NV0001"), "admin cập nhật tiền sử, chọn BS NV0001");
        Kiem.dung("TK0001".equals(str("SELECT maTaiKhoan FROM NhatKyThaoTac WHERE doiTuong = ? AND hanhDong LIKE 'Cập nhật tiền sử%'", bnMoi))
                && "NV0001".equals(str("SELECT maNguoiChiuTrachNhiem FROM NhatKyThaoTac WHERE doiTuong = ? AND hanhDong LIKE 'Cập nhật tiền sử%'", bnMoi)),
                "nhật ký: tài khoản thật TK0001, người chịu trách nhiệm NV0001");
        Kiem.dung(s.chiTiet("BN000001").luotKham().size() >= 1, "UC09 xem hồ sơ: có lượt khám");
    }

    static String lichHen() {
        Kiem.nhom("UC03/06/07 Lịch hẹn");
        dn("lethian");
        LichHenService s = new LichHenService();
        LocalDateTime t = Now.today().plusDays(1).atTime(13, 30);
        String[] ma = new String[1];
        Kiem.chay(() -> ma[0] = s.tao(new LichHenForm(bnMoi, "NV0001", t, "Khám tổng quát", null)), "tạo lịch hẹn ngày mai 13:30");
        Kiem.biChan(() -> s.tao(new LichHenForm("BN000002", "NV0001", t, null, null)), "trùng khung giờ của bác sĩ");
        Kiem.biChan(() -> s.tao(new LichHenForm("BN000002", "NV0001", Now.today().plusDays(1).atTime(13, 31), null, null)), "giờ không thuộc khung giờ");
        Kiem.biChan(() -> s.tao(new LichHenForm("BN000002", "NV0001", Now.today().minusDays(1).atTime(9, 0), null, null)), "giờ trong quá khứ");
        Kiem.dung(!s.khungGioTrong("NV0001", t.toLocalDate(), null).contains(LocalTime.of(13, 30)), "khung 13:30 không còn trống");
        Kiem.biChan(() -> s.huy(ma[0], " "), "hủy thiếu lý do");
        Kiem.chay(() -> s.xacNhan(ma[0]), "xác nhận lịch");
        String[] moi = new String[1];
        Kiem.chay(() -> moi[0] = s.doiLich(ma[0], "NV0002", t.plusMinutes(30), "Bệnh nhân xin đổi"), "đổi lịch sang BS NV0002 14:00");
        Kiem.dung("DA_DOI_LICH".equals(str("SELECT trangThai FROM LichHen WHERE maLichHen = ?", ma[0]))
                && ma[0].equals(str("SELECT maLichHenGoc FROM LichHen WHERE maLichHen = ?", moi[0])), "lịch cũ DA_DOI_LICH, lịch mới trỏ lịch gốc");
        Kiem.dung(s.khungGioTrong("NV0001", t.toLocalDate(), null).contains(LocalTime.of(13, 30)), "khung cũ được giải phóng");
        Kiem.dung(s.chuoiDoiLich(moi[0]).size() == 2, "chuỗi lịch sử đổi lịch có 2 bản ghi");
        Kiem.chay(() -> s.huy(moi[0], "Bệnh nhân báo bận"), "hủy lịch có lý do");
        Kiem.biChan(() -> s.danhDauVang("LH000014"), "đánh dấu vắng khi chưa tới giờ");
        return moi[0];
    }

    static String tiepNhanVaKham() {
        Kiem.nhom("UC02 Tiếp nhận · UC10/11/16 Khám, CLS, kê đơn");
        dn("lethian");
        TiepNhanService tn = new TiepNhanService();
        String[] pk = new String[1];
        Kiem.chay(() -> pk[0] = tn.checkIn("LH000004", null), "check-in LH000004 (hôm nay) → tạo phiếu khám");
        Kiem.dung("DA_DEN".equals(str("SELECT trangThai FROM LichHen WHERE maLichHen = 'LH000004'")), "lịch hẹn → DA_DEN");
        Kiem.biChan(() -> tn.tiepNhanTrucTiep("BN000004", "NV0001", null), "bệnh nhân đang có phiếu mở không tiếp nhận thêm");
        Kiem.biChan(() -> tn.checkIn("LH000011", null), "check-in lịch của ngày mai");
        dn("tranminh");
        KhamService k = new KhamService();
        Kiem.biChan(() -> k.batDauKham(pk[0], null), "BS NV0001 không khám phiếu của BS NV0003");
        dn("admin");
        Kiem.biChan(() -> k.batDauKham(pk[0], null), "admin bắt đầu khám mà không chọn bác sĩ");
        Kiem.biChan(() -> k.batDauKham(pk[0], "NV0008"), "chọn người không phải bác sĩ");
        Kiem.chay(() -> k.batDauKham(pk[0], "NV0003"), "admin bắt đầu khám, BS chịu trách nhiệm NV0003");
        String[] cd = new String[1];
        Kiem.chay(() -> cd[0] = k.chiDinh(pk[0], "DV02", "Chụp phim răng 38"), "lập chỉ định CLS (DV02)");
        Kiem.chay(() -> k.luuKetQua(pk[0], "Đau nhẹ", "Lành thương sau nhổ 38", "Cắt chỉ, hẹn kiểm tra"), "ghi kết quả khám");
        Kiem.biChan(() -> k.ketLuan(pk[0]), "kết luận khi CLS chưa có kết quả");
        Kiem.chay(() -> k.nhapKetQuaCls(cd[0], "Ổ răng lành tốt"), "nhập kết quả CLS");
        DonThuocForm don = new DonThuocForm("Uống sau ăn", List.of(new DonThuocForm.Dong("T004", 10, "1 viên", "Uống khi đau", 5)));
        try {
            k.keDon(pk[0], don, false);
            Kiem.dung(false, "kê thuốc KSĐB phải cảnh báo");
        } catch (CanhBaoException e) {
            Kiem.dung(e.canhBao().stream().anyMatch(x -> x.startsWith("KIỂM SOÁT")), "cảnh báo thuốc kiểm soát đặc biệt");
        }
        Kiem.chay(() -> k.keDon(pk[0], don, true), "lưu đơn sau khi xác nhận cảnh báo");
        Kiem.chay(() -> k.ketLuan(pk[0]), "kết luận lượt khám");
        Kiem.dung("HOAN_THANH".equals(str("SELECT trangThai FROM LichHen WHERE maLichHen = 'LH000004'")), "lịch hẹn → HOAN_THANH");
        Kiem.dung("NV0003".equals(str("SELECT maNguoiChiuTrachNhiem FROM NhatKyThaoTac WHERE doiTuong = ? AND hanhDong = 'Kết luận lượt khám'", pk[0]))
                && "TK0001".equals(str("SELECT maTaiKhoan FROM NhatKyThaoTac WHERE doiTuong = ? AND hanhDong = 'Kết luận lượt khám'", pk[0])),
                "nhật ký kết luận: TK0001 thao tác, NV0003 chịu trách nhiệm");
        // dị ứng
        dn("lethian");
        String[] pk2 = new String[1];
        Kiem.chay(() -> pk2[0] = tn.tiepNhanTrucTiep(bnMoi, "NV0001", "Đau răng"), "tiếp nhận trực tiếp bệnh nhân mới");
        dn("tranminh");
        Kiem.chay(() -> k.batDauKham(pk2[0], null), "BS NV0001 tự bắt đầu khám phiếu của mình");
        try {
            k.keDon(pk2[0], new DonThuocForm(null, List.of(new DonThuocForm.Dong("T002", 10, "1 viên", "Sau ăn", 5))), false);
            Kiem.dung(true, "thuốc không liên quan dị ứng: lưu không cảnh báo");
        } catch (CanhBaoException e) {
            Kiem.dung(false, "cảnh báo không mong đợi " + e.canhBao());
        }
        String t2 = str("SELECT maThuoc FROM Thuoc WHERE LOWER(tenThuoc) LIKE '%amoxicillin%' OR LOWER(hoatChat) LIKE '%amoxicillin%'");
        if (t2 != null) {
            try {
                k.keDon(pk2[0], new DonThuocForm(null, List.of(new DonThuocForm.Dong(t2, 10, "1 viên", "Sau ăn", 5))), false);
                Kiem.dung(false, "thuốc trùng dị ứng phải cảnh báo");
            } catch (CanhBaoException e) {
                Kiem.dung(e.canhBao().get(0).startsWith("DỊ ỨNG"), "cảnh báo dị ứng Amoxicillin");
            }
        }
        Kiem.chay(() -> k.luuKetQua(pk2[0], "Đau răng", "Sâu răng 36", "Trám"), "BS lưu kết quả");
        Kiem.chay(() -> k.ketLuan(pk2[0]), "BS kết luận");
        return pk[0];
    }

    static void keHoach() {
        Kiem.nhom("UC12/13/14/15 Kế hoạch điều trị & tái khám");
        dn("tranminh");
        KeHoachService s = new KeHoachService();
        String[] kh = new String[1];
        LocalDate h = Now.today();
        Kiem.biChan(() -> s.lap(bnMoi, null, "Trám răng", List.of(), null), "kế hoạch không có hạng mục");
        Kiem.chay(() -> kh[0] = s.lap(bnMoi, null, "Phục hồi răng 36, 37",
                List.of(new HangMucForm("DV03", "36", h.plusDays(3)), new HangMucForm("DV03", "37", h.plusDays(10))), null), "lập kế hoạch 2 hạng mục");
        KeHoachChiTiet ct = s.chiTiet(kh[0]);
        Kiem.dung(ct.hangMuc().size() == 2 && ct.keHoach().chiPhiDuKien().signum() > 0, "chi phí dự kiến tính từ hạng mục: " + ct.keHoach().chiPhiDuKien());
        int m1 = ct.hangMuc().get(0).ct().maChiTiet();
        int m2 = ct.hangMuc().get(1).ct().maChiTiet();
        Kiem.dung(s.danhSachTaiKham().stream().anyMatch(x -> x.maKeHoach().equals(kh[0])), "UC14: hạng mục đến hạn xuất hiện trong danh sách tái khám");
        Kiem.chay(() -> s.ghiKetQua(m1, h, "Trám composite tốt", null), "ghi kết quả hạng mục 1");
        Kiem.biChan(() -> s.hoanThanh(kh[0]), "hoàn thành khi còn hạng mục chưa làm");
        Kiem.chay(() -> s.doiNgayDuKien(m2, h.plusDays(20)), "đổi ngày dự kiến hạng mục 2");
        Kiem.chay(() -> s.huyHangMuc(m2, "Bệnh nhân chưa sắp xếp được"), "hủy hạng mục 2");
        Kiem.chay(() -> s.hoanThanh(kh[0]), "hoàn thành kế hoạch");
        dn("phamanh");
        Kiem.biChan(() -> s.huyKeHoach("KH000005", "Thử"), "BS khác không sửa kế hoạch của BS khác");
        dn("lethian");
        LichHenService lh = new LichHenService();
        TaiKhamItem it = new KeHoachService().danhSachTaiKham().stream().filter(x -> x.maLichHenMo() == null).findFirst().orElse(null);
        if (it != null) {
            LocalDate ngay = h.plusDays(2);
            LocalTime gio = lh.khungGioTrong(it.maBacSi(), ngay, null).get(0);
            Kiem.chay(() -> lh.tao(new LichHenForm(it.maBenhNhan(), it.maBacSi(), ngay.atTime(gio), "Tái khám", it.maKeHoach())),
                    "lễ tân tạo lịch tái khám từ danh sách đến hạn (" + it.maKeHoach() + ")");
        }
    }

    static void hoaDon(String pk) {
        Kiem.nhom("UC04/05 Thanh toán, công nợ, yêu cầu hủy — không tự duyệt");
        dn("lethian");
        HoaDonService s = new HoaDonService();
        Kiem.dung(s.phieuKhamChoLapHoaDon().stream().anyMatch(v -> v.pk().maPhieuKham().equals(pk)), "lượt khám vừa kết luận chờ lập hóa đơn");
        List<DongHoaDonForm> dong = new java.util.ArrayList<>(s.goiYDong(pk));
        dong.add(new DongHoaDonForm(DongHoaDonForm.Loai.DICH_VU, "DV01", 1, null));
        String[] hd = new String[1];
        Kiem.chay(() -> hd[0] = s.lap("BN000004", pk, dong), "lập hóa đơn từ lượt khám");
        Kiem.biChan(() -> s.lap("BN000004", pk, dong), "lượt khám đã có hóa đơn");
        BigDecimal tong = s.chiTiet(hd[0]).hoaDon().tongTien();
        Kiem.biChan(() -> s.thuTien(hd[0], new ThanhToanForm(new BigDecimal("100000"), PhuongThucThanhToan.CHUYEN_KHOAN, " ", null)), "CK thiếu mã giao dịch");
        Kiem.biChan(() -> s.thuTien(hd[0], new ThanhToanForm(tong.add(BigDecimal.ONE), PhuongThucThanhToan.THE_POS, "POS1", null)), "thu vượt số còn nợ");
        Kiem.biChan(() -> s.thuTien(hd[0], new ThanhToanForm(new BigDecimal("100000"), PhuongThucThanhToan.TIEN_MAT, null, new BigDecimal("50000"))),
                "tiền khách đưa nhỏ hơn số thu");
        String[] t1 = new String[1];
        Kiem.chay(() -> t1[0] = s.thuTien(hd[0], new ThanhToanForm(new BigDecimal("100000"), PhuongThucThanhToan.TIEN_MAT, null, new BigDecimal("200000"))),
                "lần 1: tiền mặt 100.000, khách đưa 200.000");
        Kiem.dung(s.chiTiet(hd[0]).thanhToan().get(0).tt().tienThoi().compareTo(new BigDecimal("100000")) == 0, "tiền thối = 100.000 (tính, không lưu)");
        Kiem.dung(s.chiTiet(hd[0]).hoaDon().hd().trangThai() == TrangThaiHoaDon.THANH_TOAN_MOT_PHAN, "hóa đơn → thanh toán một phần");
        BigDecimal con = tong.subtract(new BigDecimal("100000"));
        Kiem.chay(() -> s.thuTien(hd[0], new ThanhToanForm(con, PhuongThucThanhToan.CHUYEN_KHOAN, "VCB123", null)), "lần 2: chuyển khoản phần còn lại");
        Kiem.dung(s.chiTiet(hd[0]).hoaDon().hd().trangThai() == TrangThaiHoaDon.DA_THANH_TOAN, "hóa đơn → đã thanh toán, 2 phương thức khác nhau");
        Kiem.biChan(() -> s.thuTien("HD000001", new ThanhToanForm(BigDecimal.ONE, PhuongThucThanhToan.CHUYEN_KHOAN, "VCB123", null)), "trùng mã giao dịch CK");
        Kiem.biChan(() -> s.huyGiaoDich(t1[0], "Nhập sai"), "lễ tân không hủy giao dịch");
        dn("minhtam");
        Kiem.chay(() -> s.huyGiaoDich(t1[0], "Nhập sai số tiền"), "QL hủy lần thu tiền mặt");
        Kiem.dung(s.chiTiet(hd[0]).hoaDon().hd().trangThai() == TrangThaiHoaDon.THANH_TOAN_MOT_PHAN && s.chiTiet(hd[0]).thanhToan().size() == 2,
                "lần thu hủy vẫn giữ trong lịch sử; trạng thái tính lại");
        Kiem.dung(!s.congNo().isEmpty(), "UC05 công nợ theo bệnh nhân có dữ liệu");
        // Không tự duyệt
        dn("admin");
        Kiem.biChan(() -> s.xuLyYeuCau("YC000004", true, "Tự duyệt"), "admin duyệt yêu cầu do chính admin tạo");
        Kiem.biChan(() -> s.xuLyYeuCau("YC000004", false, "Tự từ chối"), "admin từ chối yêu cầu do chính admin tạo");
        int n = Tx.write(c -> new YeuCauHuyDao().decide(c, "YC000004", TrangThaiDuyet.DA_DUYET, "TK0001", Now.now(), "gọi DAO trực tiếp"));
        Kiem.dung(n == 0, "gọi thẳng DAO.decide với cùng tài khoản: 0 dòng được cập nhật");
        Kiem.dung(rawFails("UPDATE YeuCauHuyHoaDon SET trangThai = 'DA_DUYET', maTaiKhoanDuyet = 'TK0001', lyDoDuyet = 'x' WHERE maYeuCau = 'YC000004'"),
                "UPDATE thẳng vào CSDL bị CHECK CK_YCHuy_KhongTuDuyet chặn");
        Kiem.chay(() -> s.xuLyYeuCau("YC000005", false, "Lập lại đúng gói, không cần hủy"), "admin từ chối yêu cầu do lethian tạo");
        dn("minhtam");
        Kiem.chay(() -> s.xuLyYeuCau("YC000004", true, "Đồng ý, hóa đơn nhập trùng"), "QL (tài khoản khác) duyệt yêu cầu của admin");
        Kiem.dung("DA_HUY".equals(str("SELECT trangThai FROM HoaDon WHERE maHoaDon = 'HD000006'")), "hóa đơn HD000006 → DA_HUY");
        dn("lethian");
        String[] yc = new String[1];
        Kiem.chay(() -> yc[0] = s.guiYeuCauHuy(hd[0], "Bệnh nhân khiếu nại"), "lễ tân gửi yêu cầu hủy hóa đơn đã thu một phần");
        Kiem.biChan(() -> s.guiYeuCauHuy(hd[0], "Lần 2"), "trùng yêu cầu đang chờ");
        Kiem.biChan(() -> s.thuTien(hd[0], new ThanhToanForm(BigDecimal.ONE, PhuongThucThanhToan.TIEN_MAT, null, BigDecimal.ONE)), "không thu khi đang chờ duyệt hủy");
        dn("minhtam");
        Kiem.biChan(() -> s.xuLyYeuCau(yc[0], true, "Duyệt"), "không duyệt hủy khi còn tiền đã thu");
        Kiem.chay(() -> s.xuLyYeuCau(yc[0], false, "Đã giải thích, giữ hóa đơn"), "QL từ chối có lý do");
    }

    static boolean rawFails(String sql) {
        try (Connection c = Database.open(); Statement st = c.createStatement()) {
            st.executeUpdate(sql);
            return false;
        } catch (SQLException e) {
            System.out.println("      (CSDL từ chối: " + e.getMessage() + ")");
            return true;
        }
    }

    static void kho() {
        Kiem.nhom("UC27/29–33/36/37 Kho");
        dn("quockhanh");
        KhoService s = new KhoService();
        LocalDate h = Now.today();
        Kiem.dung(s.tonKho("lidocain").size() + s.tonKho("").size() > 0, "UC27 tìm kiếm thuốc/vật tư");
        String[] t = new String[1];
        Kiem.chay(() -> t[0] = s.luuThuoc(new Thuoc(null, "Ibuprofen", "Viên", false, "400mg", "Ibuprofen", 50, false)), "UC29 thêm thuốc");
        Kiem.biChan(() -> s.luuThuoc(new Thuoc(null, "ibuprofen", "Viên", false, null, null, 0, false)), "trùng tên thuốc");
        Kiem.biChan(() -> s.nhapKho(new NhapKhoForm("NCC01", null, List.of(new NhapKhoForm.Dong(true, t[0], "IB01", null, h.minusDays(1), 10, BigDecimal.TEN)))),
                "nhập lô đã hết hạn");
        Kiem.chay(() -> s.nhapKho(new NhapKhoForm("NCC01", "Đơn mua tháng", List.of(
                new NhapKhoForm.Dong(true, t[0], "IB01", h.minusDays(30), h.plusDays(20), 30, new BigDecimal("1500")),
                new NhapKhoForm.Dong(true, t[0], "IB02", h.minusDays(10), h.plusDays(400), 50, new BigDecimal("1500"))))), "UC30 nhập 2 lô");
        List<XuatKhoForm.Dong> fefo = s.goiYFefo(true, t[0], 40);
        Kiem.dung(fefo.size() == 2 && fefo.get(0).soLuong() == 30, "UC31 FEFO: lấy hết lô hạn gần (30) rồi lô sau (10)");
        Kiem.biChan(() -> s.goiYFefo(true, t[0], 1000), "FEFO thiếu tồn");
        Kiem.biChan(() -> s.xuatKho(new XuatKhoForm("Phòng khám 1", null, null, null, List.of(new XuatKhoForm.Dong("LO000009", 1)))), "xuất lô đã hết hạn");
        Kiem.biChan(() -> s.xuatKho(new XuatKhoForm("Phòng khám 1", null, null, null, List.of(new XuatKhoForm.Dong("LO000011", 9999)))), "xuất vượt tồn");
        String[] px = new String[1];
        Kiem.chay(() -> px[0] = s.xuatKho(new XuatKhoForm("Phòng khám 1", "Y tá Hạnh", "Dùng cho ca sáng", null, fefo)), "xuất theo FEFO");
        Kiem.biChan(() -> s.xuatKho(new XuatKhoForm("Phòng khám 1", null, null, "NV0001", List.of(new XuatKhoForm.Dong("LO000005", 2)))),
                "UC33 xuất thuốc KSĐB thiếu người nhận");
        Kiem.biChan(() -> s.xuatKho(new XuatKhoForm("Phòng khám 1", "Y tá Hạnh", null, null, List.of(new XuatKhoForm.Dong("LO000005", 2)))),
                "UC33 xuất thuốc KSĐB thiếu bác sĩ chịu trách nhiệm");
        Kiem.chay(() -> s.xuatKho(new XuatKhoForm("Phòng khám 1", "Y tá Hạnh", "Ca thủ thuật", "NV0001", List.of(new XuatKhoForm.Dong("LO000005", 2)))),
                "xuất thuốc KSĐB đủ người nhận + BS chịu trách nhiệm");
        Kiem.dung(s.soKiemSoatDacBiet(h.minusDays(400), h).stream().anyMatch(r -> r.loai().equals("Xuất") && r.ghiChu().contains("BS. Trần Minh")),
                "sổ KSĐB ghi dòng xuất kèm BS chịu trách nhiệm");
        Kiem.biChan(() -> s.hoanTra(px[0], List.of(new XuatKhoForm.Dong(fefo.get(1).maLo(), 5)), false, "Thừa"), "UC32 hoàn trả chưa xác nhận nguyên seal");
        Kiem.biChan(() -> s.hoanTra(px[0], List.of(new XuatKhoForm.Dong(fefo.get(1).maLo(), 11)), true, "Thừa"), "hoàn trả vượt số đã xuất");
        Kiem.chay(() -> s.hoanTra(px[0], List.of(new XuatKhoForm.Dong(fefo.get(1).maLo(), 5)), true, "Dùng không hết"), "hoàn trả 5 nguyên seal");
        Kiem.biChan(() -> s.hoanTra(px[0], List.of(new XuatKhoForm.Dong(fefo.get(1).maLo(), 6)), true, "Lần 2"), "hoàn trả lần 2 vượt phần còn lại (5)");
        Kiem.dung(s.canhBaoHanDung().stream().anyMatch(l -> l.lo().maLo().equals("LO000009")), "UC37 cảnh báo có lô đã hết hạn LO000009");
        Kiem.chay(() -> s.danhDauDaDatHang(true, "T005", true), "UC36 đánh dấu đã đặt hàng");
    }

    static void dieuChinh() {
        Kiem.nhom("UC28/35 Kiểm kê, hủy/hao hụt — người duyệt khác người lập");
        dn("quockhanh");
        DieuChinhKhoService s = new DieuChinhKhoService();
        Kiem.biChan(() -> s.lap(LoaiDieuChinh.HU_HONG, "Vỡ", List.of(new XuatKhoForm.Dong("LO000010", 99))), "phiếu hư hỏng không được tăng tồn");
        Kiem.biChan(() -> s.lap(LoaiDieuChinh.HET_HAN, "Hết hạn", List.of(new XuatKhoForm.Dong("LO000010", 0))), "loại hết hạn cho lô chưa hết hạn");
        String[] dc = new String[1];
        Kiem.chay(() -> dc[0] = s.lap(LoaiDieuChinh.HET_HAN, "Tiêu hủy lô hết hạn", List.of(new XuatKhoForm.Dong("LO000009", 0))), "lập phiếu hủy lô hết hạn");
        Kiem.biChan(() -> s.lap(LoaiDieuChinh.KIEM_KE, "Kiểm kê", List.of(new XuatKhoForm.Dong("LO000009", 1))), "lô đang nằm trong phiếu chờ duyệt khác");
        dn("admin");
        String[] dcAdmin = new String[1];
        Kiem.chay(() -> dcAdmin[0] = s.lap(LoaiDieuChinh.KIEM_KE, "Kiểm kê nhanh", List.of(new XuatKhoForm.Dong("LO000011", 7))), "admin lập phiếu kiểm kê");
        Kiem.biChan(() -> s.xuLy(dcAdmin[0], true, null), "admin duyệt phiếu do chính admin lập");
        int n = Tx.write(c -> new DieuChinhDao().decide(c, dcAdmin[0], TrangThaiDuyet.DA_DUYET, "TK0001", Now.now(), null));
        Kiem.dung(n == 0, "gọi thẳng DAO.decide cùng tài khoản: 0 dòng");
        Kiem.dung(rawFails("UPDATE PhieuDieuChinhKho SET trangThai = 'DA_DUYET', maTaiKhoanDuyet = 'TK0001' WHERE maPhieu = '" + dcAdmin[0] + "'"),
                "UPDATE thẳng bị CHECK CK_PDC_KhongTuDuyet chặn");
        Kiem.chay(() -> s.xuLy(dc[0], true, "Đã tiêu hủy có biên bản"), "admin duyệt phiếu của quockhanh");
        Kiem.dung(num("SELECT soLuongTon FROM LoHang WHERE maLo = 'LO000009'") == 0, "tồn LO000009 → 0 sau duyệt");
        dn("minhtam");
        Kiem.biChan(() -> s.xuLy(dcAdmin[0], false, null), "từ chối không có lý do");
        Kiem.chay(() -> s.xuLy(dcAdmin[0], true, "Khớp biên bản kiểm kê"), "QL duyệt phiếu của admin");
    }

    static void capCuu() {
        Kiem.nhom("UC34 Hộp cấp cứu (danh mục mẫu, cấu hình được)");
        dn("quockhanh");
        CapCuuService s = new CapCuuService();
        List<CapCuuView> dm = s.danhMuc();
        Kiem.dung(dm.stream().allMatch(v -> v.muc().laDuLieuMau()), "6 mục cài sẵn được đánh dấu dữ liệu mẫu");
        Kiem.biChan(() -> s.kiemTra(new KiemTraCapCuuForm(null, List.of())), "kiểm tra thiếu mục");
        List<KiemTraCapCuuForm.Dong> d = dm.stream().filter(v -> v.muc().dangApDung())
                .map(v -> new KiemTraCapCuuForm.Dong(v.muc().maMuc(), v.muc().soLuongToiThieu(), Now.today().plusDays(200))).toList();
        String[] kt = new String[1];
        Kiem.chay(() -> kt[0] = s.kiemTra(new KiemTraCapCuuForm("Định kỳ", d)), "kiểm tra đủ mọi mục");
        Kiem.dung("DAT".equals(str("SELECT ketQua FROM KiemTraCapCuu WHERE maKiemTra = ?", kt[0])), "kết quả DAT khi đủ số lượng và hạn > ngưỡng");
        String[] ma = new String[1];
        Kiem.chay(() -> ma[0] = s.luuMuc(new DanhMucCapCuu(null, "Găng tay vô khuẩn", "Đôi", 4, 0, null, null, "V001", true, false, null)), "thêm mục cấu hình");
        Kiem.chay(() -> s.boSungTuKho(ma[0], "LO000007", 4), "bổ sung từ kho (xuất kho bộ phận Hộp cấp cứu)");
        Kiem.dung(num("SELECT soLuongHienCo FROM DanhMucCapCuu WHERE maMuc = ?", ma[0]) == 4, "số hiện có của mục = 4");
        Kiem.chay(() -> s.luuCauHinh(45, 7), "đổi ngưỡng hạn dùng 45 ngày");
    }

    static void phanCongBanGiao() {
        Kiem.nhom("UC17/18/20/24 Phân công · UC19 Bàn giao");
        dn("minhtam");
        PhanCongService s = new PhanCongService();
        LocalDate d = Now.today().plusDays(8);
        String[] pc = new String[3];
        Kiem.chay(() -> pc[0] = s.them("NV0005", "CA1", d, "Phụ tá ghế 1"), "phân ca 1");
        Kiem.biChan(() -> s.them("NV0005", "CA1", d, "Trùng"), "trùng ca");
        Kiem.chay(() -> pc[1] = s.them("NV0005", "CA2", d, "Phụ tá ghế 2"), "phân ca 2 cùng ngày");
        Kiem.biChan(() -> s.them("NV0005", "CA3", d, "Ca 3"), "vượt tối đa 2 ca/ngày");
        Kiem.biChan(() -> s.them("NV0005", "CA3", Now.today().minusDays(1), "Quá khứ"), "phân công ngày đã qua");
        Kiem.biChan(() -> s.thayNguoi(pc[0], "NV0001", "Bận"), "thay bằng người khác chức danh");
        Kiem.chay(() -> pc[2] = s.thayNguoi(pc[0], "NV0006", "Đi tập huấn"), "thay người (hủy bản cũ + tạo bản mới)");
        Kiem.dung(pc[0].equals(str("SELECT maPhanCongGoc FROM PhanCong WHERE maPhanCong = ?", pc[2])) && "DA_HUY".equals(str("SELECT trangThai FROM PhanCong WHERE maPhanCong = ?", pc[0])),
                "bản mới trỏ bản gốc, bản cũ DA_HUY có lý do");
        Kiem.biChan(() -> s.batDau(pc[1]), "bắt đầu nhiệm vụ không đúng ngày");
        int[] r = s.saoChepTuanTruoc(Now.today().plusDays(7).with(java.time.DayOfWeek.MONDAY));
        Kiem.dung(r[0] + r[1] >= 0, "sao chép tuần trước: tạo " + r[0] + ", bỏ qua " + r[1]);
        dn("vophuc");
        String today = str("SELECT maPhanCong FROM PhanCong WHERE maNhanVien = 'NV0006' AND ngayLam = ? AND trangThai = 'CHO_HIEU_LUC'", Now.today());
        if (today != null) Kiem.chay(() -> s.batDau(today), "y tá tự bắt đầu nhiệm vụ hôm nay");
        Kiem.biChan(() -> s.suaNhiemVu(pc[1], "x"), "y tá không sửa nhiệm vụ (chỉ QL)");

        BanGiaoService b = new BanGiaoService();
        dn("admin");
        String[] bg = new String[1];
        Kiem.chay(() -> bg[0] = b.tao(new BanGiaoForm(Now.today(), "NV0005", "CA1", "NV0006", "CA2", "Giao 2 bộ dụng cụ", true)), "admin lập biên bản có chênh lệch");
        Kiem.biChan(() -> b.xacNhan(bg[0], "ok"), "admin tự xác nhận biên bản mình lập");
        Kiem.dung(rawFails("UPDATE BanGiaoCa SET trangThai = 'DA_BAN_GIAO', maTaiKhoanXacNhan = maTaiKhoanTao, thoiGianXacNhan = CURRENT_TIMESTAMP WHERE maBanGiao = '" + bg[0] + "'"),
                "UPDATE thẳng bị CHECK CK_BanGiao_KhongTuXacNhan chặn");
        dn("vophuc");
        Kiem.biChan(() -> b.xacNhan(bg[0], null), "có chênh lệch mà không ghi chú");
        Kiem.chay(() -> b.xacNhan(bg[0], "Đã kiểm lại, thiếu 1 bộ đã báo QL"), "người nhận xác nhận");
        dn("dohanh");
        Kiem.biChan(() -> b.tao(new BanGiaoForm(Now.today(), "NV0007", "CA1", "NV0006", "CA2", "x", false)), "y tá lập biên bản thay người khác");
    }

    static void nghiViec() {
        Kiem.nhom("Thủ tục nghỉ việc có điều kiện (không tự hủy lịch sử)");
        dn("minhtam");
        NhanVienService s = new NhanVienService();
        NghiViecCheck ck = s.kiemTraNghiViec("NV0004");
        Kiem.dung(!ck.duDieuKien(), "BS Lê Thu còn " + ck.phanCong().size() + " phân công, " + ck.lichHen().size() + " lịch hẹn, "
                + ck.phieuKham().size() + " phiếu khám, " + ck.banGiao().size() + " bàn giao");
        Kiem.biChan(() -> s.hoanTatNghiViec("NV0004", Now.today(), "Chuyển công tác"), "hoàn tất khi chưa xử lý xong");
        long lsTruoc = num("SELECT COUNT(*) FROM PhanCong WHERE maNhanVien = 'NV0004'");
        PhanCongService pc = new PhanCongService();
        LichHenService lh = new LichHenService();
        Kiem.chay(() -> {
            for (PhanCongView v : ck.phanCong()) {
                if (v.pc().trangThai() == TrangThaiPhanCong.CHO_HIEU_LUC && !v.pc().ngayLam().isBefore(Now.today())) {
                    try {
                        pc.thayNguoi(v.pc().maPhanCong(), "NV0001", "Nghỉ việc");
                        continue;
                    } catch (BusinessException ignore) { /* người thay đã kín lịch: hủy có lý do */ }
                }
                pc.huy(v.pc().maPhanCong(), "Nhân viên nghỉ việc");
            }
            for (LichHenView v : ck.lichHen()) {
                LocalDate ngay = v.lh().thoiGianHen().toLocalDate();
                List<LocalTime> trong = lh.khungGioTrong("NV0002", ngay, null);
                if (trong.contains(v.lh().thoiGianHen().toLocalTime())) lh.doiLich(v.lh().maLichHen(), "NV0002", v.lh().thoiGianHen(), "BS nghỉ việc, chuyển BS");
                else lh.huy(v.lh().maLichHen(), "BS nghỉ việc, đã gọi báo bệnh nhân");
            }
        }, "xử lý từng phân công (thay người/hủy) và lịch hẹn (chuyển BS/hủy) có lý do");
        Kiem.dung(s.kiemTraNghiViec("NV0004").duDieuKien(), "đủ điều kiện sau khi xử lý");
        Kiem.chay(() -> s.hoanTatNghiViec("NV0004", Now.today(), "Chuyển công tác"), "hoàn tất nghỉ việc");
        Kiem.dung("VO_HIEU".equals(str("SELECT trangThai FROM TaiKhoan WHERE maNhanVien = 'NV0004'")), "tài khoản → VO_HIEU");
        Kiem.dung(num("SELECT COUNT(*) FROM PhanCong WHERE maNhanVien = 'NV0004'") == lsTruoc, "không xóa phân công lịch sử");
        Kiem.dung(num("SELECT COUNT(*) FROM NhanVien WHERE maNhanVien = 'NV0004'") == 1, "không xóa nhân viên");
        Kiem.biChan(() -> AUTH.login("lethu", MK), "nhân viên đã nghỉ không đăng nhập được");
        dn("minhtam");
        Kiem.biChan(() -> s.hoanTatNghiViec("NV0011", Now.today(), "x"), "không tự làm thủ tục nghỉ việc cho chính mình");
    }

    static void danhGia() {
        Kiem.nhom("UC25 Đánh giá nhân sự");
        dn("minhtam");
        DanhGiaService s = new DanhGiaService();
        String ky = DanhGiaService.ky(Now.today());
        Kiem.biChan(() -> s.luu("NV0005", ky, List.of(5, 5, 5, 5, 6), null, false), "điểm ngoài 1–5");
        Kiem.biChan(() -> s.luu("NV0011", ky, List.of(5, 5, 5, 5, 5), null, false), "tự đánh giá");
        Kiem.chay(() -> s.luu("NV0005", ky, List.of(5, 4, 5, 4, 5), "Tốt", false), "lưu nháp");
        Kiem.chay(() -> s.luu("NV0005", ky, List.of(5, 5, 5, 4, 5), "Rất tốt", true), "gửi đánh giá");
        Kiem.dung(new BigDecimal("4.80").compareTo(Tx.read(c -> Jdbc.sum(c, "SELECT diem FROM DanhGiaNhanSu WHERE maNhanVien = 'NV0005' AND kyDanhGia = ?", ky))) == 0,
                "điểm trung bình = 4.80");
        Kiem.biChan(() -> s.luu("NV0005", ky, List.of(1, 1, 1, 1, 1), null, false), "sửa đánh giá đã gửi");
    }

    static void bangGiaVaCa() {
        Kiem.nhom("Class diagram: DichVu.capNhatDonGia · CaLamViec.capNhatCa");
        dn("lethian");
        DichVuService dv = new DichVuService();
        Kiem.dung(!dv.danhSach().isEmpty(), "lễ tân xem bảng giá");
        Kiem.biChan(() -> dv.luu(new DichVu("DV01", "Khám tổng quát", new BigDecimal("250000"), null, false, true)), "lễ tân đổi giá");
        BigDecimal truoc = Tx.read(c -> Jdbc.sum(c, "SELECT SUM(soLuong * donGiaApDung) FROM ChiTietHoaDon WHERE maHoaDon = 'HD000001'"));
        dn("minhtam");
        Kiem.chay(() -> dv.luu(new DichVu("DV01", "Khám tổng quát", new BigDecimal("250000"), null, false, true)), "QL cập nhật đơn giá DV01");
        Kiem.dung(truoc.compareTo(Tx.read(c -> Jdbc.sum(c, "SELECT SUM(soLuong * donGiaApDung) FROM ChiTietHoaDon WHERE maHoaDon = 'HD000001'"))) == 0,
                "hóa đơn đã lập giữ đơn giá cũ (donGiaApDung)");
        Kiem.biChan(() -> dv.luu(new DichVu(null, "khám tổng quát", BigDecimal.ONE, null, false, true)), "trùng tên dịch vụ");
        PhanCongService pc = new PhanCongService();
        CaLamViec ca = pc.danhSachCa().get(0);
        Kiem.biChan(() -> pc.capNhatCa(new CaLamViec(ca.maCa(), ca.tenCa(), LocalTime.of(12, 0), LocalTime.of(8, 0), 2, 2, 1)), "giờ kết thúc trước giờ bắt đầu");
        Kiem.chay(() -> pc.capNhatCa(new CaLamViec(ca.maCa(), ca.tenCa(), ca.gioBatDau(), ca.gioKetThuc(), 3, 2, 1)), "QL đổi định mức bác sĩ ca sáng = 3");
        Kiem.dung(pc.danhSachCa().get(0).soBacSi() == 3, "định mức đã lưu");
    }

    static void taiKhoan() {
        Kiem.nhom("UC21/22/26 Tài khoản & nhân viên");
        dn("minhtam");
        NhanVienService nv = new NhanVienService();
        TaiKhoanService tk = new TaiKhoanService();
        String[] ma = new String[2];
        Kiem.biChan(() -> nv.them(new NhanVienForm("Ngô Văn B", "0903111201", VaiTro.LT, null, GioiTinh.NAM, null, null, null)), "SĐT trùng nhân viên khác");
        Kiem.chay(() -> ma[0] = nv.them(new NhanVienForm("Ngô Văn B", "0903999888", VaiTro.LT, LocalDate.of(2000, 1, 1), GioiTinh.NAM, "b@x.vn", null, null)), "thêm nhân viên");
        Kiem.biChan(() -> tk.tao(ma[0], "admin", VaiTro.LT, "Matkhau123"), "tên đăng nhập trùng");
        Kiem.biChan(() -> tk.tao(ma[0], "ngovanb", VaiTro.LT, "123"), "mật khẩu yếu");
        Kiem.biChan(() -> tk.tao(ma[0], "ngovanb", VaiTro.AD, "Matkhau123"), "QL cấp vai trò quản trị");
        Kiem.chay(() -> ma[1] = tk.tao(ma[0], "ngovanb", VaiTro.LT, "Matkhau123"), "cấp tài khoản");
        Kiem.chay(() -> tk.khoa(ma[1], "Nghỉ phép dài"), "khóa (BI_KHOA)");
        Kiem.chay(() -> tk.moKhoa(ma[1]), "mở khóa");
        Kiem.chay(() -> tk.doiVaiTro(ma[1], VaiTro.YT), "đổi vai trò");
        Kiem.biChan(() -> tk.khoa("TK0012", "x"), "tự khóa tài khoản đang đăng nhập");
        Kiem.biChan(() -> tk.voHieu("TK0001", "x"), "QL vô hiệu tài khoản quản trị");
        Kiem.chay(() -> tk.voHieu(ma[1], "Không còn sử dụng"), "vô hiệu (VO_HIEU)");
        Kiem.biChan(() -> tk.moKhoa(ma[1]), "không mở lại tài khoản VO_HIEU");
        Kiem.chay(() -> AUTH.doiMatKhau("Nhakhoa@123", "Moi12345", "Moi12345"), "đổi mật khẩu của chính mình");
        Kiem.chay(() -> { AUTH.logout(); AUTH.login("minhtam", "Moi12345"); }, "đăng nhập bằng mật khẩu mới");
    }

    static void tongQuan() {
        Kiem.nhom("Tổng quan theo vai trò");
        for (String u : List.of("admin", "lethian", "tranminh", "vophuc", "minhtam", "quockhanh")) {
            Kiem.chay(() -> {
                if (u.equals("minhtam")) { AUTH.logout(); AUTH.login(u, "Moi12345"); } else dn(u);
                DashboardData d = new DashboardService().tai();
                System.out.println("      " + u + ": lịch hẹn=" + (d.lichHenHomNay() != null) + ", doanh thu=" + d.doanhThuHomNay() + ", kho dưới định mức=" + d.duoiDinhMuc()
                        + ", ca thiếu=" + d.caThieuNguoi());
            }, "tải Tổng quan với " + u);
        }
    }

    static void doiSoat() {
        Kiem.nhom("Đối soát dữ liệu sau toàn bộ kiểm thử (truy vấn của 04_kiem_tra.sql)");
        long lechTon = num("SELECT COUNT(*) FROM LoHang l WHERE l.soLuongTon <> "
                + "COALESCE((SELECT SUM(soLuong) FROM ChiTietPhieuNhap n WHERE n.maLo = l.maLo), 0) "
                + "- COALESCE((SELECT SUM(soLuong) FROM ChiTietPhieuXuat x WHERE x.maLo = l.maLo), 0) "
                + "+ COALESCE((SELECT SUM(c.soLuongThucTe - c.soLuongHeThong) FROM ChiTietDieuChinhKho c JOIN PhieuDieuChinhKho p ON p.maPhieu = c.maPhieu "
                + "WHERE p.trangThai = 'DA_DUYET' AND c.maLo = l.maLo), 0)");
        Kiem.dung(lechTon == 0, "tồn lô khớp lịch sử phiếu (lệch: " + lechTon + ")");
        List<String> sai = Tx.read(c -> Jdbc.list(c, "SELECT hd.maHoaDon, hd.trangThai, "
                + "(SELECT COALESCE(SUM(soLuong * donGiaApDung), 0) FROM ChiTietHoaDon ct WHERE ct.maHoaDon = hd.maHoaDon) AS tong, "
                + "(SELECT COALESCE(SUM(soTien), 0) FROM ThanhToan t WHERE t.maHoaDon = hd.maHoaDon AND t.trangThai = 'THANH_CONG') AS tra FROM HoaDon hd",
                rs -> {
                    String tt = rs.getString(2);
                    BigDecimal tong = rs.getBigDecimal(3), tra = rs.getBigDecimal(4);
                    String dung = tt.equals("DA_HUY") ? "DA_HUY" : tra.signum() == 0 ? "CHUA_THANH_TOAN" : tra.compareTo(tong) >= 0 ? "DA_THANH_TOAN" : "THANH_TOAN_MOT_PHAN";
                    return dung.equals(tt) ? null : rs.getString(1) + ":" + tt + "≠" + dung;
                })).stream().filter(java.util.Objects::nonNull).toList();
        Kiem.dung(sai.isEmpty(), "trạng thái hóa đơn khớp số đã thu " + sai);
        Kiem.dung(num("SELECT COUNT(*) FROM YeuCauHuyHoaDon WHERE maTaiKhoanDuyet = maTaiKhoanTao") == 0, "không có yêu cầu hủy tự duyệt");
        Kiem.dung(num("SELECT COUNT(*) FROM PhieuDieuChinhKho WHERE maTaiKhoanDuyet = maTaiKhoanTao") == 0, "không có phiếu điều chỉnh tự duyệt");
        Kiem.dung(num("SELECT COUNT(*) FROM NhanVien nv JOIN TaiKhoan tk ON tk.maNhanVien = nv.maNhanVien WHERE nv.trangThai = 'NGHI_VIEC' AND tk.trangThai <> 'VO_HIEU'") == 0,
                "nhân viên nghỉ việc không còn tài khoản hoạt động");
        Kiem.dung(num("SELECT COUNT(*) FROM PhanCong pc JOIN NhanVien nv ON nv.maNhanVien = pc.maNhanVien WHERE nv.trangThai = 'NGHI_VIEC' AND pc.trangThai IN ('CHO_HIEU_LUC','DANG_THUC_HIEN')") == 0,
                "nhân viên nghỉ việc không còn phân công mở");
        Kiem.dung(num("SELECT COUNT(*) FROM NhatKyThaoTac") > 100, "nhật ký đã ghi " + num("SELECT COUNT(*) FROM NhatKyThaoTac") + " dòng");
    }
}
