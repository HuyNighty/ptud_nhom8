package com.quanlynhakhoa.desktop.menu;

import java.util.List;

/**
 * Cấu trúc menu của tài khoản Quản trị (Admin) — 21 màn hình, nhóm theo luồng nghiệp vụ:
 * tiếp nhận → khám & điều trị → thanh toán → nhân viên → thuốc & vật tư → kiểm soát truy cập.
 * <p>
 * Mỗi {@link Entry#key()} dùng làm tên card trong {@code CardLayout} của MainFrame.
 * Nguồn: docs/THIET_KE_DU_LIEU.md mục 8 (Use case → màn hình → Service).
 */
public final class AdminMenu {

    private AdminMenu() {
    }

    /** Một chức năng (màn hình) trong menu. {@code hint} hiển thị ở tooltip và dùng cho tìm kiếm. */
    public record Entry(String key, String title, MenuIcon.Shape icon, String hint) {
    }

    /** Một nhóm nghiệp vụ có thể xổ xuống / thu gọn. */
    public record Group(String key, String title, MenuIcon.Shape icon, List<Entry> entries) {
    }

    // ===== Khóa màn hình (dùng làm tên card) =====
    public static final String TONG_QUAN = "tong-quan";

    public static final String TIEP_NHAN = "tiep-nhan";
    public static final String LICH_HEN = "lich-hen";
    public static final String HO_SO_BENH_NHAN = "ho-so-benh-nhan";

    public static final String PHIEU_KHAM = "phieu-kham";
    public static final String KE_HOACH = "ke-hoach";
    public static final String TAI_KHAM = "tai-kham";

    public static final String HOA_DON = "hoa-don";
    public static final String DUYET_HUY = "duyet-huy";
    public static final String BANG_GIA = "bang-gia";

    public static final String NHAN_VIEN = "nhan-vien";
    public static final String PHAN_CONG = "phan-cong";
    public static final String BAN_GIAO = "ban-giao";
    public static final String DANH_GIA = "danh-gia";

    public static final String THUOC_VAT_TU = "thuoc-vat-tu";
    public static final String PHIEU_KHO = "phieu-kho";
    public static final String KIEM_KE = "kiem-ke";
    public static final String CAP_CUU = "cap-cuu";
    public static final String NHA_CUNG_CAP = "nha-cung-cap";

    public static final String TAI_KHOAN = "tai-khoan";
    public static final String NHAT_KY = "nhat-ky";

    /** Mục đứng riêng ở đầu menu. */
    public static Entry home() {
        return new Entry(TONG_QUAN, "Tổng quan", MenuIcon.Shape.GRID,
                "Số liệu trong ngày và việc cần xử lý");
    }

