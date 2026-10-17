package com.quanlynhakhoa.features.inventory.support;

import com.quanlynhakhoa.features.auth.service.AuthorizationService;
import com.quanlynhakhoa.features.auth.service.RolePermissionPolicy;
import com.quanlynhakhoa.features.inventory.service.InventoryItemService;
import com.quanlynhakhoa.features.inventory.service.InventoryOverviewService;
import com.quanlynhakhoa.features.inventory.service.StockInService;

/** TEST-ONLY: ghép các Service kho với repository trong bộ nhớ (unit test và bản xem trước). */
public final class InventoryTestFixture {
    public final InMemoryMedicineRepository medicines = new InMemoryMedicineRepository();
    public final InMemoryMedicalSupplyRepository supplies = new InMemoryMedicalSupplyRepository();
    public final InMemoryStockLotRepository lots = new InMemoryStockLotRepository();
    public final InMemorySupplierRepository suppliers = new InMemorySupplierRepository();
    public final InMemoryStockInReceiptRepository receipts = new InMemoryStockInReceiptRepository();
    public final AuthorizationService authorization = new AuthorizationService(new RolePermissionPolicy());
    public final InventoryItemService itemService =
            new InventoryItemService(authorization, medicines, supplies, lots);
    public final InventoryOverviewService overviewService =
            new InventoryOverviewService(authorization, medicines, supplies, lots, receipts);
    public final StockInService stockInService =
            new StockInService(authorization, receipts, suppliers, lots, medicines, supplies);
}
