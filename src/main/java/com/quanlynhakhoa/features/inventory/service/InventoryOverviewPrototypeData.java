package com.quanlynhakhoa.features.inventory.service;

import java.time.LocalDate;
import java.util.List;

/**
 * PROTOTYPE — dữ liệu mẫu cho màn hình Tổng quan kho.
 *
 * <p>Schema Thuoc/VatTu/LoHang/Phieu*Kho chưa được cấp version Flyway, nên chưa có
 * Repository. Khi migration được duyệt, xóa class này và thay bằng truy vấn
 * Repository trong {@link InventoryOverviewService}. Không dùng dữ liệu này để báo
 * "DB PASS".</p>
 */
final class InventoryOverviewPrototypeData {

    record StockItem(String ma, String ten, int ton, int dinhMuc) {
    }

    record Lot(String maLo, String tenSanPham, LocalDate hanSuDung, int soLuong, String donVi) {
    }

    static final int TONG_MAT_HANG = 158;
    static final int PHIEU_CHO_XU_LY = 5;
    static final int NHAP_HOM_NAY = 6;
    static final int XUAT_HOM_NAY = 12;
    static final int HOAN_TRA_HOM_NAY = 2;
    static final int HUY_HOM_NAY = 1;
    static final boolean CO_THUOC_KSDB_CAN_DOI_SOAT = true;
    static final boolean CO_HOP_CAP_CUU_CAN_KIEM_TRA = true;

    private InventoryOverviewPrototypeData() {
    }

    static List<StockItem> stockItems() {
        return List.of(
                new StockItem("VT001", "Găng tay y tế", 35, 50),
                new StockItem("VT002", "Kim tiêm", 68, 60),
                new StockItem("VT003", "Khẩu trang y tế", 15, 20),
                new StockItem("VT004", "Bông y tế", 34, 30),
                new StockItem("VT005", "Gạc vô trùng", 120, 40));
    }

    /** Hạn dùng tính tương đối theo ngày xem để dữ liệu mẫu không "cũ" theo thời gian. */
    static List<Lot> lots(LocalDate today) {
        return List.of(
                new Lot("L010", "Thuốc sát khuẩn C", today.minusDays(2), 4, "chai"),
                new Lot("L001", "Thuốc tê A", today.plusDays(3), 20, "ống"),
                new Lot("L008", "Vật tư trám răng", today.plusDays(6), 35, "cái"),
                new Lot("L004", "Thuốc kháng sinh B", today.plusDays(12), 12, "hộp"),
                new Lot("L012", "Chỉ nha khoa", today.plusDays(26), 50, "cuộn"),
                new Lot("L015", "Thuốc giảm đau D", today.plusDays(90), 30, "vỉ"));
    }
}
