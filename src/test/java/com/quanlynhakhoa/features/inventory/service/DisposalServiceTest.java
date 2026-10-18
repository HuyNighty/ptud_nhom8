package com.quanlynhakhoa.features.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.auth.exception.PermissionDeniedException;
import com.quanlynhakhoa.features.inventory.dto.request.DisposalLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.DisposalRequest;
import com.quanlynhakhoa.features.inventory.dto.request.InventoryItemRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockInLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockInRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockOutLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockOutRequest;
import com.quanlynhakhoa.features.inventory.dto.request.SupplierRequest;
import com.quanlynhakhoa.features.inventory.dto.response.DisposalDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.DisposalLotOption;
import com.quanlynhakhoa.features.inventory.entity.StockLot;
import com.quanlynhakhoa.features.inventory.enums.DisposalReason;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
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

class DisposalServiceTest {
    private static final Set<Role> ADMIN = Set.of(Role.SYSTEM_ADMIN);
    private static final InventoryActor REQUESTER = new InventoryActor(ADMIN, "NV0001", "Nguyễn Văn A");
    private static final InventoryActor APPROVER = new InventoryActor(ADMIN, "NV0002", "Nguyễn Dần");
    private static final LocalDate TODAY = LocalDate.now();

    private final InventoryTestFixture fx = new InventoryTestFixture();
    private int expired;
    private int good;
    private int gloves;

