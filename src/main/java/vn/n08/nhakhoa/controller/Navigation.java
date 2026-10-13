package vn.n08.nhakhoa.controller;

import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.view.screen.*;

import java.util.List;
import java.util.function.Supplier;

/**
 * Mô hình điều hướng: nhóm theo nghiệp vụ (như prototype v0.3). Mục chỉ hiện khi tài khoản có ít nhất một quyền tương ứng;
 * Service vẫn kiểm tra quyền lần nữa khi thao tác.
 */
public final class Navigation {
    public record Item(String id, String label, String uc, Supplier<Page> factory, Quyen... quyen) {
        public boolean visible() { return Session.canAny(quyen); }
    }

    public record Group(String name, List<Item> items) {}

    private Navigation() {}

    public static List<Group> groups() {
        return List.of(
                new Group("", List.of(
                        new Item("dashboard", "Tổng quan", "Figma QL-01", DashboardPage::new, Quyen.TONG_QUAN))),
                new Group("Tiếp nhận", List.of(
                        new Item("tiep-nhan", "Tiếp nhận bệnh nhân", "UC01, UC02", TiepNhanPage::new, Quyen.TIEP_NHAN),
                        new Item("lich-hen", "Lịch hẹn", "UC03, UC06, UC07", LichHenPage::new, Quyen.LICH_HEN),
                        new Item("benh-nhan", "Hồ sơ bệnh nhân", "UC08, UC09, UC23", BenhNhanPage::new, Quyen.BENH_NHAN_XEM, Quyen.TIEP_NHAN))),
                new Group("Khám & điều trị", List.of(
                        new Item("phieu-kham", "Phiếu khám", "UC10, UC11, UC16", PhieuKhamPage::new, Quyen.KHAM),
                        new Item("ke-hoach", "Kế hoạch điều trị", "UC12, UC13, UC15", KeHoachPage::new, Quyen.KE_HOACH),
                        new Item("tai-kham", "Tái khám", "UC14", TaiKhamPage::new, Quyen.TAI_KHAM))),
                new Group("Thu ngân", List.of(
                        new Item("hoa-don", "Hóa đơn & thanh toán", "UC04, UC05", HoaDonPage::new, Quyen.THU_NGAN, Quyen.HUY_GIAO_DICH),
                        new Item("duyet-huy", "Duyệt hủy hóa đơn", "Figma QL-08, QL-09", DuyetHuyPage::new, Quyen.DUYET_HUY),
                        new Item("dich-vu", "Bảng giá dịch vụ", "DichVu.capNhatDonGia", DichVuPage::new, Quyen.BANG_GIA, Quyen.THU_NGAN))),
                new Group("Nhân sự & ca làm", List.of(
                        new Item("nhan-vien", "Nhân viên", "UC22, UC26", NhanVienPage::new, Quyen.NHAN_VIEN),
                        new Item("phan-cong", "Phân công & lịch làm việc", "UC17, UC18, UC20, UC24", PhanCongPage::new, Quyen.LICH_LAM_VIEC),
                        new Item("ban-giao", "Bàn giao ca", "UC19", BanGiaoPage::new, Quyen.BAN_GIAO),
                        new Item("danh-gia", "Đánh giá nhân sự", "UC25", DanhGiaPage::new, Quyen.DANH_GIA))),
                new Group("Kho", List.of(
                        new Item("ton-kho", "Thuốc & vật tư", "UC27, UC29, UC33, UC36, UC37", TonKhoPage::new, Quyen.KHO, Quyen.DUYET_KHO),
                        new Item("phieu-kho", "Nhập / xuất / hoàn trả", "UC30, UC31, UC32", PhieuKhoPage::new, Quyen.KHO),
                        new Item("kiem-ke", "Kiểm kê & hao hụt", "UC28, UC35", KiemKePage::new, Quyen.KIEM_KE, Quyen.DUYET_KHO),
                        new Item("cap-cuu", "Hộp cấp cứu", "UC34", CapCuuPage::new, Quyen.CAP_CUU),
                        new Item("ncc", "Nhà cung cấp", "Hỗ trợ UC30", NhaCungCapPage::new, Quyen.KHO))),
                new Group("Hệ thống", List.of(
                        new Item("tai-khoan", "Tài khoản & phân quyền", "UC21", TaiKhoanPage::new, Quyen.TAI_KHOAN),
                        new Item("nhat-ky", "Nhật ký thao tác", "Hỗ trợ kiểm tra", NhatKyPage::new, Quyen.NHAT_KY))));
    }
}
