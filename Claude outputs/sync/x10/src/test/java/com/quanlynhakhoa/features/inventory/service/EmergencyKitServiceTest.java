package com.quanlynhakhoa.features.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.auth.exception.PermissionDeniedException;
import com.quanlynhakhoa.features.inventory.dto.request.EmergencyKitItemRequest;
import com.quanlynhakhoa.features.inventory.dto.request.EmergencyKitRequest;
import com.quanlynhakhoa.features.inventory.dto.request.InventoryItemRequest;
import com.quanlynhakhoa.features.inventory.dto.request.KitCheckLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.KitCheckRequest;
import com.quanlynhakhoa.features.inventory.dto.request.KitRestockLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.KitRestockRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockInLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockInRequest;
import com.quanlynhakhoa.features.inventory.dto.request.SupplierRequest;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitItemResponse;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitLogResponse;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitRowResponse;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitSummaryResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockOutDetailResponse;
import com.quanlynhakhoa.features.inventory.entity.StockLot;
import com.quanlynhakhoa.features.inventory.enums.EmergencyKitStatus;
import com.quanlynhakhoa.features.inventory.enums.EmergencyKitType;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.enums.KitCheckResult;
import com.quanlynhakhoa.features.inventory.enums.KitLogType;
import com.quanlynhakhoa.features.inventory.enums.StockOutReason;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.exception.InventoryValidationException;
import com.quanlynhakhoa.features.inventory.support.InventoryTestFixture;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmergencyKitServiceTest {
    private static final Set<Role> ADMIN = Set.of(Role.SYSTEM_ADMIN);
    private static final InventoryActor A = new InventoryActor(ADMIN, "NV0001", "Nguyễn Văn A");
    private static final LocalDate TODAY = LocalDate.now();

    private final InventoryTestFixture fx = new InventoryTestFixture();
    private int adrenalineGood;
    private int adrenalineNearExpiry;

    @BeforeEach
    void setUp() {
        fx.receipts.addEmployee("NV0001", "Nguyễn Văn A");
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.THUOC, "TH001", "Adrenaline", "Ống",
                5, "1mg/1ml", "Epinephrine", false, null));
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.THUOC, "TH002", "Solu-Medrol", "Lọ",
                2, "40mg", "Methylprednisolone", false, null));
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.THUOC, "TH003", "Diazepam", "Ống",
                2, "10mg/2ml", "Diazepam", true, null));
        fx.itemService.create(ADMIN, new InventoryItemRequest(InventoryItemType.VAT_TU, "VT001", "Bơm tiêm 5ml", "Cái",
                10, null, null, false, null));
        String ncc = fx.stockInService.createSupplier(ADMIN, new SupplierRequest("Dược Trung ương", "0901234567", null,
                null)).ma();
        LocalDate d = TODAY.minusDays(10);
        String in = fx.stockInService.create(A, new StockInRequest(d, ncc, null, List.of(
                new StockInLineRequest(InventoryItemType.THUOC, "TH001", "AD1", TODAY.plusDays(400), 20, BigDecimal.TEN),
                new StockInLineRequest(InventoryItemType.THUOC, "TH001", "AD0", TODAY.plusDays(10), 5, BigDecimal.TEN),
                new StockInLineRequest(InventoryItemType.THUOC, "TH002", "SM1", TODAY.plusDays(300), 3, BigDecimal.TEN),
                new StockInLineRequest(InventoryItemType.THUOC, "TH003", "DZ1", TODAY.plusDays(400), 10, BigDecimal.TEN),
                new StockInLineRequest(InventoryItemType.VAT_TU, "VT001", "BT1", TODAY.plusDays(700), 50, BigDecimal.ONE))),
                d.atTime(8, 0)).maPhieu();
        fx.stockInService.complete(A, in, d.atTime(9, 0));
        for (StockLot l : fx.lots.all()) {
            if (l.getSoLo().equals("AD1")) {
                adrenalineGood = l.getMaLo();
            } else if (l.getSoLo().equals("AD0")) {
                adrenalineNearExpiry = l.getMaLo();
            }
        }
    }

    private static EmergencyKitRequest kit(String ten, EmergencyKitItemRequest... items) {
        return new EmergencyKitRequest(ten, EmergencyKitType.CHONG_SOC, "Phòng điều trị 1", 7, 30, null,
                List.of(items));
    }

    private static EmergencyKitItemRequest thuoc(String ma, int min) {
        return new EmergencyKitItemRequest(InventoryItemType.THUOC, ma, min);
    }

    private static EmergencyKitItemRequest vatTu(String ma, int min) {
        return new EmergencyKitItemRequest(InventoryItemType.VAT_TU, ma, min);
    }

    private EmergencyKitRowResponse row(String ma) {
        return fx.emergencyKitService.list(ADMIN).stream().filter(r -> r.maHop().equals(ma)).findFirst().orElseThrow();
    }

    private static EmergencyKitItemResponse item(EmergencyKitDetailResponse d, String ma) {
        return d.matHang().stream().filter(i -> i.maMatHang().equals(ma)).findFirst().orElseThrow();
    }

    private static KitCheckRequest counts(EmergencyKitDetailResponse d, Object... codeQtyExpiry) {
        List<KitCheckLineRequest> lines = new ArrayList<>();
        for (int i = 0; i < codeQtyExpiry.length; i += 3) {
            lines.add(new KitCheckLineRequest(item(d, (String) codeQtyExpiry[i]).maChiTiet(),
                    (Integer) codeQtyExpiry[i + 1], (LocalDate) codeQtyExpiry[i + 2]));
        }
        return new KitCheckRequest(lines, null);
    }

    private int lotStock(int maLo) {
        return fx.lots.findById(maLo).orElseThrow().getSoLuongTon();
    }

    @Test
    void newKitGetsSequentialCodeAndStartsShortWhileStockCanCoverIt() {
        EmergencyKitDetailResponse d = fx.emergencyKitService.create(A, kit("Hộp chống sốc - Đội A",
                thuoc("th001", 2), vatTu("VT001", 3)));
        assertEquals("HTCC-01", d.maHop());
        assertEquals("HTCC-02", fx.emergencyKitService.create(A, kit("Hộp B", thuoc("TH002", 1))).maHop());

        EmergencyKitRowResponse r = row("HTCC-01");
        assertEquals(EmergencyKitStatus.THIEU, r.trangThai());
        assertEquals(0, r.soMatHangDat());
        assertEquals(2, r.soMatHang());
        assertNull(r.kiemTraGanNhat());
        assertTrue(r.denHanKiemTra(), "Hộp chưa kiểm tra lần nào phải đến hạn kiểm tra");
        assertEquals(20, item(d, "TH001").tonKhoDung(), "Lô còn hạn 10 ngày không được tính để bổ sung");
    }

    @Test
    void rejectsInvalidKits() {
        assertThrows(InventoryValidationException.class, () -> fx.emergencyKitService.create(A, kit("Hộp rỗng")));
        assertThrows(InventoryValidationException.class, () -> fx.emergencyKitService.create(A,
                kit("Trùng", thuoc("TH001", 1), thuoc("TH001", 2))));
        assertThrows(InventoryValidationException.class, () -> fx.emergencyKitService.create(A,
                kit("Không có", thuoc("TH999", 1))));
        assertThrows(InventoryValidationException.class, () -> fx.emergencyKitService.create(A,
                kit("  ", thuoc("TH001", 1))));
        assertThrows(InventoryValidationException.class, () -> fx.emergencyKitService.create(A,
                kit("Số âm", thuoc("TH001", 0))));
    }

    @Test
    void checkRecordsCountsAndPassesWhenEveryItemIsEnoughAndFarFromExpiry() {
        EmergencyKitDetailResponse d = fx.emergencyKitService.create(A, kit("Hộp A", thuoc("TH001", 2),
                vatTu("VT001", 3)));
        InventoryValidationException missing = assertThrows(InventoryValidationException.class,
                () -> fx.emergencyKitService.check(A, d.maHop(), counts(d, "TH001", 2, TODAY.plusDays(200))));
        assertTrue(missing.getMessage().contains("Chưa nhập kết quả cho mục Bơm tiêm 5ml"), missing.getMessage());
        assertThrows(InventoryValidationException.class, () -> fx.emergencyKitService.check(A, d.maHop(),
                counts(d, "TH001", 2, null, "VT001", 3, null)), "Thuốc còn trong hộp phải có hạn dùng");

        EmergencyKitDetailResponse after = fx.emergencyKitService.check(A, d.maHop(),
                counts(d, "TH001", 2, TODAY.plusDays(200), "VT001", 3, null));
        assertEquals(EmergencyKitStatus.AN_TOAN, after.trangThai());
        assertFalse(after.denHanKiemTra());
        assertEquals(TODAY.plusDays(7), after.hanKiemTraTiep());
        assertEquals("Nguyễn Văn A", after.nguoiKiemTraGanNhat());
        EmergencyKitLogResponse log = after.nhatKy().get(0);
        assertEquals(KitLogType.KIEM_TRA, log.loai());
        assertEquals(Boolean.TRUE, log.dat());
        assertEquals(List.of(KitCheckResult.DAT, KitCheckResult.DAT),
                log.dong().stream().map(l -> l.ketQua()).toList());
        assertEquals(2, row(d.maHop()).soMatHangDat());
        assertEquals(20, lotStock(adrenalineGood), "Kiểm tra không được đụng tới tồn kho");
    }

    @Test
    void nearExpiryItemFailsCheckAndRaisesWarning() {
        EmergencyKitDetailResponse d = fx.emergencyKitService.create(A, kit("Hộp C", thuoc("TH001", 2)));
        EmergencyKitDetailResponse after = fx.emergencyKitService.check(A, d.maHop(),
                counts(d, "TH001", 2, TODAY.plusDays(20)));
        assertEquals(EmergencyKitStatus.CAN_HAN, after.trangThai());
        assertEquals(Boolean.FALSE, after.nhatKy().get(0).dat());
        assertEquals(KitCheckResult.CAN_HAN, item(after, "TH001").danhGia());

        EmergencyKitSummaryResponse s = fx.emergencyKitService.summarize(ADMIN);
        assertEquals(1, s.hopCanHetHan());
        assertEquals(0, s.hopCanBoSung());
        assertTrue(s.canhBao().get(0).startsWith("Hộp " + d.maHop() + " chứa Adrenaline 1mg/1ml sắp hết hạn"),
                s.canhBao().toString());
    }

    @Test
    void restockIssuesFromLotsBeyondThresholdThroughACompletedStockOut() {
        EmergencyKitDetailResponse d = fx.emergencyKitService.create(A, kit("Hộp A", thuoc("TH001", 2),
                vatTu("VT001", 3)));
        EmergencyKitDetailResponse after = fx.emergencyKitService.restock(A, d.maHop(), new KitRestockRequest(
                KitLogType.BO_SUNG, List.of(new KitRestockLineRequest(item(d, "TH001").maChiTiet(), 2),
                new KitRestockLineRequest(item(d, "VT001").maChiTiet(), 3)), "Bổ sung sau ca cấp cứu"));

        assertEquals(18, lotStock(adrenalineGood));
        assertEquals(5, lotStock(adrenalineNearExpiry), "Không lấy lô còn hạn ít hơn ngưỡng của hộp");
        assertEquals(2, item(after, "TH001").soLuongHienCo());
        assertEquals(TODAY.plusDays(400), item(after, "TH001").hanDung());
        assertEquals(EmergencyKitStatus.AN_TOAN, after.trangThai());

        EmergencyKitLogResponse log = after.nhatKy().get(0);
        assertEquals(KitLogType.BO_SUNG, log.loai());
        assertNull(log.dat());
        assertNotNull(log.maPhieuXuat());
        StockOutDetailResponse out = fx.stockOutService.getDetail(ADMIN, log.maPhieuXuat());
        assertEquals(VoucherStatus.DA_HOAN_THANH, out.trangThai());
        assertEquals(StockOutReason.PHUC_VU_PHONG_KHAM, out.lyDo());
        assertTrue(out.nguoiNhan().startsWith("Hộp cấp cứu " + d.maHop()));
        assertEquals(2, out.dong().size());
    }

    @Test
    void restockFailsWithoutSideEffectsWhenStockIsShort() {
        EmergencyKitDetailResponse d = fx.emergencyKitService.create(A, kit("Hộp D", thuoc("TH002", 5)));
        assertEquals(EmergencyKitStatus.CHO_BO_SUNG, row(d.maHop()).trangThai(),
                "Kho chỉ có 3 lọ — hộp phải ở trạng thái chờ bổ sung");
        InventoryValidationException e = assertThrows(InventoryValidationException.class,
                () -> fx.emergencyKitService.restock(A, d.maHop(), new KitRestockRequest(KitLogType.BO_SUNG,
                        List.of(new KitRestockLineRequest(item(d, "TH002").maChiTiet(), 5)), null)));
        assertTrue(e.getMessage().startsWith("Kho chỉ còn 3 lọ Solu-Medrol 40mg"), e.getMessage());
        assertTrue(fx.stockOutService.list(ADMIN).isEmpty());
        assertTrue(fx.emergencyKitService.logs(ADMIN, null).isEmpty());
    }

    @Test
    void controlledDrugCannotBeRestockedDirectly() {
        EmergencyKitDetailResponse d = fx.emergencyKitService.create(A, kit("Hộp E", thuoc("TH003", 1)));
        InventoryValidationException e = assertThrows(InventoryValidationException.class,
                () -> fx.emergencyKitService.restock(A, d.maHop(), new KitRestockRequest(KitLogType.BO_SUNG,
                        List.of(new KitRestockLineRequest(item(d, "TH003").maChiTiet(), 1)), null)));
        assertTrue(e.getMessage().contains("kiểm soát đặc biệt"), e.getMessage());
        assertTrue(fx.stockOutService.list(ADMIN).isEmpty());
    }

    @Test
    void replaceSwapsNearExpiryUnitsForFreshOnes() {
        EmergencyKitDetailResponse d = fx.emergencyKitService.create(A, kit("Hộp C", thuoc("TH001", 2)));
        fx.emergencyKitService.check(A, d.maHop(), counts(d, "TH001", 2, TODAY.plusDays(5)));
        EmergencyKitDetailResponse after = fx.emergencyKitService.restock(A, d.maHop(), new KitRestockRequest(
                KitLogType.THAY_THE, List.of(new KitRestockLineRequest(item(d, "TH001").maChiTiet(), 2)), null));
        assertEquals(2, item(after, "TH001").soLuongHienCo(), "Thay thế không cộng dồn hàng cũ");
        assertEquals(TODAY.plusDays(400), item(after, "TH001").hanDung());
        assertEquals(EmergencyKitStatus.AN_TOAN, after.trangThai());
        assertEquals(18, lotStock(adrenalineGood));
        assertEquals(List.of(KitLogType.THAY_THE, KitLogType.KIEM_TRA),
                fx.emergencyKitService.logs(ADMIN, d.maHop()).stream().map(EmergencyKitLogResponse::loai).toList());
    }

    @Test
    void editingCompositionKeepsCountsOfRemainingItems() {
        EmergencyKitDetailResponse d = fx.emergencyKitService.create(A, kit("Hộp A", thuoc("TH001", 2),
                vatTu("VT001", 3)));
        fx.emergencyKitService.check(A, d.maHop(), counts(d, "TH001", 2, TODAY.plusDays(200), "VT001", 3, null));
        EmergencyKitDetailResponse edited = fx.emergencyKitService.update(A, d.maHop(),
                new EmergencyKitRequest("Hộp A (sửa)", EmergencyKitType.CO_BAN, "Sảnh chờ", 14, 45, "Ghi chú",
                        List.of(thuoc("TH001", 3), thuoc("TH002", 1))));
        assertEquals("Hộp A (sửa)", edited.tenHop());
        assertEquals(EmergencyKitType.CO_BAN, edited.loai());
        assertEquals(2, edited.matHang().size());
        assertEquals(2, item(edited, "TH001").soLuongHienCo());
        assertEquals(3, item(edited, "TH001").soLuongToiThieu());
        assertEquals(0, item(edited, "TH002").soLuongHienCo());
        assertEquals(EmergencyKitStatus.THIEU, edited.trangThai());
        assertEquals(14, edited.chuKyKiemTraNgay());
    }

    @Test
    void evaluateUsesThresholdStrictly() {
        assertEquals(KitCheckResult.CAN_HAN, EmergencyKitService.evaluate(2, TODAY.plusDays(30), 2, 30, TODAY));
        assertEquals(KitCheckResult.DAT, EmergencyKitService.evaluate(2, TODAY.plusDays(31), 2, 30, TODAY));
        assertEquals(KitCheckResult.HET_HAN, EmergencyKitService.evaluate(2, TODAY.minusDays(1), 2, 30, TODAY));
        assertEquals(KitCheckResult.THIEU, EmergencyKitService.evaluate(1, TODAY.plusDays(90), 2, 30, TODAY));
        assertEquals(KitCheckResult.THIEU, EmergencyKitService.evaluate(0, null, 2, 30, TODAY));
    }

    @Test
    void readersWithoutInventoryPermissionAreRejected() {
        Set<Role> receptionist = Set.of(Role.RECEPTIONIST);
        assertThrows(PermissionDeniedException.class, () -> fx.emergencyKitService.list(receptionist));
        assertThrows(PermissionDeniedException.class, () -> fx.emergencyKitService.create(
                new InventoryActor(receptionist, "NV0003", "Lễ tân"), kit("Hộp", thuoc("TH001", 1))));
    }
}
