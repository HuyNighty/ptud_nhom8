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
import com.quanlynhakhoa.features.inventory.dto.request.SupplierRequest;
import com.quanlynhakhoa.features.inventory.dto.response.StockOutDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockOutLineResponse;
import com.quanlynhakhoa.features.inventory.entity.StockLot;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.enums.StockOutReason;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.exception.InventoryValidationException;
import com.quanlynhakhoa.features.inventory.support.InventoryTestFixture;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StockOutServiceTest {
    private static final Set<Role> ADMIN = Set.of(Role.SYSTEM_ADMIN);
    private static final InventoryActor ACTOR = new InventoryActor(ADMIN, "NV0001", "Nguyễn Dần");
    /** TH001 là thuốc KSĐB: người xuất kho phải khác người lập (phê duyệt kép). */
    private static final InventoryActor KEEPER = new InventoryActor(ADMIN, "NV0002", "Trần Thủ Kho");
    private static final LocalDate TODAY = LocalDate.now();

    private final InventoryTestFixture fx = new InventoryTestFixture();
    private int early;
    private int late;
    private int expired;

    @BeforeEach
    void setUp() {
        fx.receipts.addEmployee("NV0001", "Nguyễn Dần");
        fx.receipts.addEmployee("NV0002", "Trần Thủ Kho");
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.THUOC, "TH001", "Thuốc tê", "ống",
                5, null, null, true, null));
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.VAT_TU, "VT001", "Găng tay", "hộp",
                5, null, null, false, null));
        String ncc = fx.stockInService.createSupplier(ADMIN, new SupplierRequest("NCC A", "0901234567", null, null)).ma();
        // Lô đã hết hạn (nhập từ 60 ngày trước, hạn đã qua 5 ngày).
        LocalDate past = TODAY.minusDays(60);
        String old = fx.stockInService.create(ACTOR, new StockInRequest(past, ncc, null, List.of(
                new StockInLineRequest(InventoryItemType.THUOC, "TH001", "OLD", TODAY.minusDays(5), 5,
                        BigDecimal.valueOf(900)))), past.atTime(8, 0)).maPhieu();
        fx.stockInService.complete(ACTOR, old, past.atTime(9, 0));
        String in = fx.stockInService.create(ACTOR, new StockInRequest(TODAY, ncc, null, List.of(
                new StockInLineRequest(InventoryItemType.THUOC, "TH001", "LATE", TODAY.plusDays(200), 20,
                        BigDecimal.valueOf(1200)),
                new StockInLineRequest(InventoryItemType.THUOC, "TH001", "EARLY", TODAY.plusDays(30), 10,
                        BigDecimal.valueOf(1000)),
                new StockInLineRequest(InventoryItemType.VAT_TU, "VT001", "G1", null, 50, BigDecimal.valueOf(85_000)))),
                TODAY.atTime(8, 0)).maPhieu();
        fx.stockInService.complete(ACTOR, in, TODAY.atTime(8, 30));
        for (StockLot lot : fx.lots.all()) {
            switch (lot.getSoLo()) {
                case "EARLY" -> early = lot.getMaLo();
                case "LATE" -> late = lot.getMaLo();
                case "OLD" -> expired = lot.getMaLo();
                default -> { }
            }
        }
    }

    private StockOutDetailResponse create(StockOutReason reason, StockOutLineRequest... lines) {
        return fx.stockOutService.create(ACTOR, new StockOutRequest(TODAY, reason, "BS. Nguyễn Văn B", null,
                List.of(lines)), TODAY.atTime(10, 0));
    }

    private int stockOf(int maLo) {
        return fx.lots.findById(maLo).orElseThrow().getSoLuongTon();
    }

    @Test
    void fefoTakesEarliestExpiryFirstAndSkipsExpiredLots() {
        List<StockOutLineResponse> s = fx.stockOutService.suggestFefo(ADMIN, InventoryItemType.THUOC, "th001", 13, Map.of());
        assertEquals(2, s.size());
        assertEquals("EARLY", s.get(0).soLo());
        assertEquals(10, s.get(0).soLuong());
        assertEquals("LATE", s.get(1).soLo());
        assertEquals(3, s.get(1).soLuong());
        assertTrue(s.stream().noneMatch(l -> l.soLo().equals("OLD")), "Không gợi ý lô đã hết hạn");
    }

    @Test
    void fefoRespectsLinesAlreadyChosenAndReportsShortage() {
        List<StockOutLineResponse> s = fx.stockOutService.suggestFefo(ADMIN, InventoryItemType.THUOC, "TH001", 5,
                Map.of(early, 10));
        assertEquals("LATE", s.get(0).soLo());
        InventoryValidationException e = assertThrows(InventoryValidationException.class,
                () -> fx.stockOutService.suggestFefo(ADMIN, InventoryItemType.THUOC, "TH001", 31, Map.of()));
        assertTrue(e.getMessage().contains("chỉ còn 30"), e.getMessage());
    }

    @Test
    void createKeepsStockAndCompleteDeductsWithCost() {
        StockOutDetailResponse draft = create(StockOutReason.DIEU_TRI,
                new StockOutLineRequest(early, 10), new StockOutLineRequest(late, 3));
        assertEquals("PX001", draft.maPhieu());
        assertEquals(VoucherStatus.CHO_XU_LY, draft.trangThai());
        assertEquals(10, stockOf(early), "Chưa xuất kho thì chưa trừ tồn");
        assertNull(draft.dong().get(0).donGia());

        StockOutDetailResponse done = fx.stockOutService.complete(KEEPER, draft.maPhieu(), TODAY.atTime(11, 0));
        assertEquals(VoucherStatus.DA_HOAN_THANH, done.trangThai());
        assertEquals("Trần Thủ Kho", done.nguoiXuatKho());
        assertEquals(0, stockOf(early));
        assertEquals(17, stockOf(late));
        assertEquals(new BigDecimal("13600"), done.tongTien(), "10×1.000 + 3×1.200 theo giá nhập của từng lô");
        assertEquals(17, fx.itemService.search(ADMIN, "TH001", null).get(0).tonKho());
    }

    @Test
    void secondReceiptCannotOverdrawTheSameLot() {
        String first = create(StockOutReason.DIEU_TRI, new StockOutLineRequest(early, 8)).maPhieu();
        String second = create(StockOutReason.PHUC_VU_PHONG_KHAM, new StockOutLineRequest(early, 5)).maPhieu();
        fx.stockOutService.complete(KEEPER, first, TODAY.atTime(11, 0));
        InventoryValidationException e = assertThrows(InventoryValidationException.class,
                () -> fx.stockOutService.complete(KEEPER, second, TODAY.atTime(11, 5)));
        assertTrue(e.getMessage().contains("chỉ còn 2"), e.getMessage());
        assertEquals(2, stockOf(early), "Không âm tồn, không trừ dở dang");
        assertEquals(VoucherStatus.CHO_XU_LY, fx.stockOutService.getDetail(ADMIN, second).trangThai());
    }

    @Test
    void lotThatExpiresBeforeCompletionIsRejected() {
        String ma = create(StockOutReason.DIEU_TRI, new StockOutLineRequest(early, 1)).maPhieu();
        assertThrows(InventoryValidationException.class,
                () -> fx.stockOutService.complete(KEEPER, ma, TODAY.plusDays(31).atTime(9, 0)));
        assertEquals(10, stockOf(early));
    }

    @Test
    void validation() {
        assertThrows(InventoryValidationException.class, () -> create(StockOutReason.DIEU_TRI), "Không có dòng");
        assertThrows(InventoryValidationException.class,
                () -> create(StockOutReason.DIEU_TRI, new StockOutLineRequest(expired, 1)), "Lô hết hạn");
        assertThrows(InventoryValidationException.class,
                () -> create(StockOutReason.DIEU_TRI, new StockOutLineRequest(early, 11)), "Vượt tồn lô");
        assertThrows(InventoryValidationException.class, () -> create(StockOutReason.DIEU_TRI,
                new StockOutLineRequest(early, 1), new StockOutLineRequest(early, 2)), "Trùng lô");
        assertThrows(InventoryValidationException.class, () -> fx.stockOutService.create(ACTOR,
                new StockOutRequest(TODAY, StockOutReason.DIEU_TRI, "  ", null, List.of(new StockOutLineRequest(early, 1))),
                TODAY.atTime(10, 0)), "Thiếu người nhận");
        assertThrows(InventoryValidationException.class, () -> fx.stockOutService.create(ACTOR,
                new StockOutRequest(TODAY.plusDays(1), StockOutReason.DIEU_TRI, "A", null,
                        List.of(new StockOutLineRequest(early, 1))), TODAY.atTime(10, 0)), "Ngày tương lai");
        assertThrows(InventoryValidationException.class, () -> fx.stockOutService.create(ACTOR,
                new StockOutRequest(TODAY, null, "A", null, List.of(new StockOutLineRequest(early, 1))),
                TODAY.atTime(10, 0)), "Thiếu lý do");
    }

    @Test
    void cancelAndEditRules() {
        String ma = create(StockOutReason.TRA_NCC, new StockOutLineRequest(late, 2)).maPhieu();
        StockOutDetailResponse edited = fx.stockOutService.update(ACTOR, ma, new StockOutRequest(TODAY,
                StockOutReason.HUY_HAO_HUT, "Kho vật tư", "Hỏng bao bì", List.of(new StockOutLineRequest(late, 4))));
        assertEquals(StockOutReason.HUY_HAO_HUT, edited.lyDo());
        assertEquals(4, edited.dong().get(0).soLuong());
        assertThrows(InventoryValidationException.class, () -> fx.stockOutService.cancel(ACTOR, ma, " "));
        StockOutDetailResponse cancelled = fx.stockOutService.cancel(ACTOR, ma, "Lập nhầm");
        assertEquals(VoucherStatus.DA_HUY, cancelled.trangThai());
        assertThrows(InventoryValidationException.class, () -> fx.stockOutService.complete(KEEPER, ma, LocalDateTime.now()));
        assertEquals(20, stockOf(late));

        String done = create(StockOutReason.DIEU_TRI, new StockOutLineRequest(late, 1)).maPhieu();
        fx.stockOutService.complete(KEEPER, done, TODAY.atTime(12, 0));
        assertThrows(InventoryValidationException.class, () -> fx.stockOutService.cancel(ACTOR, done, "x"));
        assertEquals("Đã giao", fx.stockOutService.updateNote(ACTOR, done, "Đã giao").ghiChu());
    }

    @Test
    void summaryAndOverview() {
        fx.stockOutService.complete(KEEPER, create(StockOutReason.DIEU_TRI, new StockOutLineRequest(early, 2)).maPhieu(),
                LocalDateTime.now());
        create(StockOutReason.DIEU_TRI, new StockOutLineRequest(late, 1));
        var s = fx.stockOutService.summarize(ADMIN, TODAY);
        assertEquals(2, s.tongPhieu());
        assertEquals(1, s.soChoXuLy());
        assertEquals(1, s.soHoanThanh());
        assertEquals(new BigDecimal("2000"), s.giaTriThangNay());
        var overview = fx.overviewService.getOverview(ADMIN);
        assertEquals(1, overview.hoatDongHomNay().xuatKho());
        assertEquals(1, overview.soPhieuChoXuLy());
    }

    @Test
    void permissions() {
        Set<Role> receptionist = Set.of(Role.RECEPTIONIST);
        assertThrows(PermissionDeniedException.class, () -> fx.stockOutService.list(receptionist));
        assertThrows(PermissionDeniedException.class, () -> fx.stockOutService.create(
                new InventoryActor(receptionist, "NV0002", "Lễ tân"),
                new StockOutRequest(TODAY, StockOutReason.DIEU_TRI, "A", null, List.of(new StockOutLineRequest(early, 1)))));
    }

    @Test
    void controlledDrugNeedsDoubleApproval() {
        String ma = create(StockOutReason.DIEU_TRI, new StockOutLineRequest(early, 1)).maPhieu();
        InventoryValidationException e = assertThrows(InventoryValidationException.class,
                () -> fx.stockOutService.complete(ACTOR, ma, TODAY.atTime(11, 0)));
        assertTrue(e.getMessage().contains("phê duyệt kép"), e.getMessage());
        assertEquals(10, stockOf(early));
        // Phiếu chỉ có vật tư thường: người lập tự xuất được.
        int gloves = fx.lots.all().stream().filter(l -> l.getSoLo().equals("G1")).findFirst().orElseThrow().getMaLo();
        String plain = create(StockOutReason.PHUC_VU_PHONG_KHAM, new StockOutLineRequest(gloves, 1)).maPhieu();
        assertEquals(VoucherStatus.DA_HOAN_THANH,
                fx.stockOutService.complete(ACTOR, plain, TODAY.atTime(11, 0)).trangThai());
    }
}
