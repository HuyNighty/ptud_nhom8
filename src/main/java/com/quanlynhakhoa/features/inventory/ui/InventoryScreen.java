package com.quanlynhakhoa.features.inventory.ui;

import com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.MenuGlyph;

/** Các màn hình trong không gian làm việc Quản lý kho, theo thứ tự menu (Figma QL kho). */
public enum InventoryScreen {
    OVERVIEW(null, "Tổng quan", MenuGlyph.HOME, true, null),

    ITEMS("KHO", "Vật tư & thuốc", MenuGlyph.BOX, true, null),
    STOCK_IN("KHO", "Nhập kho", MenuGlyph.BOX, true, null),
    STOCK_OUT("KHO", "Xuất kho", MenuGlyph.BOX, true, null),
    RETURN("KHO", "Hoàn trả kho", MenuGlyph.BOX, false,
            "Nhập lại hàng đã xuất, gắn với phiếu xuất gốc (UC32)."),
    STOCKTAKE("KHO", "Kiểm kê", MenuGlyph.BOX, false,
            "Phiếu kiểm kê có người lập và người duyệt khác nhau (UC28)."),
    DISPOSAL("KHO", "Hủy / hao hụt", MenuGlyph.BOX, false,
            "Ghi nhận hư hỏng, hết hạn; chỉ giảm tồn sau khi được duyệt (UC35)."),
    CONTROLLED_DRUGS("KHO", "Thuốc kiểm soát đặc biệt", MenuGlyph.BOX, false,
            "Sổ theo dõi thuốc kiểm soát đặc biệt, bắt buộc người nhận khi xuất (UC33)."),
    EMERGENCY_KIT("KHO", "Hộp thuốc cấp cứu", MenuGlyph.BOX, false,
            "Danh mục hộp cấp cứu và lịch sử từng lần kiểm tra (UC34)."),

    STOCK_LEVEL("THEO DÕI", "Tồn kho", MenuGlyph.BOX, false,
            "Tồn theo mặt hàng và theo lô, cảnh báo dưới định mức (UC36)."),
    EXPIRY("THEO DÕI", "Hạn sử dụng", MenuGlyph.BOX, false,
            "Lô đã hết hạn, còn ≤ 7 ngày và ≤ 30 ngày (UC37)."),

    WORK_SCHEDULE("CÁ NHÂN", "Lịch làm việc", MenuGlyph.CALENDAR, false,
            "Lịch phân công ca của bản thân — thuộc chức năng nhân sự.");

    private final String section;
    private final String label;
    private final MenuGlyph glyph;
    private final boolean implemented;
    private final String plannedDescription;

    InventoryScreen(String section, String label, MenuGlyph glyph, boolean implemented, String plannedDescription) {
        this.section = section;
        this.label = label;
        this.glyph = glyph;
        this.implemented = implemented;
        this.plannedDescription = plannedDescription;
    }

    /** null = mục đứng đầu, không thuộc nhóm. */
    public String section() {
        return section;
    }

    public String label() {
        return label;
    }

    MenuGlyph glyph() {
        return glyph;
    }

    public boolean implemented() {
        return implemented;
    }

    public String plannedDescription() {
        return plannedDescription;
    }
}
