package com.quanlynhakhoa.features.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import com.quanlynhakhoa.features.inventory.dto.response.ControlledDrugRowResponse;
import com.quanlynhakhoa.features.inventory.dto.response.ControlledLedgerEntry;
import com.quanlynhakhoa.features.inventory.entity.StockLot;
import com.quanlynhakhoa.features.inventory.enums.ControlledDrugGroup;
import com.quanlynhakhoa.features.inventory.enums.DisposalReason;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.enums.NoticeSeverity;
import com.quanlynhakhoa.features.inventory.enums.StockOutReason;
import com.quanlynhakhoa.features.inventory.exception.InventoryValidationException;
import com.quanlynhakhoa.features.inventory.support.InventoryTestFixture;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ControlledDrugServiceTest {
    private static final Set<Role> ADMIN = Set.of(Role.SYSTEM_ADMIN);
    private static final InventoryActor A = new InventoryActor(ADMIN, "NV0001", "Nguyễn Văn A");
    private static final InventoryActor B = new InventoryActor(ADMIN, "NV0002", "Nguyễn Dần");
    private static final LocalDate TODAY = LocalDate.now();

    private final InventoryTestFixture fx = new InventoryTestFixture();
    private int diazepam;
    private int fentanylOld;

    @BeforeEach
    void setUp() {
        fx.receipts.addEmployee("NV0001", "Nguyễn Văn A");
        fx.receipts.addEmployee("NV0002", "Nguyễn Dần");
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.THUOC, "TH001", "Diazepam", "Ống",
                10, "5mg/2ml", "Diazepam", true, null));
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.THUOC, "TH002", "Fentanyl", "Ống",
                10, "0.1mg/2ml", "Fentanyl", true, null));
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.THUOC, "TH003", "Paracetamol", "Viên",
                10, null, null, false, null));
        String ncc = fx.stockInService.createSupplier(ADMIN, new SupplierRequest("Dược Trung ương", "0901234567", null,
                null)).ma();
        LocalDate d = TODAY.minusDays(30);
        String in = fx.stockInService.create(A, new StockInRequest(d, ncc, null, List.of(
                new StockInLineRequest(InventoryItemType.THUOC, "TH001", "DZ1", TODAY.plusDays(400), 35, BigDecimal.TEN),
                new StockInLineRequest(InventoryItemType.THUOC, "TH002", "FT0", TODAY.plusDays(5), 4, BigDecimal.TEN),
                new StockInLineRequest(InventoryItemType.THUOC, "TH003", "P1", TODAY.plusDays(400), 50, BigDecimal.ONE))),
                d.atTime(8, 0)).maPhieu();
        fx.stockInService.complete(A, in, d.atTime(9, 0));
        for (StockLot l : fx.lots.all()) {
            if (l.getSoLo().equals("DZ1")) {
                diazepam = l.getMaLo();
            } else if (l.getSoLo().equals("FT0")) {
                fentanylOld = l.getMaLo();
            }
        }
    }

    private ControlledDrugRowResponse row(String ma) {
        return fx.controlledDrugService.list(ADMIN).stream().filter(r -> r.maThuoc().equals(ma)).findFirst().orElseThrow();
    }

    @Test
    void listsOnlyControlledDrugsWithStatus() {
        List<ControlledDrugRowResponse> rows = fx.controlledDrugService.list(ADMIN);
        assertEquals(List.of("TH001", "TH002"), rows.stream().map(ControlledDrugRowResponse::maThuoc).toList());
        ControlledDrugRowResponse dz = row("TH001");
        assertEquals("An toàn", dz.trangThai());
        assertEquals(NoticeSeverity.INFO, dz.mucDo());
        assertEquals("Diazepam 5mg/2ml", dz.tenDayDu());
        ControlledDrugRowResponse ft = row("TH002");
        assertEquals("Thiếu/Cận hạn", ft.trangThai(), "4 < định mức 10 và còn 5 ngày");
        assertEquals(NoticeSeverity.CRITICAL, ft.mucDo());
        var s = fx.controlledDrugService.summarize(ADMIN);
        assertEquals(2, s.tongLoai());
        assertEquals(39, s.tongTon());
        assertEquals(1, s.canHanHetHan());
    }

    @Test
    void classifyGroup() {
        assertEquals(ControlledDrugGroup.HUONG_THAN,
                fx.controlledDrugService.classify(B, "th001", ControlledDrugGroup.HUONG_THAN).thuoc().nhom());
        assertEquals(ControlledDrugGroup.HUONG_THAN, row("TH001").nhom());
        assertNull(row("TH002").nhom());
        assertThrows(InventoryValidationException.class,
                () -> fx.controlledDrugService.classify(B, "TH003", ControlledDrugGroup.TIEN_CHAT), "Không phải KSĐB");
        assertThrows(PermissionDeniedException.class, () -> fx.controlledDrugService.classify(
                new InventoryActor(Set.of(Role.RECEPTIONIST), "NV0003", "Lễ tân"), "TH001", ControlledDrugGroup.TIEN_CHAT));
    }

    @Test
    void ledgerTracksEveryMovementWithRunningBalanceAndPendingCount() {
        String px = fx.stockOutService.create(A, new StockOutRequest(TODAY, StockOutReason.DIEU_TRI, "BS. Trần Bình",
                null, List.of(new StockOutLineRequest(diazepam, 5))), TODAY.atTime(9, 0)).maPhieu();
        assertEquals(1, fx.controlledDrugService.summarize(ADMIN).choDuyet(), "Phiếu xuất chờ có thuốc KSĐB");
        fx.stockOutService.complete(B, px, TODAY.atTime(10, 0));
        String hh = fx.disposalService.create(A, new DisposalRequest(TODAY, DisposalReason.QUA_HAN, null,
                List.of(new DisposalLineRequest(fentanylOld, 1)))).maPhieu();
        fx.disposalService.approve(B, hh);
        assertEquals(0, fx.controlledDrugService.summarize(ADMIN).choDuyet());

        List<ControlledLedgerEntry> all = fx.controlledDrugService.ledger(ADMIN, null);
        assertEquals(4, all.size(), "2 dòng nhập + 1 xuất + 1 hủy, không có paracetamol");
        List<ControlledLedgerEntry> dz = fx.controlledDrugService.ledger(ADMIN, "TH001");
        assertEquals(2, dz.size());
        assertEquals(35, dz.get(0).nhap());
        assertEquals(35, dz.get(0).tonSau());
        assertEquals("Xuất kho", dz.get(1).loai());
        assertEquals(5, dz.get(1).xuat());
        assertEquals(30, dz.get(1).tonSau());
        assertEquals("Nguyễn Dần", dz.get(1).nguoiThucHien());
        assertEquals("Nhận: BS. Trần Bình (Điều trị)", dz.get(1).doiTac());
        ControlledLedgerEntry disposal = fx.controlledDrugService.ledger(ADMIN, "TH002").get(1);
        assertEquals("Hủy", disposal.loai());
        assertEquals(3, disposal.tonSau());
        assertThrows(InventoryValidationException.class, () -> fx.controlledDrugService.ledger(ADMIN, "TH003"));
    }
}
