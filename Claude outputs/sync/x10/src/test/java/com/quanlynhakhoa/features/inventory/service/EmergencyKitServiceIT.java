package com.quanlynhakhoa.features.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.inventory.dto.request.EmergencyKitItemRequest;
import com.quanlynhakhoa.features.inventory.dto.request.EmergencyKitRequest;
import com.quanlynhakhoa.features.inventory.dto.request.InventoryItemRequest;
import com.quanlynhakhoa.features.inventory.dto.request.KitCheckLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.KitCheckRequest;
import com.quanlynhakhoa.features.inventory.dto.request.KitRestockLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.KitRestockRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockInLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockInRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockMonitorAdjustRequest;
import com.quanlynhakhoa.features.inventory.dto.request.SupplierRequest;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitItemResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockMonitorRowResponse;
import com.quanlynhakhoa.features.inventory.enums.EmergencyKitStatus;
import com.quanlynhakhoa.features.inventory.enums.EmergencyKitType;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.enums.KitLogType;
import com.quanlynhakhoa.features.inventory.enums.StockLevel;
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
 * Hộp cấp cứu và tồn kho trên SQL Server thật: mapping và ràng buộc của V12 (HopCapCuu, ChiTietHopCapCuu,
 * NhatKyHopCapCuu, cột viTriLuuTru), khóa UPDLOCK khi cấp mã, bổ sung qua phiếu xuất. Rollback sau mỗi test.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "n08.desktop.enabled=false",
                "spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl"
        }
)
@Transactional
class EmergencyKitServiceIT {
    private static final Set<Role> ADMIN = Set.of(Role.SYSTEM_ADMIN);

    @Autowired
    private EmergencyKitService kits;
    @Autowired
    private StockMonitorService monitor;
    @Autowired
    private StockInService stockIn;
    @Autowired
    private InventoryItemService items;
    @Autowired
    private JdbcTemplate jdbc;

    private InventoryActor actor;

    @BeforeEach
    void setUp() {
        String phone = "09" + ThreadLocalRandom.current().nextLong(10_000_000L, 99_999_999L);
        jdbc.update("INSERT INTO dbo.NhanVien (maNhanVien, hoTen, soDienThoai, chucDanh) VALUES (?, ?, ?, 'KHO')",
                "NVIT002", "Kho IT hộp cấp cứu", phone);
        actor = new InventoryActor(ADMIN, "NVIT002", "Kho IT hộp cấp cứu");
        items.create(ADMIN, new InventoryItemRequest(InventoryItemType.THUOC, "TH90011", "Adrenaline IT", "ống",
                5, "1mg/1ml", "Epinephrine", false, null));
        items.create(ADMIN, new InventoryItemRequest(InventoryItemType.VAT_TU, "VT90011", "Bơm tiêm IT", "cái",
                5, null, null, false, null));
        String supplier = stockIn.createSupplier(ADMIN,
                new SupplierRequest("NCC hộp cấp cứu IT " + phone, "0901234567", null, null)).ma();
        String in = stockIn.create(actor, new StockInRequest(LocalDate.now(), supplier, "IT", List.of(
                new StockInLineRequest(InventoryItemType.THUOC, "TH90011", "IT-AD1", LocalDate.now().plusYears(1), 20,
                        BigDecimal.valueOf(9_000)),
                new StockInLineRequest(InventoryItemType.VAT_TU, "VT90011", "IT-BT1", null, 30, BigDecimal.TEN)))).maPhieu();
        stockIn.complete(actor, in);
    }

    private static EmergencyKitItemResponse item(EmergencyKitDetailResponse d, String ma) {
        return d.matHang().stream().filter(i -> i.maMatHang().equals(ma)).findFirst().orElseThrow();
    }

    @Test
    void createCheckRestockAndEditRoundTrip() {
        EmergencyKitDetailResponse created = kits.create(actor, new EmergencyKitRequest("Hộp chống sốc IT",
                EmergencyKitType.CHONG_SOC, "Phòng điều trị IT", 7, 30, "Ghi chú tiếng Việt", List.of(
                new EmergencyKitItemRequest(InventoryItemType.THUOC, "TH90011", 2),
                new EmergencyKitItemRequest(InventoryItemType.VAT_TU, "VT90011", 3))));
        assertTrue(created.maHop().matches("HTCC-\\d{2,5}"), created.maHop());
        assertNotNull(item(created, "TH90011").maChiTiet());
        assertEquals(EmergencyKitStatus.THIEU, created.trangThai());

        EmergencyKitDetailResponse restocked = kits.restock(actor, created.maHop(), new KitRestockRequest(
                KitLogType.BO_SUNG, List.of(new KitRestockLineRequest(item(created, "TH90011").maChiTiet(), 2),
                new KitRestockLineRequest(item(created, "VT90011").maChiTiet(), 3)), "IT"));
        assertEquals(EmergencyKitStatus.AN_TOAN, restocked.trangThai());
        assertNotNull(restocked.nhatKy().get(0).maPhieuXuat());

        EmergencyKitDetailResponse checked = kits.check(actor, created.maHop(), new KitCheckRequest(List.of(
                new KitCheckLineRequest(item(restocked, "TH90011").maChiTiet(), 1, LocalDate.now().plusDays(200)),
                new KitCheckLineRequest(item(restocked, "VT90011").maChiTiet(), 3, null)), null));
        assertEquals(EmergencyKitStatus.THIEU, checked.trangThai());
        assertEquals(Boolean.FALSE, checked.nhatKy().get(0).dat());

        EmergencyKitDetailResponse edited = kits.update(actor, created.maHop(), new EmergencyKitRequest(
                "Hộp chống sốc IT (sửa)", EmergencyKitType.CO_BAN, "Sảnh IT", 14, 45, null,
                List.of(new EmergencyKitItemRequest(InventoryItemType.THUOC, "TH90011", 1))));
        assertEquals(1, edited.matHang().size());
        assertEquals(1, item(edited, "TH90011").soLuongHienCo());
        assertNotNull(item(edited, "TH90011").maChiTiet());
        assertEquals(List.of(KitLogType.KIEM_TRA, KitLogType.BO_SUNG),
                kits.logs(ADMIN, created.maHop()).stream().map(g -> g.loai()).toList());
    }

    @Test
    void stockMonitorReadsValueAndSavesLocation() {
        StockMonitorRowResponse adjusted = monitor.adjust(actor, InventoryItemType.THUOC, "TH90011",
                new StockMonitorAdjustRequest(30, "Tủ thuốc IT", true));
        assertEquals("Tủ thuốc IT", adjusted.viTriLuuTru());
        assertEquals(StockLevel.THIEU, adjusted.mucTon());
        assertEquals(0, BigDecimal.valueOf(180_000).compareTo(adjusted.giaTri()));
        assertEquals(1, monitor.history(ADMIN, InventoryItemType.THUOC, "TH90011").bienDong().size());
    }
}