    /** Các nhóm nghiệp vụ, theo thứ tự hành trình của bệnh nhân rồi đến vận hành phòng khám. */
    public static List<Group> groups() {
        return List.of(
                new Group("grp-tiep-nhan", "Tiếp nhận & lịch hẹn", MenuIcon.Shape.BELL, List.of(
                        new Entry(TIEP_NHAN, "Tiếp nhận bệnh nhân", MenuIcon.Shape.USER_PLUS,
                                "UC01, UC02, UC08 — tìm, tiếp nhận, tạo hồ sơ bệnh nhân"),
                        new Entry(LICH_HEN, "Lịch hẹn", MenuIcon.Shape.CALENDAR,
                                "UC03, UC06, UC07 — tạo, xác nhận, đổi lịch, hủy lịch hẹn"),
                        new Entry(HO_SO_BENH_NHAN, "Hồ sơ bệnh nhân", MenuIcon.Shape.PATIENT_FILE,
                                "UC09, UC23 — xem, cập nhật hồ sơ, tiền sử, dị ứng"))),

                new Group("grp-kham", "Khám & điều trị", MenuIcon.Shape.TOOTH, List.of(
                        new Entry(PHIEU_KHAM, "Phiếu khám", MenuIcon.Shape.CLIPBOARD,
                                "UC10, UC11, UC16 — kết quả khám, chỉ định CLS, kê đơn"),
                        new Entry(KE_HOACH, "Kế hoạch điều trị", MenuIcon.Shape.PLAN,
                                "UC12, UC13, UC15 — lập, cập nhật, ghi kết quả điều trị"),
                        new Entry(TAI_KHAM, "Tái khám", MenuIcon.Shape.REPEAT,
                                "UC14 — hạng mục đến hạn / quá hạn tái khám"))),

                new Group("grp-thanh-toan", "Thanh toán & công nợ", MenuIcon.Shape.WALLET, List.of(
                        new Entry(HOA_DON, "Hóa đơn & thanh toán", MenuIcon.Shape.RECEIPT,
                                "UC04, UC05 — lập hóa đơn, thu tiền, công nợ"),
                        new Entry(DUYET_HUY, "Duyệt hủy hóa đơn", MenuIcon.Shape.CANCEL_DOC,
                                "Duyệt / từ chối yêu cầu hủy (không tự duyệt yêu cầu của mình)"),
                        new Entry(BANG_GIA, "Bảng giá dịch vụ", MenuIcon.Shape.TAG,
                                "Danh mục dịch vụ và đơn giá"))),

                new Group("grp-nhan-vien", "Quản lý nhân viên", MenuIcon.Shape.TEAM, List.of(
                        new Entry(NHAN_VIEN, "Hồ sơ nhân viên", MenuIcon.Shape.ID_CARD,
                                "UC22, UC26 — hồ sơ nhân sự, thủ tục nghỉ việc"),
                        new Entry(PHAN_CONG, "Phân công & lịch làm việc", MenuIcon.Shape.CALENDAR_CHECK,
                                "UC17, UC18, UC20, UC24 — phân ca, nhiệm vụ, lịch tuần"),
                        new Entry(BAN_GIAO, "Bàn giao ca", MenuIcon.Shape.HANDOVER,
                                "UC19 — lập / xác nhận biên bản giao nhận ca"),
                        new Entry(DANH_GIA, "Đánh giá nhân viên", MenuIcon.Shape.STAR,
                                "UC25 — đánh giá theo quý"))),

                new Group("grp-kho", "Quản lý thuốc & vật tư", MenuIcon.Shape.BOX, List.of(
                        new Entry(THUOC_VAT_TU, "Thuốc & vật tư", MenuIcon.Shape.PILL,
                                "UC27, UC29, UC33, UC36, UC37 — tồn kho, hạn dùng, sổ kiểm soát đặc biệt"),
                        new Entry(PHIEU_KHO, "Nhập / xuất / hoàn trả", MenuIcon.Shape.TRUCK,
                                "UC30, UC31, UC32 — phiếu nhập, xuất (FEFO), hoàn trả"),
                        new Entry(KIEM_KE, "Kiểm kê & hao hụt", MenuIcon.Shape.CHECKLIST,
                                "UC28, UC35 — kiểm kê, hư hỏng, hết hạn, duyệt phiếu"),
                        new Entry(CAP_CUU, "Hộp cấp cứu", MenuIcon.Shape.FIRST_AID,
                                "UC34 — kiểm tra hộp thuốc cấp cứu"),
                        new Entry(NHA_CUNG_CAP, "Nhà cung cấp", MenuIcon.Shape.SUPPLIER,
                                "Danh mục nhà cung cấp"))),

                new Group("grp-truy-cap", "Kiểm soát truy cập", MenuIcon.Shape.SHIELD, List.of(
                        new Entry(TAI_KHOAN, "Tài khoản & phân quyền", MenuIcon.Shape.KEY,
                                "UC21 — tạo tài khoản, đổi vai trò, khóa / mở khóa"),
                        new Entry(NHAT_KY, "Nhật ký thao tác", MenuIcon.Shape.HISTORY,
                                "Lịch sử thao tác của mọi tài khoản")))
        );
    }
}
