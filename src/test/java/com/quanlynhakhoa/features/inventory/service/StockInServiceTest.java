package com.quanlynhakhoa.features.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.auth.exception.PermissionDeniedException;
import com.quanlynhakhoa.features.inventory.dto.request.InventoryItemRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockInLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockInRequest;
import com.quanlynhakhoa.features.inventory.dto.request.SupplierRequest;
import com.quanlynhakhoa.features.inventory.dto.response.StockInDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockInSummaryResponse;
import com.quanlynhakhoa.features.inventory.entity.StockLot;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.enums.StockInStatus;
import com.quanlynhakhoa.features.inventory.exception.InventoryValidationException;
import com.quanlynhakhoa.features.inventory.support.InventoryTestFixture;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StockInServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 17);
    private static final LocalDateTime NOW = TODAY.atTime(9, 30);
    private static final Set<Role> ADMIN = Set.of(Role.SYSTEM_ADMIN);
    private static final InventoryActor ACTOR = new InventoryActor(ADMIN, "NV0001", "Nguyễn Văn Trí");

    private final InventoryTestFixture fx = new InventoryTestFixture();
    private String ncc;

    @BeforeEach
    void setUp() {
        fx.receipts.addEmployee("NV0001", "Nguyễn Văn Trí");
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.THUOC, "TH001", "Thuốc tê Lidocaine",
                "ống", 10, null, null, false, null));
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.VAT_TU, "VT001", "Găng tay",
                "hộp", 10, null, null, false, null));
        ncc = fx.stockInService.createSupplier(ADMIN,
                new SupplierRequest("Dược phẩm Hoàng Gia", "0901 234 567", "Q.1", "lienhe@hoanggia.vn")).ma();
    }

    private static StockInLineRequest thuoc(String lo, int sl, long gia) {
        return new StockInLineRequest(InventoryItemType.THUOC, "TH001", lo, TODAY.plusYears(1), sl, BigDecimal.valueOf(gia));
    }

    private static StockInLineRequest vatTu(String lo, int sl, long gia) {
        return new StockInLineRequest(InventoryItemType.VAT_TU, "VT001", lo, null, sl, BigDecimal.valueOf(gia));
    }

    private StockInDetailResponse create(StockInLineRequest... lines) {
        return fx.stockInService.create(ACTOR, new StockInRequest(TODAY, ncc, "Đợt 1", List.of(lines)), NOW);
    }

    @Test
    void supplierGetsSequentialCode() {
        assertEquals("NCC001", ncc);
        assertEquals("NCC002", fx.stockInService.createSupplier(ADMIN,
                new SupplierRequest("Thiết bị Y tế Tiến Phát", "0281234567", null, null)).ma());
        assertThrows(InventoryValidationException.class, () -> fx.stockInService.createSupplier(ADMIN,
                new SupplierRequest("dược phẩm hoàng gia", "0901234567", null, null)), "Trùng tên");
        assertThrows(InventoryValidationException.class, () -> fx.stockInService.createSupplier(ADMIN,
                new SupplierRequest("X", "abc", null, null)), "SĐT sai");
    }

    @Test
    void createKeepsReceiptPendingAndDoesNotTouchStock() {
        StockInDetailResponse r = create(thuoc("A1", 10, 12_000), vatTu("G1", 5, 50_000));
        assertEquals("PN0001", r.maPhieu());
        assertEquals(StockInStatus.CHO_XU_LY, r.trangThai());
        assertEquals(new BigDecimal("370000"), r.tongTien());
        assertEquals("Nguyễn Văn Trí", r.nguoiLap());
        assertEquals("Dược phẩm Hoàng Gia", r.tenNhaCungCap());
        assertTrue(fx.lots.all().isEmpty(), "Chưa nhập kho thì chưa có lô");
        assertEquals("PN0002", create(vatTu("G2", 1, 1)).maPhieu());
    }

    @Test
    void completeCreatesLotsThenAddsToExistingLot() {
        String first = create(thuoc("A1", 10, 1)).maPhieu();
        fx.stockInService.complete(ACTOR, first, NOW);
        String second = create(thuoc("a1", 7, 1)).maPhieu();
        StockInDetailResponse done = fx.stockInService.complete(ACTOR, second, NOW);

        assertEquals(StockInStatus.DA_HOAN_THANH, done.trangThai());
        assertEquals("Nguyễn Văn Trí", done.nguoiNhapKho());
        List<StockLot> lots = fx.lots.all();
        assertEquals(1, lots.size(), "Cùng mặt hàng + cùng số lô thì cộng dồn");
        assertEquals(17, lots.get(0).getSoLuongTon());
        assertEquals("A1", lots.get(0).getSoLo());
    }

    @Test
    void sameLotWithDifferentExpiryIsRejectedAndNothingChanges() {
        fx.stockInService.complete(ACTOR, create(thuoc("A1", 10, 1)).maPhieu(), NOW);
        String other = fx.stockInService.create(ACTOR, new StockInRequest(TODAY, ncc, null, List.of(
                new StockInLineRequest(InventoryItemType.THUOC, "TH001", "A1", TODAY.plusYears(2), 3, BigDecimal.ONE))),
                NOW).maPhieu();
        assertThrows(InventoryValidationException.class, () -> fx.stockInService.complete(ACTOR, other, NOW));
        assertEquals(10, fx.lots.all().get(0).getSoLuongTon());
    }

    @Test
    void completedOrCancelledReceiptCannotBeCompletedOrEditedAgain() {
        String ma = create(vatTu("G1", 5, 1)).maPhieu();
        fx.stockInService.complete(ACTOR, ma, NOW);
        assertThrows(InventoryValidationException.class, () -> fx.stockInService.complete(ACTOR, ma, NOW));
        assertThrows(InventoryValidationException.class, () -> fx.stockInService.cancel(ACTOR, ma, "nhầm"));
        assertThrows(InventoryValidationException.class, () -> fx.stockInService.update(ACTOR, ma,
                new StockInRequest(TODAY, ncc, null, List.of(vatTu("G1", 99, 1)))));
        assertEquals("Đã kiểm", fx.stockInService.updateNote(ACTOR, ma, "Đã kiểm").ghiChu(),
                "Phiếu đã hoàn thành vẫn sửa được ghi chú");
        assertEquals(5, fx.lots.all().get(0).getSoLuongTon());
    }

    @Test
    void cancelNeedsReasonAndKeepsHistory() {
        String ma = create(vatTu("G1", 5, 1)).maPhieu();
        assertThrows(InventoryValidationException.class, () -> fx.stockInService.cancel(ACTOR, ma, "  "));
        StockInDetailResponse cancelled = fx.stockInService.cancel(ACTOR, ma, "Nhà cung cấp giao sai hàng");
        assertEquals(StockInStatus.DA_HUY, cancelled.trangThai());
        assertEquals("Nhà cung cấp giao sai hàng", cancelled.lyDoHuy());
        assertEquals(1, cancelled.dong().size());
        assertThrows(InventoryValidationException.class, () -> fx.stockInService.updateNote(ACTOR, ma, "x"));
    }

    @Test
    void pendingReceiptCanBeEdited() {
        String ma = create(vatTu("G1", 5, 1)).maPhieu();
        StockInDetailResponse edited = fx.stockInService.update(ACTOR, ma,
                new StockInRequest(TODAY.minusDays(1), ncc, "Sửa", List.of(vatTu("G1", 8, 2), thuoc("A1", 1, 3))));
        assertEquals(2, edited.dong().size());
        assertEquals(new BigDecimal("19"), edited.tongTien());
        assertEquals(TODAY.minusDays(1), edited.ngayNhap());
    }

    @Test
    void lineValidation() {
        assertThrows(InventoryValidationException.class, () -> create(), "Không có dòng");
        assertThrows(InventoryValidationException.class, () -> create(new StockInLineRequest(
                InventoryItemType.THUOC, "TH001", "A1", null, 1, BigDecimal.ONE)), "Thuốc thiếu HSD");
        assertThrows(InventoryValidationException.class, () -> create(new StockInLineRequest(
                InventoryItemType.THUOC, "TH001", "A1", TODAY, 1, BigDecimal.ONE)), "HSD không sau ngày nhập");
        assertThrows(InventoryValidationException.class, () -> create(thuoc("A1", 0, 1)), "SL = 0");
        assertThrows(InventoryValidationException.class, () -> create(thuoc("A 1", 1, 1)), "Số lô có khoảng trắng");
        assertThrows(InventoryValidationException.class, () -> create(thuoc("A1", 1, 1), thuoc("a1", 2, 1)), "Trùng lô");
        assertThrows(InventoryValidationException.class, () -> create(new StockInLineRequest(
                InventoryItemType.VAT_TU, "VT999", "X", null, 1, BigDecimal.ONE)), "Mặt hàng không tồn tại");
        assertThrows(InventoryValidationException.class, () -> create(new StockInLineRequest(
                InventoryItemType.VAT_TU, "VT001", "X", null, 1, new BigDecimal("1.5"))), "Đơn giá lẻ");
        assertThrows(InventoryValidationException.class, () -> fx.stockInService.create(ACTOR,
                new StockInRequest(TODAY.plusDays(1), ncc, null, List.of(vatTu("G1", 1, 1))), NOW), "Ngày tương lai");
    }

    @Test
    void summaryCountsAndMonthlyValue() {
        fx.stockInService.complete(ACTOR, create(vatTu("G1", 10, 100)).maPhieu(), NOW);
        fx.stockInService.cancel(ACTOR, create(vatTu("G2", 1, 1)).maPhieu(), "sai");
        create(vatTu("G3", 1, 1));
        String last = fx.stockInService.create(ACTOR, new StockInRequest(TODAY.minusMonths(1), ncc, null,
                List.of(vatTu("G4", 4, 100))), NOW).maPhieu();
        fx.stockInService.complete(ACTOR, last, NOW);

        StockInSummaryResponse s = fx.stockInService.summarize(ADMIN, TODAY);
        assertEquals(4, s.tongPhieu());
        assertEquals(1, s.soChoXuLy());
        assertEquals(2, s.soHoanThanh());
        assertEquals(1, s.soDaHuy());
        assertEquals(new BigDecimal("1000"), s.giaTriThangNay());
        assertEquals(new BigDecimal("400"), s.giaTriThangTruoc());
    }

    @Test
    void permissionsAndSession() {
        Set<Role> receptionist = Set.of(Role.RECEPTIONIST);
        assertThrows(PermissionDeniedException.class, () -> fx.stockInService.list(receptionist));
        assertThrows(PermissionDeniedException.class, () -> fx.stockInService.create(
                new InventoryActor(receptionist, "NV0002", "Lễ tân"),
                new StockInRequest(TODAY, ncc, null, List.of(vatTu("G1", 1, 1)))));
        assertThrows(InventoryValidationException.class, () -> fx.stockInService.create(
                new InventoryActor(ADMIN, " ", "?"),
                new StockInRequest(TODAY, ncc, null, List.of(vatTu("G1", 1, 1)))), "Phiên chưa gắn nhân viên");
    }

    @Test
    void itemListShowsUsableStockAndNearestExpiry() {
        fx.stockInService.complete(ACTOR, create(thuoc("A1", 4, 1), thuoc("A2", 3, 1)).maPhieu(), NOW);
        var item = fx.itemService.search(ADMIN, "TH001", null).get(0);
        assertEquals(7, item.tonKho());
        assertEquals(TODAY.plusYears(1), item.hanSuDungGanNhat());
    }
}
