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
import com.quanlynhakhoa.features.inventory.dto.response.InventoryOverviewResponse;
import com.quanlynhakhoa.features.inventory.enums.ExpiryLevel;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.enums.StockLevel;
import com.quanlynhakhoa.features.inventory.support.InventoryTestFixture;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InventoryOverviewServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 17);
    private static final Set<Role> ADMIN = Set.of(Role.SYSTEM_ADMIN);
    private static final InventoryActor ACTOR = new InventoryActor(ADMIN, "NV0001", "Người dùng");

    private final InventoryTestFixture fx = new InventoryTestFixture();

    @Test
    void emptyCatalog() {
        InventoryOverviewResponse r = fx.overviewService.getOverview(ADMIN, TODAY);
        assertEquals(0, r.tongMatHang());
        assertTrue(r.matHangSapHetTon().isEmpty());
        assertTrue(r.loTheoDoiHan().isEmpty());
        assertTrue(r.canChuY().stream().anyMatch(n -> n.noiDung().contains("chưa có mặt hàng")));
    }

    @Test
    void figuresComeFromCatalogLotsAndReceipts() {
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.THUOC, "TH001", "Thuốc tê", "ống",
                50, null, null, true, null));
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.VAT_TU, "VT001", "Găng tay", "hộp",
                10, null, null, false, null));
        String ncc = fx.stockInService.createSupplier(ADMIN, new SupplierRequest("Dược phẩm A", "0901234567", null, null)).ma();
        var receipt = fx.stockInService.create(ACTOR, new StockInRequest(TODAY, ncc, null, List.of(
                new StockInLineRequest(InventoryItemType.THUOC, "TH001", "L1", TODAY.plusDays(5), 20, BigDecimal.TEN),
                new StockInLineRequest(InventoryItemType.VAT_TU, "VT001", "G1", null, 30, BigDecimal.ONE))),
                TODAY.atTime(9, 0));
        fx.stockInService.complete(ACTOR, receipt.maPhieu(), TODAY.atTime(10, 0));
        fx.stockInService.create(ACTOR, new StockInRequest(TODAY, ncc, null, List.of(
                new StockInLineRequest(InventoryItemType.VAT_TU, "VT001", "G2", null, 5, BigDecimal.ONE))),
                TODAY.atTime(11, 0));

        InventoryOverviewResponse r = fx.overviewService.getOverview(ADMIN, TODAY);
        assertEquals(2, r.tongMatHang());
        assertEquals(1, r.soMatHangSapHetTon(), "TH001 tồn 20 < định mức 50");
        assertEquals("TH001", r.matHangSapHetTon().get(0).maMatHang());
        assertEquals(StockLevel.THIEU, r.matHangSapHetTon().get(0).mucTon());
        assertEquals(1, r.loTheoDoiHan().size(), "Lô L1 còn 5 ngày");
        assertEquals(ExpiryLevel.CON_TOI_DA_7_NGAY, r.loTheoDoiHan().get(0).mucCanhBao());
        assertEquals(1, r.soLoCanXuLyGap());
        assertEquals(1, r.soPhieuChoXuLy());
        assertEquals(1, r.hoatDongHomNay().nhapKho());
        assertEquals(0, r.hoatDongHomNay().xuatKho(), "Đã có xuất kho: hôm nay chưa xuất phiếu nào");
    }

    @Test
    void expiredLotIsExcludedFromUsableStock() {
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.THUOC, "TH001", "Thuốc tê", "ống",
                1, null, null, false, null));
        String ncc = fx.stockInService.createSupplier(ADMIN, new SupplierRequest("NCC", "0901234567", null, null)).ma();
        LocalDate past = TODAY.minusDays(30);
        var receipt = fx.stockInService.create(ACTOR, new StockInRequest(past, ncc, null, List.of(
                new StockInLineRequest(InventoryItemType.THUOC, "TH001", "OLD", past.plusDays(10), 8, BigDecimal.ONE))),
                past.atTime(9, 0));
        fx.stockInService.complete(ACTOR, receipt.maPhieu(), past.atTime(9, 30));

        InventoryOverviewResponse r = fx.overviewService.getOverview(ADMIN, TODAY);
        assertEquals(StockLevel.HET_HANG, r.matHangSapHetTon().get(0).mucTon(), "Lô đã hết hạn không tính vào tồn khả dụng");
        assertEquals(ExpiryLevel.DA_HET_HAN, r.loTheoDoiHan().get(0).mucCanhBao());
        assertTrue(r.canChuY().stream().anyMatch(n -> n.noiDung().contains("đã hết hạn")));
    }

    @Test
    void roleWithoutInventoryReadIsDenied() {
        assertThrows(PermissionDeniedException.class, () -> fx.overviewService.getOverview(Set.of(Role.RECEPTIONIST)));
    }

    @Test
    void stockClassification() {
        assertEquals(StockLevel.HET_HANG, InventoryRules.classifyStock(0, 0));
        assertEquals(StockLevel.THIEU, InventoryRules.classifyStock(35, 50));
        assertEquals(StockLevel.SAP_HET, InventoryRules.classifyStock(50, 50));
        assertEquals(StockLevel.SAP_HET, InventoryRules.classifyStock(59, 50));
        assertEquals(StockLevel.DU, InventoryRules.classifyStock(60, 50));
        assertThrows(IllegalArgumentException.class, () -> InventoryRules.classifyStock(-1, 10));
    }

    @Test
    void expiryBoundaries() {
        assertEquals(ExpiryLevel.DA_HET_HAN, InventoryRules.classifyExpiry(TODAY.minusDays(1), TODAY));
        assertEquals(ExpiryLevel.CON_TOI_DA_7_NGAY, InventoryRules.classifyExpiry(TODAY, TODAY));
        assertEquals(ExpiryLevel.CON_TOI_DA_7_NGAY, InventoryRules.classifyExpiry(TODAY.plusDays(7), TODAY));
        assertEquals(ExpiryLevel.CON_TOI_DA_30_NGAY, InventoryRules.classifyExpiry(TODAY.plusDays(8), TODAY));
        assertEquals(ExpiryLevel.CON_TOI_DA_30_NGAY, InventoryRules.classifyExpiry(TODAY.plusDays(30), TODAY));
        assertEquals(ExpiryLevel.CON_HAN, InventoryRules.classifyExpiry(TODAY.plusDays(31), TODAY));
        assertEquals(ExpiryLevel.CON_HAN, InventoryRules.classifyExpiry(null, TODAY), "Vật tư không có hạn");
    }
}
