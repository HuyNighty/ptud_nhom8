package com.quanlynhakhoa.features.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.auth.exception.PermissionDeniedException;
import com.quanlynhakhoa.features.inventory.dto.request.InventoryItemRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockInLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockInRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockMonitorAdjustRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockOutLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockOutRequest;
import com.quanlynhakhoa.features.inventory.dto.request.SupplierRequest;
import com.quanlynhakhoa.features.inventory.dto.response.StockHistoryResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockMonitorRowResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockMonitorSummaryResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockMovementResponse;
import com.quanlynhakhoa.features.inventory.entity.StockLot;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.enums.StockLevel;
import com.quanlynhakhoa.features.inventory.enums.StockOutReason;
import com.quanlynhakhoa.features.inventory.exception.InventoryValidationException;
import com.quanlynhakhoa.features.inventory.support.InventoryTestFixture;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StockMonitorServiceTest {
    private static final Set<Role> ADMIN = Set.of(Role.SYSTEM_ADMIN);
    private static final InventoryActor A = new InventoryActor(ADMIN, "NV0001", "Nguyễn Văn A");
    private static final InventoryActor B = new InventoryActor(ADMIN, "NV0002", "Trần Thị B");
    private static final LocalDate TODAY = LocalDate.now();

    private final InventoryTestFixture fx = new InventoryTestFixture();
    private int amoxicillinLot;

    @BeforeEach
    void setUp() {
        fx.receipts.addEmployee("NV0001", "Nguyễn Văn A");
        fx.receipts.addEmployee("NV0002", "Trần Thị B");
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.THUOC, "TH001", "Lidocaine", "Ống",
                200, "2%", "Lidocaine", false, null));
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.THUOC, "TH002", "Amoxicillin", "Viên",
                100, "500mg", "Amoxicillin", false, null));
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.VAT_TU, "VT001", "Găng tay y tế (S)",
                "Hộp", 50, null, null, false, null));
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.VAT_TU, "VT002", "Bông y tế 100g",
                "Gói", 15, null, null, false, null));
        String ncc = fx.stockInService.createSupplier(ADMIN, new SupplierRequest("Dược Trung ương", "0901234567", null,
                null)).ma();
        LocalDate d = TODAY.minusDays(5);
        String in = fx.stockInService.create(A, new StockInRequest(d, ncc, null, List.of(
                new StockInLineRequest(InventoryItemType.THUOC, "TH001", "LD1", TODAY.plusDays(400), 120,
                        BigDecimal.valueOf(5_000)),
                new StockInLineRequest(InventoryItemType.THUOC, "TH002", "AM1", TODAY.plusDays(300), 250,
                        BigDecimal.valueOf(2_000)),
                new StockInLineRequest(InventoryItemType.VAT_TU, "VT001", "GT1", TODAY.plusDays(700), 35,
                        BigDecimal.valueOf(30_000)))), d.atTime(8, 0)).maPhieu();
        fx.stockInService.complete(A, in, d.atTime(9, 0));
        for (StockLot l : fx.lots.all()) {
            if (l.getSoLo().equals("AM1")) {
                amoxicillinLot = l.getMaLo();
            }
        }
    }

    private StockMonitorRowResponse row(String ma) {
        return fx.stockMonitorService.list(ADMIN).stream().filter(r -> r.ma().equals(ma)).findFirst().orElseThrow();
    }

    @Test
    void classifiesEveryCatalogItemAgainstItsMinimum() {
        assertEquals(StockLevel.THIEU, row("TH001").mucTon());
        assertEquals(StockLevel.DU, row("TH002").mucTon());
        assertEquals(StockLevel.THIEU, row("VT001").mucTon());
        StockMonitorRowResponse cotton = row("VT002");
        assertEquals(StockLevel.HET_HANG, cotton.mucTon());
        assertEquals(0, cotton.tonThucTe());
        assertEquals(BigDecimal.ZERO, cotton.giaTri());
        assertEquals("Lidocaine 2%", row("TH001").ten());
        assertEquals(new BigDecimal("600000"), row("TH001").giaTri());
        assertNull(row("TH001").viTriLuuTru());
    }

    @Test
    void summaryAddsQuantitiesAndValueAtLatestImportPrice() {
        StockMonitorSummaryResponse s = fx.stockMonitorService.summarize(ADMIN);
        assertEquals(4, s.tongMatHang());
        assertEquals(405, s.tongSoLuongTon());
        assertEquals(0, new BigDecimal("2150000").compareTo(s.giaTriTonKho()));
        assertEquals(3, s.duoiDinhMuc());
        assertEquals(0, s.sapHet());
        assertEquals(1, s.duTon());
    }

    @Test
    void quickAdjustChangesMonitoringFieldsButNotStock() {
        StockMonitorRowResponse r = fx.stockMonitorService.adjust(A, InventoryItemType.VAT_TU, "vt001",
                new StockMonitorAdjustRequest(30, "  Kệ A-01 ", true));
        assertEquals("Kệ A-01", r.viTriLuuTru());
        assertEquals(30, r.dinhMucToiThieu());
        assertTrue(r.daDatHang());
        assertEquals(35, r.tonThucTe());
        assertEquals(StockLevel.SAP_HET, r.mucTon(), "35 < 30 × 1,2 nên vẫn ở vùng sắp hết");

        assertNull(fx.stockMonitorService.adjust(A, InventoryItemType.VAT_TU, "VT001",
                new StockMonitorAdjustRequest(30, " ", false)).viTriLuuTru());
        assertThrows(InventoryValidationException.class, () -> fx.stockMonitorService.adjust(A,
                InventoryItemType.VAT_TU, "VT001", new StockMonitorAdjustRequest(-1, null, false)));
        assertThrows(InventoryValidationException.class, () -> fx.stockMonitorService.adjust(A,
                InventoryItemType.VAT_TU, "VT001", new StockMonitorAdjustRequest(1, "x".repeat(51), false)));
        assertThrows(InventoryValidationException.class, () -> fx.stockMonitorService.adjust(A,
                InventoryItemType.THUOC, "TH999", new StockMonitorAdjustRequest(1, null, false)));
        assertThrows(PermissionDeniedException.class, () -> fx.stockMonitorService.adjust(
                new InventoryActor(Set.of(Role.RECEPTIONIST), "NV0003", "Lễ tân"), InventoryItemType.VAT_TU, "VT001",
                new StockMonitorAdjustRequest(1, null, false)));
    }

    @Test
    void historyListsLotsAndRunningBalance() {
        String out = fx.stockOutService.create(A, new StockOutRequest(TODAY, StockOutReason.DIEU_TRI, "Phòng 1", null,
                List.of(new StockOutLineRequest(amoxicillinLot, 20)))).maPhieu();
        fx.stockOutService.complete(B, out);

        StockHistoryResponse h = fx.stockMonitorService.history(ADMIN, InventoryItemType.THUOC, "TH002");
        assertEquals(230, h.matHang().tonThucTe());
        assertEquals(1, h.lo().size());
        assertEquals(0, BigDecimal.valueOf(2_000).compareTo(h.lo().get(0).donGiaNhap()));
        assertEquals("Dược Trung ương", h.lo().get(0).nhaCungCap());
        List<StockMovementResponse> m = h.bienDong();
        assertEquals(List.of("Nhập kho", "Xuất kho"), m.stream().map(StockMovementResponse::loai).toList());
        assertEquals(250, m.get(0).nhap());
        assertEquals(250, m.get(0).tonSau());
        assertEquals(20, m.get(1).xuat());
        assertEquals(230, m.get(1).tonSau());
        assertEquals("Trần Thị B", m.get(1).nguoiThucHien());
        assertTrue(fx.stockMonitorService.history(ADMIN, InventoryItemType.VAT_TU, "VT002").bienDong().isEmpty());
    }
}