    @BeforeEach
    void setUp() {
        fx.receipts.addEmployee("NV0001", "Nguyễn Văn A");
        fx.receipts.addEmployee("NV0002", "Nguyễn Dần");
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.THUOC, "TH001", "Thuốc tê", "ống",
                5, null, null, false, null));
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.VAT_TU, "VT001", "Găng tay", "hộp",
                5, null, null, false, null));
        String ncc = fx.stockInService.createSupplier(ADMIN, new SupplierRequest("NCC A", "0901234567", null, null)).ma();
        LocalDate d = TODAY.minusDays(60);
        String in = fx.stockInService.create(REQUESTER, new StockInRequest(d, ncc, null, List.of(
                new StockInLineRequest(InventoryItemType.THUOC, "TH001", "OLD", TODAY.minusDays(3), 10,
                        BigDecimal.valueOf(12000)),
                new StockInLineRequest(InventoryItemType.THUOC, "TH001", "NEW", TODAY.plusDays(300), 20,
                        BigDecimal.valueOf(13000)),
                new StockInLineRequest(InventoryItemType.VAT_TU, "VT001", "G1", null, 30, BigDecimal.valueOf(85000)))),
                d.atTime(8, 0)).maPhieu();
        fx.stockInService.complete(REQUESTER, in, d.atTime(9, 0));
        for (StockLot lot : fx.lots.all()) {
            switch (lot.getSoLo()) {
                case "OLD" -> expired = lot.getMaLo();
                case "NEW" -> good = lot.getMaLo();
                default -> gloves = lot.getMaLo();
            }
        }
    }

    private DisposalDetailResponse create(DisposalReason reason, String moTa, DisposalLineRequest... lines) {
        return fx.disposalService.create(REQUESTER, new DisposalRequest(TODAY, reason, moTa, List.of(lines)),
                TODAY.atTime(10, 0));
    }

    private int stockOf(int maLo) {
        return fx.lots.findById(maLo).orElseThrow().getSoLuongTon();
    }

    @Test
    void expiredLotsComeFirstWithCost() {
        List<DisposalLotOption> lots = fx.disposalService.availableLots(ADMIN);
        assertEquals("OLD", lots.get(0).soLo());
        assertTrue(lots.get(0).hetHan());
        assertEquals(new BigDecimal("12000"), lots.get(0).donGia());
        assertEquals(3, lots.size());
    }

    @Test
    void pendingKeepsStockApproveDeducts() {
        DisposalDetailResponse draft = create(DisposalReason.QUA_HAN, null,
                new DisposalLineRequest(expired, 10), new DisposalLineRequest(gloves, 1));
        assertEquals("HH001", draft.maPhieu());
        assertEquals(VoucherStatus.CHO_XU_LY, draft.trangThai());
        assertEquals(new BigDecimal("205000"), draft.giaTri(), "10×12.000 + 1×85.000, có ngay khi chờ duyệt");
        assertEquals(10, stockOf(expired), "Chưa duyệt thì chưa trừ tồn");
        assertNull(fx.disposalService.list(ADMIN).get(0).nguoiDuyet());
        assertEquals("Quá hạn sử dụng", fx.disposalService.list(ADMIN).get(0).lyDoHienThi());

        DisposalDetailResponse done = fx.disposalService.approve(APPROVER, draft.maPhieu(), TODAY.atTime(11, 0));
        assertEquals(VoucherStatus.DA_HOAN_THANH, done.trangThai());
        assertEquals("Nguyễn Dần", done.nguoiDuyet());
        assertEquals(0, stockOf(expired));
        assertEquals(29, stockOf(gloves));
        var s = fx.disposalService.summarize(ADMIN, TODAY);
        assertEquals(new BigDecimal("205000"), s.giaTriThangNay());
        assertEquals(1, s.soHoanThanh());
    }

    @Test
    void approverMustDifferAndStockIsRechecked() {
        String ma = create(DisposalReason.HONG_HOC, "Hỏng hóc do đổ vỡ", new DisposalLineRequest(good, 15)).maPhieu();
        InventoryValidationException same = assertThrows(InventoryValidationException.class,
                () -> fx.disposalService.approve(REQUESTER, ma, LocalDateTime.now()));
        assertTrue(same.getMessage().contains("khác người lập"), same.getMessage());
        // Xuất bớt trước khi duyệt → không đủ tồn để hủy 15.
        String px = fx.stockOutService.create(REQUESTER, new StockOutRequest(TODAY, StockOutReason.DIEU_TRI, "BS. B",
                null, List.of(new StockOutLineRequest(good, 10))), TODAY.atTime(9, 0)).maPhieu();
        fx.stockOutService.complete(REQUESTER, px, TODAY.atTime(9, 5));
        InventoryValidationException e = assertThrows(InventoryValidationException.class,
                () -> fx.disposalService.approve(APPROVER, ma, LocalDateTime.now()));
        assertTrue(e.getMessage().contains("chỉ còn 10"), e.getMessage());
        assertEquals(10, stockOf(good));
        assertEquals("Hỏng hóc do đổ vỡ", fx.disposalService.list(ADMIN).get(0).lyDoHienThi());
    }

    @Test
    void editAndCancel() {
        String ma = create(DisposalReason.HAO_HUT, null, new DisposalLineRequest(gloves, 2)).maPhieu();
        DisposalDetailResponse edited = fx.disposalService.update(REQUESTER, ma, new DisposalRequest(TODAY,
                DisposalReason.KHAC, "Chuột cắn bao bì", List.of(new DisposalLineRequest(gloves, 3))));
        assertEquals(3, edited.dong().get(0).soLuong());
        assertThrows(InventoryValidationException.class, () -> fx.disposalService.cancel(APPROVER, ma, " "));
        assertEquals(VoucherStatus.DA_HUY, fx.disposalService.cancel(APPROVER, ma, "Tìm thấy hàng").trangThai());
        assertThrows(InventoryValidationException.class,
                () -> fx.disposalService.approve(APPROVER, ma, LocalDateTime.now()));
        assertThrows(InventoryValidationException.class, () -> fx.disposalService.update(REQUESTER, ma,
                new DisposalRequest(TODAY, DisposalReason.KHAC, null, List.of(new DisposalLineRequest(gloves, 1)))));
        assertEquals(30, stockOf(gloves));
    }

    @Test
    void validation() {
        assertThrows(InventoryValidationException.class, () -> create(DisposalReason.QUA_HAN, null), "Không có dòng");
        assertThrows(InventoryValidationException.class,
                () -> create(DisposalReason.QUA_HAN, null, new DisposalLineRequest(expired, 11)), "Vượt tồn");
        assertThrows(InventoryValidationException.class, () -> create(DisposalReason.QUA_HAN, null,
                new DisposalLineRequest(expired, 1), new DisposalLineRequest(expired, 1)), "Trùng lô");
        assertThrows(InventoryValidationException.class, () -> fx.disposalService.create(REQUESTER,
                new DisposalRequest(TODAY.plusDays(1), DisposalReason.QUA_HAN, null,
                        List.of(new DisposalLineRequest(expired, 1))), TODAY.atTime(10, 0)), "Ngày tương lai");
        assertThrows(InventoryValidationException.class, () -> fx.disposalService.create(REQUESTER,
                new DisposalRequest(TODAY, null, null, List.of(new DisposalLineRequest(expired, 1))),
                TODAY.atTime(10, 0)), "Thiếu lý do");
    }

    @Test
    void overviewAndPermissions() {
        fx.disposalService.approve(APPROVER, create(DisposalReason.QUA_HAN, null,
                new DisposalLineRequest(expired, 1)).maPhieu(), LocalDateTime.now());
        create(DisposalReason.HONG_HOC, null, new DisposalLineRequest(gloves, 1));
        var overview = fx.overviewService.getOverview(ADMIN);
        assertEquals(1, overview.hoatDongHomNay().huyHaoHut());
        assertEquals(1, overview.soPhieuChoXuLy());
        assertTrue(overview.canChuY().stream().anyMatch(n -> n.noiDung().contains("hủy / hao hụt")));
        Set<Role> receptionist = Set.of(Role.RECEPTIONIST);
        assertThrows(PermissionDeniedException.class, () -> fx.disposalService.list(receptionist));
        assertThrows(PermissionDeniedException.class, () -> fx.disposalService.create(
                new InventoryActor(receptionist, "NV0003", "Lễ tân"),
                new DisposalRequest(TODAY, DisposalReason.QUA_HAN, null, List.of(new DisposalLineRequest(expired, 1)))));
    }
}
