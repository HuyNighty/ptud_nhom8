package com.quanlynhakhoa.features.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.auth.exception.PermissionDeniedException;
import com.quanlynhakhoa.features.auth.service.AuthorizationService;
import com.quanlynhakhoa.features.auth.service.RolePermissionPolicy;
import com.quanlynhakhoa.features.inventory.dto.request.InventoryItemRequest;
import com.quanlynhakhoa.features.inventory.dto.response.InventoryItemResponse;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.exception.InventoryValidationException;
import com.quanlynhakhoa.features.inventory.support.InMemoryMedicalSupplyRepository;
import com.quanlynhakhoa.features.inventory.support.InMemoryMedicineRepository;
import com.quanlynhakhoa.features.inventory.support.InMemoryStockLotRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit test không cần Spring/SQL Server: repository là bản giả trong bộ nhớ. */
class InventoryItemServiceTest {
    private static final Set<Role> ADMIN = Set.of(Role.SYSTEM_ADMIN);

    private final InMemoryMedicineRepository medicines = new InMemoryMedicineRepository();
    private final InMemoryMedicalSupplyRepository supplies = new InMemoryMedicalSupplyRepository();
    private final InventoryItemService service = new InventoryItemService(
            new AuthorizationService(new RolePermissionPolicy()), medicines, supplies, new InMemoryStockLotRepository());

    private static InventoryItemRequest thuoc(String ma, String ten) {
        return new InventoryItemRequest(InventoryItemType.THUOC, ma, ten, "ống", 10,
                "2%", "Lidocaine", true, "bị bỏ qua");
    }

    private static InventoryItemRequest vatTu(String ma, String ten) {
        return new InventoryItemRequest(InventoryItemType.VAT_TU, ma, ten, "hộp", 50,
                "bị bỏ qua", "bị bỏ qua", true, "Size M");
    }

    @Test
    void createMedicineNormalizesCodeAndDropsSupplyOnlyFields() {
        InventoryItemResponse r = service.create(ADMIN, thuoc("  th001 ", "  Thuốc tê A "));
        assertEquals("TH001", r.ma());
        assertEquals("Thuốc tê A", r.ten());
        assertTrue(r.kiemSoatDacBiet());
        assertNull(r.moTa());
    }

    @Test
    void createSupplyDropsMedicineOnlyFields() {
        InventoryItemResponse r = service.create(ADMIN, vatTu("VT001", "Găng tay y tế"));
        assertNull(r.hamLuong());
        assertNull(r.hoatChat());
        assertEquals(false, r.kiemSoatDacBiet());
        assertEquals("Size M", r.moTa());
    }

    @Test
    void duplicateCodeIsRejected() {
        service.create(ADMIN, thuoc("TH001", "A"));
        InventoryValidationException e = assertThrows(InventoryValidationException.class,
                () -> service.create(ADMIN, thuoc("TH001", "B")));
        assertTrue(e.getMessage().contains("đã tồn tại"));
    }

    @Test
    void codeMustMatchTypePrefixAndDigits() {
        assertThrows(InventoryValidationException.class, () -> service.create(ADMIN, thuoc("VT001", "A")));
        assertThrows(InventoryValidationException.class, () -> service.create(ADMIN, thuoc("TH01", "A")));
        assertThrows(InventoryValidationException.class, () -> service.create(ADMIN, thuoc("TH12345678X", "A")));
        assertThrows(InventoryValidationException.class, () -> service.create(ADMIN, vatTu("VTABC", "A")));
    }

    @Test
    void requiredFieldsAndLimits() {
        assertThrows(InventoryValidationException.class, () -> service.create(ADMIN, thuoc("TH001", "   ")));
        assertThrows(InventoryValidationException.class, () -> service.create(ADMIN,
                new InventoryItemRequest(InventoryItemType.VAT_TU, "VT001", "A", " ", 1, null, null, false, null)));
        assertThrows(InventoryValidationException.class, () -> service.create(ADMIN,
                new InventoryItemRequest(InventoryItemType.VAT_TU, "VT001", "A", "cái", -1, null, null, false, null)));
        assertThrows(InventoryValidationException.class, () -> service.create(ADMIN,
                thuoc("TH001", "x".repeat(InventoryItemService.MAX_TEN + 1))));
    }

    @Test
    void updateKeepsCodeAndChangesInfo() {
        service.create(ADMIN, vatTu("VT001", "Bông"));
        InventoryItemResponse r = service.update(ADMIN,
                new InventoryItemRequest(InventoryItemType.VAT_TU, "VT001", "Bông y tế", "gói", 30, null, null, false, ""));
        assertEquals("Bông y tế", r.ten());
        assertEquals(30, r.dinhMucToiThieu());
        assertNull(r.moTa(), "Chuỗi rỗng ở trường tùy chọn được lưu là null");
    }

    @Test
    void updateUnknownItemFails() {
        assertThrows(InventoryValidationException.class, () -> service.update(ADMIN, thuoc("TH999", "A")));
    }

    @Test
    void searchByKeywordAndType() {
        service.create(ADMIN, thuoc("TH001", "Thuốc tê A"));
        service.create(ADMIN, vatTu("VT001", "Găng tay y tế"));
        service.create(ADMIN, vatTu("VT002", "Kim tiêm"));
        assertEquals(3, service.search(ADMIN, null, null).size());
        assertEquals(2, service.search(ADMIN, "", InventoryItemType.VAT_TU).size());
        assertEquals(List.of("VT002"), service.search(ADMIN, " KIM ", null).stream().map(InventoryItemResponse::ma).toList());
        assertEquals(List.of("TH001"), service.search(ADMIN, "lidocaine", null).stream().map(InventoryItemResponse::ma).toList());
    }

    @Test
    void suggestNextCode() {
        assertEquals("TH001", service.suggestNextCode(ADMIN, InventoryItemType.THUOC));
        service.create(ADMIN, vatTu("VT009", "A"));
        assertEquals("VT010", service.suggestNextCode(ADMIN, InventoryItemType.VAT_TU));
    }

    @Test
    void permissionsAreEnforced() {
        Set<Role> receptionist = Set.of(Role.RECEPTIONIST);
        assertThrows(PermissionDeniedException.class, () -> service.search(receptionist, "", null));
        assertThrows(PermissionDeniedException.class, () -> service.create(receptionist, thuoc("TH001", "A")));
        assertThrows(PermissionDeniedException.class, () -> service.update(receptionist, thuoc("TH001", "A")));
        assertThrows(PermissionDeniedException.class,
                () -> service.suggestNextCode(receptionist, InventoryItemType.THUOC));
    }

    @Test
    void summaryCountsByType() {
        service.create(ADMIN, thuoc("TH001", "A"));
        service.create(ADMIN, vatTu("VT001", "B"));
        service.create(ADMIN, vatTu("VT002", "C"));
        var summary = service.summarize(ADMIN);
        assertEquals(3, summary.tongMatHang());
        assertEquals(1, summary.soThuoc());
        assertEquals(2, summary.soVatTu());
        assertEquals(true, summary.coDuLieuTonKho());
        assertEquals(3, summary.soCanBoSung(), "Chưa nhập hàng nên cả 3 mặt hàng (định mức > 0) đều dưới định mức");
        assertThrows(PermissionDeniedException.class, () -> service.summarize(Set.of(Role.RECEPTIONIST)));
    }
}
