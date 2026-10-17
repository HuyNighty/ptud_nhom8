package com.quanlynhakhoa.features.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.auth.exception.PermissionDeniedException;
import com.quanlynhakhoa.features.auth.service.AuthorizationService;
import com.quanlynhakhoa.features.auth.service.RolePermissionPolicy;
import com.quanlynhakhoa.features.inventory.dto.response.InventoryOverviewResponse;
import com.quanlynhakhoa.features.inventory.enums.ExpiryLevel;
import com.quanlynhakhoa.features.inventory.enums.StockLevel;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InventoryOverviewServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 17);
    private final InventoryOverviewService service =
            new InventoryOverviewService(new AuthorizationService(new RolePermissionPolicy()));

    @Test
    void stockBelowMinimumIsShortage() {
        assertEquals(StockLevel.THIEU, InventoryOverviewService.classifyStock(35, 50));
    }

    @Test
    void stockWithinTwentyPercentBufferIsRunningLow() {
        assertEquals(StockLevel.SAP_HET, InventoryOverviewService.classifyStock(50, 50));
        assertEquals(StockLevel.SAP_HET, InventoryOverviewService.classifyStock(59, 50));
        assertEquals(StockLevel.DU, InventoryOverviewService.classifyStock(60, 50));
    }

    @Test
    void negativeStockIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> InventoryOverviewService.classifyStock(-1, 10));
    }

    @Test
    void expiryBoundaries() {
        assertEquals(ExpiryLevel.DA_HET_HAN, InventoryOverviewService.classifyExpiry(TODAY.minusDays(1), TODAY));
        assertEquals(ExpiryLevel.CON_TOI_DA_7_NGAY, InventoryOverviewService.classifyExpiry(TODAY, TODAY));
        assertEquals(ExpiryLevel.CON_TOI_DA_7_NGAY, InventoryOverviewService.classifyExpiry(TODAY.plusDays(7), TODAY));
        assertEquals(ExpiryLevel.CON_TOI_DA_30_NGAY, InventoryOverviewService.classifyExpiry(TODAY.plusDays(8), TODAY));
        assertEquals(ExpiryLevel.CON_TOI_DA_30_NGAY, InventoryOverviewService.classifyExpiry(TODAY.plusDays(30), TODAY));
        assertEquals(ExpiryLevel.CON_HAN, InventoryOverviewService.classifyExpiry(TODAY.plusDays(31), TODAY));
    }

    @Test
    void roleWithoutInventoryReadIsDenied() {
        assertThrows(PermissionDeniedException.class,
                () -> service.getOverview(Set.of(Role.RECEPTIONIST), TODAY));
    }

    @Test
    void overviewCountsAreConsistentWithLists() {
        InventoryOverviewResponse r = service.getOverview(Set.of(Role.SYSTEM_ADMIN), TODAY);
        assertTrue(r.prototypeData(), "Dữ liệu mẫu phải được đánh dấu prototype");
        assertEquals(r.matHangSapHetTon().size(), r.soMatHangSapHetTon());
        long urgent = r.loTheoDoiHan().stream()
                .filter(l -> l.mucCanhBao() == ExpiryLevel.DA_HET_HAN
                        || l.mucCanhBao() == ExpiryLevel.CON_TOI_DA_7_NGAY)
                .count();
        assertEquals(urgent, r.soLoCanXuLyGap());
        assertTrue(r.matHangSapHetTon().stream().noneMatch(i -> i.mucTon() == StockLevel.DU));
        assertTrue(r.loTheoDoiHan().stream().noneMatch(l -> l.mucCanhBao() == ExpiryLevel.CON_HAN));
    }
}
