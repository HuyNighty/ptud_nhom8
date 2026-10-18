package com.quanlynhakhoa.features.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.inventory.dto.request.InventoryItemRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockInLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockInRequest;
import com.quanlynhakhoa.features.inventory.dto.request.SupplierRequest;
import com.quanlynhakhoa.features.inventory.dto.response.InventoryItemResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockInDetailResponse;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.exception.InventoryValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhập kho trên SQL Server thật: kiểm tra truy vấn JPQL/native (khóa UPDLOCK, fetch join, đếm),
 * mapping và ràng buộc của V6. Mọi thay đổi rollback sau mỗi test.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "n08.desktop.enabled=false",
                "spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl"
        }
)
@Transactional
class StockInServiceIT {
    private static final Set<Role> ADMIN = Set.of(Role.SYSTEM_ADMIN);

    @Autowired
    private StockInService stockIn;
    @Autowired
    private InventoryItemService items;
    @Autowired
    private InventoryOverviewService overview;
    @Autowired
    private JdbcTemplate jdbc;

    private InventoryActor actor;
    private String supplier;

    @BeforeEach
    void setUp() {
        String phone = "09" + ThreadLocalRandom.current().nextLong(10_000_000L, 99_999_999L);
        jdbc.update("INSERT INTO dbo.NhanVien (maNhanVien, hoTen, soDienThoai, chucDanh) VALUES (?, ?, ?, 'KHO')",
                "NVIT001", "Nhân viên kho IT", phone);
        actor = new InventoryActor(ADMIN, "NVIT001", "Nhân viên kho IT");
        items.create(ADMIN, new InventoryItemRequest(InventoryItemType.THUOC, "TH90001", "Thuốc tê IT", "ống",
                100, "2%", "Lidocaine", false, null));
        items.create(ADMIN, new InventoryItemRequest(InventoryItemType.VAT_TU, "VT90001", "Găng tay IT", "hộp",
                5, null, null, false, null));
        supplier = stockIn.createSupplier(ADMIN,
                new SupplierRequest("Nhà cung cấp IT " + phone, "0901234567", null, null)).ma();
    }

    private StockInDetailResponse create(String lot, int quantity) {
        return stockIn.create(actor, new StockInRequest(LocalDate.now(), supplier, "IT", List.of(
                new StockInLineRequest(InventoryItemType.THUOC, "TH90001", lot, LocalDate.now().plusYears(1),
                        quantity, BigDecimal.valueOf(12_000)),
                new StockInLineRequest(InventoryItemType.VAT_TU, "VT90001", "G-" + lot, null, 3, BigDecimal.TEN))));
    }

    @Test
    void createListCompleteAndReadBack() {
        StockInDetailResponse first = create("IT-L1", 40);
        StockInDetailResponse second = create("IT-L2", 10);
        assertTrue(second.maPhieu().compareTo(first.maPhieu()) > 0, "Mã phiếu tăng dần");
        assertEquals("Nhân viên kho IT", first.nguoiLap());
        assertTrue(stockIn.list(ADMIN).stream().anyMatch(r -> r.maPhieu().equals(first.maPhieu())));

        StockInDetailResponse done = stockIn.complete(actor, first.maPhieu());
        assertEquals(VoucherStatus.DA_HOAN_THANH, done.trangThai());
        assertEquals(2, stockIn.getDetail(ADMIN, first.maPhieu()).dong().size());

        InventoryItemResponse medicine = items.search(ADMIN, "TH90001", null).get(0);
        assertEquals(40, medicine.tonKho());
        assertEquals(LocalDate.now().plusYears(1), medicine.hanSuDungGanNhat());
        assertTrue(overview.getOverview(ADMIN).soPhieuChoXuLy() >= 1);
    }

    @Test
    void sameLotAccumulatesAndCompletedReceiptIsLocked() {
        stockIn.complete(actor, create("IT-L1", 40).maPhieu());
        String again = create("IT-L1", 15).maPhieu();
        stockIn.complete(actor, again);
        assertEquals(55, items.search(ADMIN, "TH90001", null).get(0).tonKho());
        assertThrows(InventoryValidationException.class, () -> stockIn.complete(actor, again));
        assertThrows(InventoryValidationException.class, () -> stockIn.cancel(actor, again, "x"));
    }

    @Test
    void cancelStoresReasonAndPassesDatabaseCheck() {
        String ma = create("IT-L3", 1).maPhieu();
        StockInDetailResponse cancelled = stockIn.cancel(actor, ma, "Giao sai hàng");
        assertEquals(VoucherStatus.DA_HUY, cancelled.trangThai());
        assertEquals("Giao sai hàng", stockIn.getDetail(ADMIN, ma).lyDoHuy());
    }
}
