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
import com.quanlynhakhoa.features.inventory.dto.request.StockOutLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockOutRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StocktakeCountRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StocktakeRequest;
import com.quanlynhakhoa.features.inventory.dto.request.SupplierRequest;
import com.quanlynhakhoa.features.inventory.dto.response.StocktakeDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StocktakeRowResponse;
import com.quanlynhakhoa.features.inventory.entity.StockLot;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.enums.StockOutReason;
import com.quanlynhakhoa.features.inventory.enums.StocktakeScope;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.exception.InventoryValidationException;
import com.quanlynhakhoa.features.inventory.support.InventoryTestFixture;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StocktakeServiceTest {
    private static final Set<Role> ADMIN = Set.of(Role.SYSTEM_ADMIN);
    private static final InventoryActor CREATOR = new InventoryActor(ADMIN, "NV0001", "Nguyễn Dần");
    private static final InventoryActor APPROVER = new InventoryActor(ADMIN, "NV0002", "Trần Thị Thảo");
    private static final LocalDate TODAY = LocalDate.now();

    private final InventoryTestFixture fx = new InventoryTestFixture();
    private int lotA;
    private int lotB;
    private int lotG;

    @BeforeEach
    void setUp() {
        fx.receipts.addEmployee("NV0001", "Nguyễn Dần");
        fx.receipts.addEmployee("NV0002", "Trần Thị Thảo");
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.THUOC, "TH001", "Thuốc tê", "ống",
                5, null, null, false, null));
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.VAT_TU, "VT001", "Găng tay", "hộp",
                5, null, null, false, null));
        String ncc = fx.stockInService.createSupplier(ADMIN, new SupplierRequest("NCC A", "0901234567", null, null)).ma();
        LocalDate d = TODAY.minusDays(10);
        String in = fx.stockInService.create(CREATOR, new StockInRequest(d, ncc, null, List.of(
                new StockInLineRequest(InventoryItemType.THUOC, "TH001", "A", TODAY.plusDays(100), 20, BigDecimal.ONE),
                new StockInLineRequest(InventoryItemType.THUOC, "TH001", "B", TODAY.plusDays(200), 10, BigDecimal.ONE),
                new StockInLineRequest(InventoryItemType.VAT_TU, "VT001", "G", null, 30, BigDecimal.ONE))),
                d.atTime(8, 0)).maPhieu();
        fx.stockInService.complete(CREATOR, in, d.atTime(9, 0));
        for (StockLot lot : fx.lots.all()) {
            switch (lot.getSoLo()) {
                case "A" -> lotA = lot.getMaLo();
                case "B" -> lotB = lot.getMaLo();
                default -> lotG = lot.getMaLo();
            }
        }
    }

    private StocktakeDetailResponse open(StocktakeScope scope) {
        return fx.stocktakeService.create(CREATOR, new StocktakeRequest(TODAY, scope, null), TODAY.atTime(8, 0));
    }

    private int stockOf(int maLo) {
        return fx.lots.findById(maLo).orElseThrow().getSoLuongTon();
    }

    @Test
    void createSnapshotsBookQuantitiesInScope() {
        StocktakeDetailResponse s = open(StocktakeScope.THUOC);
        assertEquals("KK001", s.maKiemKe());
        assertEquals(VoucherStatus.CHO_XU_LY, s.trangThai());
        assertEquals(2, s.dong().size(), "Chỉ lô thuốc");
        assertEquals(1, s.tongMatHang());
        assertEquals(20, s.dong().stream().filter(l -> l.maLo() == lotA).findFirst().orElseThrow().soSach());
        assertNull(s.dong().get(0).thucTe());
        InventoryValidationException e = assertThrows(InventoryValidationException.class,
                () -> open(StocktakeScope.TAT_CA));
        assertTrue(e.getMessage().contains("chưa hoàn tất"), "Mỗi lúc chỉ một đợt đang kiểm kê");
    }

    @Test
    void approveAdjustsByDifferenceAndKeepsMovementsDuringCount() {
        String ma = open(StocktakeScope.TAT_CA).maKiemKe();
        // Trong lúc kiểm, xuất 5 từ lô A (sổ sách lúc chụp = 20).
        String px = fx.stockOutService.create(CREATOR, new StockOutRequest(TODAY, StockOutReason.DIEU_TRI, "BS. B", null,
                List.of(new StockOutLineRequest(lotA, 5))), TODAY.atTime(9, 0)).maPhieu();
        fx.stockOutService.complete(CREATOR, px, TODAY.atTime(9, 5));
        fx.stocktakeService.saveCounts(CREATOR, ma, List.of(
                new StocktakeCountRequest(lotA, 18, "Vỡ 2 ống"),
                new StocktakeCountRequest(lotB, 11, "Nhập dư chưa ghi"),
                new StocktakeCountRequest(lotG, 30, null)));
        StocktakeDetailResponse done = fx.stocktakeService.approve(APPROVER, ma, TODAY.atTime(17, 0));
        assertEquals(VoucherStatus.DA_HOAN_THANH, done.trangThai());
        assertEquals("Trần Thị Thảo", done.nguoiDuyet());
        assertEquals(13, stockOf(lotA), "20 − 5 (xuất trong lúc kiểm) − 2 (thiếu)");
        assertEquals(11, stockOf(lotB));
        assertEquals(30, stockOf(lotG));
        assertEquals(2, done.thieu());
        assertEquals(1, done.thua());
        StocktakeRowResponse row = fx.stocktakeService.list(ADMIN).get(0);
        assertTrue(row.lech());
        assertEquals(2, row.tongMatHang());
        var summary = fx.stocktakeService.summarize(ADMIN, TODAY);
        assertEquals(1, summary.lech());
        assertEquals(0, summary.khop());
    }

    @Test
    void approvalRules() {
        String ma = open(StocktakeScope.TAT_CA).maKiemKe();
        fx.stocktakeService.saveCounts(CREATOR, ma, List.of(new StocktakeCountRequest(lotA, 19, null)));
        InventoryValidationException same = assertThrows(InventoryValidationException.class,
                () -> fx.stocktakeService.approve(CREATOR, ma, LocalDateTime.now()));
        assertTrue(same.getMessage().contains("khác người lập"), same.getMessage());
        InventoryValidationException notCounted = assertThrows(InventoryValidationException.class,
                () -> fx.stocktakeService.approve(APPROVER, ma, LocalDateTime.now()));
        assertTrue(notCounted.getMessage().contains("Còn 2 lô chưa nhập"), notCounted.getMessage());
        fx.stocktakeService.saveCounts(CREATOR, ma, List.of(new StocktakeCountRequest(lotB, 10, null),
                new StocktakeCountRequest(lotG, 30, null)));
        InventoryValidationException noReason = assertThrows(InventoryValidationException.class,
                () -> fx.stocktakeService.approve(APPROVER, ma, LocalDateTime.now()));
        assertTrue(noReason.getMessage().contains("lý do"), noReason.getMessage());
        assertEquals(20, stockOf(lotA), "Duyệt thất bại thì tồn giữ nguyên");
    }

    @Test
    void matchedStocktakeAndNegativeGuard() {
        String ma = open(StocktakeScope.VAT_TU).maKiemKe();
        fx.stocktakeService.saveCounts(CREATOR, ma, List.of(new StocktakeCountRequest(lotG, 30, null)));
        fx.stocktakeService.approve(APPROVER, ma, LocalDateTime.now());
        assertEquals(1, fx.stocktakeService.summarize(ADMIN, TODAY).khop());

        String ma2 = open(StocktakeScope.THUOC).maKiemKe();
        String px = fx.stockOutService.create(CREATOR, new StockOutRequest(TODAY, StockOutReason.DIEU_TRI, "BS. B", null,
                List.of(new StockOutLineRequest(lotB, 10))), TODAY.atTime(9, 0)).maPhieu();
        fx.stockOutService.complete(CREATOR, px, TODAY.atTime(9, 5));
        fx.stocktakeService.saveCounts(CREATOR, ma2, List.of(new StocktakeCountRequest(lotA, 20, null),
                new StocktakeCountRequest(lotB, 5, "Đếm sai")));
        InventoryValidationException e = assertThrows(InventoryValidationException.class,
                () -> fx.stocktakeService.approve(APPROVER, ma2, LocalDateTime.now()));
        assertTrue(e.getMessage().contains("đếm lại"), e.getMessage());
        assertEquals(0, stockOf(lotB));
    }

    @Test
    void editCancelAndValidation() {
        String ma = open(StocktakeScope.TAT_CA).maKiemKe();
        assertThrows(InventoryValidationException.class, () -> fx.stocktakeService.saveCounts(CREATOR, ma,
                List.of(new StocktakeCountRequest(lotA, -1, null))));
        assertThrows(InventoryValidationException.class, () -> fx.stocktakeService.saveCounts(CREATOR, ma,
                List.of(new StocktakeCountRequest(9999, 1, null))));
        assertThrows(InventoryValidationException.class,
                () -> fx.stocktakeService.update(CREATOR, ma, TODAY.plusDays(1), null));
        assertEquals("Kiểm cuối tháng",
                fx.stocktakeService.update(CREATOR, ma, TODAY.minusDays(1), "Kiểm cuối tháng").ghiChu());
        assertThrows(InventoryValidationException.class, () -> fx.stocktakeService.cancel(CREATOR, ma, ""));
        assertEquals(VoucherStatus.DA_HUY, fx.stocktakeService.cancel(CREATOR, ma, "Tạo nhầm phạm vi").trangThai());
        assertThrows(InventoryValidationException.class,
                () -> fx.stocktakeService.update(CREATOR, ma, TODAY, "x"), "Đã hủy không sửa");
        assertEquals("KK002", open(StocktakeScope.TAT_CA).maKiemKe(), "Hủy xong thì tạo được đợt mới");
        assertEquals(20, stockOf(lotA));
    }

    @Test
    void overviewNoticeAndPermissions() {
        open(StocktakeScope.TAT_CA);
        assertTrue(fx.overviewService.getOverview(ADMIN).canChuY().stream()
                .anyMatch(n -> n.noiDung().contains("kiểm kê")));
        Set<Role> receptionist = Set.of(Role.RECEPTIONIST);
        assertThrows(PermissionDeniedException.class, () -> fx.stocktakeService.list(receptionist));
        assertThrows(PermissionDeniedException.class, () -> fx.stocktakeService.create(
                new InventoryActor(receptionist, "NV0003", "Lễ tân"), new StocktakeRequest(TODAY, StocktakeScope.TAT_CA, null)));
    }
}
