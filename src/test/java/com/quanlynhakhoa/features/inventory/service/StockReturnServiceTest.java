package com.quanlynhakhoa.features.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.auth.exception.PermissionDeniedException;
import com.quanlynhakhoa.features.inventory.dto.request.InventoryItemRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockInLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockInRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockOutLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockOutRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockReturnLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockReturnRequest;
import com.quanlynhakhoa.features.inventory.dto.request.SupplierRequest;
import com.quanlynhakhoa.features.inventory.dto.response.ReturnSourceResponse;
import com.quanlynhakhoa.features.inventory.dto.response.ReturnableLineResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockReturnDetailResponse;
import com.quanlynhakhoa.features.inventory.entity.StockLot;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.enums.ReturnReason;
import com.quanlynhakhoa.features.inventory.enums.StockOutReason;
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

class StockReturnServiceTest {
    private static final Set<Role> ADMIN = Set.of(Role.SYSTEM_ADMIN);
    private static final InventoryActor ACTOR = new InventoryActor(ADMIN, "NV0001", "Nguyễn Dần");
    private static final LocalDate TODAY = LocalDate.now();

    private final InventoryTestFixture fx = new InventoryTestFixture();
    private int lotA;
    private int lotB;
    private int lotOther;
    private String px;
    private String pxDisposal;
    private String pxPending;

