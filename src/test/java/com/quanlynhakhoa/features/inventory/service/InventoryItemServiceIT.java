package com.quanlynhakhoa.features.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.inventory.dto.request.InventoryItemRequest;
import com.quanlynhakhoa.features.inventory.dto.response.InventoryItemResponse;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.exception.InventoryValidationException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** Service → JPA → SQL Server thật (kiểm tra JPQL, mapping Unicode, chặn trùng mã). Rollback sau mỗi test. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "n08.desktop.enabled=false"
)
@Transactional
class InventoryItemServiceIT {
    private static final Set<Role> ADMIN = Set.of(Role.SYSTEM_ADMIN);

    @Autowired
    private InventoryItemService service;

    @Test
    void createSearchUpdateRoundTrip() {
        service.create(ADMIN, new InventoryItemRequest(InventoryItemType.VAT_TU, "VT90001",
                "Găng tay nitrile", "hộp", 20, null, null, false, "Không bột, size M"));

        List<InventoryItemResponse> found = service.search(ADMIN, "nitrile", InventoryItemType.VAT_TU);
        assertEquals(1, found.size());
        assertEquals("Găng tay nitrile", found.get(0).ten());

        InventoryItemResponse updated = service.update(ADMIN, new InventoryItemRequest(InventoryItemType.VAT_TU,
                "VT90001", "Găng tay nitrile xanh", "hộp", 25, null, null, false, null));
        assertEquals(25, updated.dinhMucToiThieu());
        assertEquals("Găng tay nitrile xanh", service.search(ADMIN, "VT90001", null).get(0).ten());
    }

    @Test
    void duplicateCodeDoesNotOverwriteExistingRow() {
        service.create(ADMIN, new InventoryItemRequest(InventoryItemType.THUOC, "TH90001",
                "Thuốc tê A", "ống", 5, "2%", "Lidocaine", true, null));
        assertThrows(InventoryValidationException.class, () -> service.create(ADMIN,
                new InventoryItemRequest(InventoryItemType.THUOC, "TH90001", "Ghi đè", "ống", 0, null, null, false, null)));
        assertEquals("Thuốc tê A", service.search(ADMIN, "TH90001", null).get(0).ten());
    }

    @Test
    void suggestedCodeIsFreeAndWellFormed() {
        String code = service.suggestNextCode(ADMIN, InventoryItemType.THUOC);
        assertTrue(code.matches("TH\\d{3,8}"), code);
        assertTrue(service.search(ADMIN, code, InventoryItemType.THUOC).isEmpty());
    }
}