    @BeforeEach
    void setUp() {
        fx.receipts.addEmployee("NV0001", "Nguyễn Dần");
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.THUOC, "TH001", "Thuốc tê", "ống",
                5, null, null, false, null));
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.VAT_TU, "VT001", "Găng tay", "hộp",
                5, null, null, false, null));
        String ncc = fx.stockInService.createSupplier(ADMIN, new SupplierRequest("NCC A", "0901234567", null, null)).ma();
        LocalDate inDate = TODAY.minusDays(10);
        String in = fx.stockInService.create(ACTOR, new StockInRequest(inDate, ncc, null, List.of(
                new StockInLineRequest(InventoryItemType.THUOC, "TH001", "A", TODAY.plusDays(100), 20,
                        BigDecimal.valueOf(1000)),
                new StockInLineRequest(InventoryItemType.THUOC, "TH001", "B", TODAY.plusDays(200), 10,
                        BigDecimal.valueOf(1200)),
                new StockInLineRequest(InventoryItemType.VAT_TU, "VT001", "G", null, 30, BigDecimal.valueOf(5000)))),
                inDate.atTime(8, 0)).maPhieu();
        fx.stockInService.complete(ACTOR, in, inDate.atTime(9, 0));
        for (StockLot lot : fx.lots.all()) {
            switch (lot.getSoLo()) {
                case "A" -> lotA = lot.getMaLo();
                case "B" -> lotB = lot.getMaLo();
                default -> lotOther = lot.getMaLo();
            }
        }
        LocalDate outDate = TODAY.minusDays(3);
        px = issue(outDate, StockOutReason.DIEU_TRI, true, new StockOutLineRequest(lotA, 5), new StockOutLineRequest(lotB, 3));
        pxDisposal = issue(outDate, StockOutReason.HUY_HAO_HUT, true, new StockOutLineRequest(lotA, 1));
        pxPending = issue(outDate, StockOutReason.DIEU_TRI, false, new StockOutLineRequest(lotB, 1));
    }

    private String issue(LocalDate date, StockOutReason reason, boolean complete, StockOutLineRequest... lines) {
        String ma = fx.stockOutService.create(ACTOR, new StockOutRequest(date, reason, "Phòng khám 01", null,
                List.of(lines)), date.atTime(10, 0)).maPhieu();
        if (complete) {
            fx.stockOutService.complete(ACTOR, ma, date.atTime(11, 0));
        }
        return ma;
    }

    private StockReturnRequest request(String maPx, ReturnReason reason, StockReturnLineRequest... lines) {
        return new StockReturnRequest(TODAY, maPx, reason, "Phòng khám 01", "Y tá Nguyễn Anh", null, List.of(lines));
    }

    private StockReturnDetailResponse create(ReturnReason reason, StockReturnLineRequest... lines) {
        return fx.stockReturnService.create(ACTOR, request(px, reason, lines), TODAY.atTime(14, 0));
    }

    private int stockOf(int maLo) {
        return fx.lots.findById(maLo).orElseThrow().getSoLuongTon();
    }

    @Test
    void onlyCompletedNonDisposalIssuesAreSources() {
        List<ReturnSourceResponse> sources = fx.stockReturnService.listSources(ADMIN);
        assertEquals(List.of(px), sources.stream().map(ReturnSourceResponse::maPhieuXuat).toList());
        assertEquals(8, sources.get(0).conDuocTra());
        assertThrows(InventoryValidationException.class, () -> fx.stockReturnService.create(ACTOR,
                request(pxDisposal, ReturnReason.DU_THUA, new StockReturnLineRequest(lotA, 1))), "Phiếu hủy/hao hụt");
        assertThrows(InventoryValidationException.class, () -> fx.stockReturnService.create(ACTOR,
                request(pxPending, ReturnReason.DU_THUA, new StockReturnLineRequest(lotB, 1))), "Phiếu chưa xuất");
    }

    @Test
    void approveRestocksTheSameLotAtIssueCost() {
        StockReturnDetailResponse draft = create(ReturnReason.DU_THUA,
                new StockReturnLineRequest(lotA, 2), new StockReturnLineRequest(lotB, 3));
        assertEquals("HT001", draft.maPhieu());
        assertEquals(VoucherStatus.CHO_XU_LY, draft.trangThai());
        assertEquals(14, stockOf(lotA), "Chưa duyệt thì chưa cộng tồn");
        assertEquals(new BigDecimal("5600"), draft.tongTien(), "2×1.000 + 3×1.200 theo giá vốn lúc xuất");

        StockReturnDetailResponse done = fx.stockReturnService.approve(ACTOR, draft.maPhieu(), TODAY.atTime(15, 0));
        assertEquals(VoucherStatus.DA_HOAN_THANH, done.trangThai());
        assertEquals("Nguyễn Dần", done.nguoiDuyet());
        assertEquals(16, stockOf(lotA));
        assertEquals(10, stockOf(lotB));
        assertEquals("Nguyễn Dần", fx.stockReturnService.list(ADMIN).get(0).nguoiNhan());
        assertEquals(new BigDecimal("5600"), fx.stockReturnService.summarize(ADMIN).giaTriHoanTra());
    }

    @Test
    void cannotReturnMoreThanIssuedIncludingPendingReturns() {
        create(ReturnReason.DU_THUA, new StockReturnLineRequest(lotA, 4));
        InventoryValidationException e = assertThrows(InventoryValidationException.class,
                () -> create(ReturnReason.DU_THUA, new StockReturnLineRequest(lotA, 2)));
        assertTrue(e.getMessage().contains("chỉ còn được trả 1"), e.getMessage());
        ReturnableLineResponse a = fx.stockReturnService.returnableLines(ADMIN, px, null).stream()
                .filter(l -> l.maLo() == lotA).findFirst().orElseThrow();
        assertEquals(5, a.daXuat());
        assertEquals(4, a.daTra());
        assertEquals(1, a.conDuocTra());
        assertEquals(new BigDecimal("1000"), a.donGia());
    }

    @Test
    void cancelledReturnFreesQuantityAndEditExcludesItself() {
        String ma = create(ReturnReason.DU_THUA, new StockReturnLineRequest(lotA, 5)).maPhieu();
        StockReturnDetailResponse edited = fx.stockReturnService.update(ACTOR, ma, new StockReturnRequest(TODAY, px,
                ReturnReason.XUAT_NHAM, "Phòng khám 02", "BS. Trần Bình", "Ghi chú",
                List.of(new StockReturnLineRequest(lotA, 5))));
        assertEquals(ReturnReason.XUAT_NHAM, edited.lyDo(), "Sửa phiếu không bị tính trùng số của chính nó");
        assertThrows(InventoryValidationException.class, () -> fx.stockReturnService.update(ACTOR, ma,
                new StockReturnRequest(TODAY, pxDisposal, ReturnReason.DU_THUA, "X", "Y", null,
                        List.of(new StockReturnLineRequest(lotA, 1)))), "Không đổi phiếu xuất gốc");
        assertThrows(InventoryValidationException.class, () -> fx.stockReturnService.cancel(ACTOR, ma, " "));
        fx.stockReturnService.cancel(ACTOR, ma, "Lập nhầm");
        assertEquals(5, fx.stockReturnService.returnableLines(ADMIN, px, null).stream()
                .filter(l -> l.maLo() == lotA).findFirst().orElseThrow().conDuocTra());
        assertThrows(InventoryValidationException.class,
                () -> fx.stockReturnService.approve(ACTOR, ma, LocalDateTime.now()), "Phiếu đã hủy");
        assertEquals(14, stockOf(lotA));
    }

    @Test
    void defectiveGoodsAreRecordedButNotRestocked() {
        String ma = create(ReturnReason.HANG_LOI, new StockReturnLineRequest(lotB, 2)).maPhieu();
        fx.stockReturnService.approve(ACTOR, ma, TODAY.atTime(15, 0));
        assertEquals(7, stockOf(lotB), "Hàng lỗi không quay lại tồn khả dụng");
        assertEquals(VoucherStatus.DA_HOAN_THANH, fx.stockReturnService.getDetail(ADMIN, ma).trangThai());
        assertEquals(1, fx.stockReturnService.returnableLines(ADMIN, px, null).stream()
                .filter(l -> l.maLo() == lotB).findFirst().orElseThrow().conDuocTra());
    }

    @Test
    void validation() {
        assertThrows(InventoryValidationException.class,
                () -> create(ReturnReason.DU_THUA, new StockReturnLineRequest(lotOther, 1)), "Lô không có trên phiếu");
        assertThrows(InventoryValidationException.class, () -> create(ReturnReason.DU_THUA), "Không có dòng");
        assertThrows(InventoryValidationException.class, () -> create(ReturnReason.DU_THUA,
                new StockReturnLineRequest(lotA, 1), new StockReturnLineRequest(lotA, 1)), "Trùng lô");
        assertThrows(InventoryValidationException.class, () -> fx.stockReturnService.create(ACTOR,
                new StockReturnRequest(TODAY.minusDays(5), px, ReturnReason.DU_THUA, "A", "B", null,
                        List.of(new StockReturnLineRequest(lotA, 1))), TODAY.atTime(14, 0)), "Trước ngày xuất");
        assertThrows(InventoryValidationException.class, () -> fx.stockReturnService.create(ACTOR,
                new StockReturnRequest(TODAY.plusDays(1), px, ReturnReason.DU_THUA, "A", "B", null,
                        List.of(new StockReturnLineRequest(lotA, 1))), TODAY.atTime(14, 0)), "Ngày tương lai");
        assertThrows(InventoryValidationException.class, () -> fx.stockReturnService.create(ACTOR,
                new StockReturnRequest(TODAY, px, ReturnReason.DU_THUA, "A", " ", null,
                        List.of(new StockReturnLineRequest(lotA, 1))), TODAY.atTime(14, 0)), "Thiếu người hoàn trả");
        assertThrows(InventoryValidationException.class, () -> fx.stockReturnService.create(ACTOR,
                new StockReturnRequest(TODAY, px, null, "A", "B", null,
                        List.of(new StockReturnLineRequest(lotA, 1))), TODAY.atTime(14, 0)), "Thiếu lý do");
    }

    @Test
    void overviewCountsReturns() {
        fx.stockReturnService.approve(ACTOR, create(ReturnReason.DU_THUA, new StockReturnLineRequest(lotA, 1)).maPhieu(),
                LocalDateTime.now());
        create(ReturnReason.DU_THUA, new StockReturnLineRequest(lotA, 1));
        var overview = fx.overviewService.getOverview(ADMIN);
        assertEquals(1, overview.hoatDongHomNay().hoanTra());
        assertEquals(2, overview.soPhieuChoXuLy(), "1 phiếu xuất chờ + 1 phiếu hoàn trả chờ duyệt");
        var s = fx.stockReturnService.summarize(ADMIN);
        assertEquals(2, s.tongPhieu());
        assertEquals(1, s.soChoDuyet());
        assertEquals(1, s.soHoanThanh());
    }

    @Test
    void permissions() {
        Set<Role> receptionist = Set.of(Role.RECEPTIONIST);
        assertThrows(PermissionDeniedException.class, () -> fx.stockReturnService.list(receptionist));
        assertThrows(PermissionDeniedException.class, () -> fx.stockReturnService.create(
                new InventoryActor(receptionist, "NV0002", "Lễ tân"),
                request(px, ReturnReason.DU_THUA, new StockReturnLineRequest(lotA, 1))));
    }
}
