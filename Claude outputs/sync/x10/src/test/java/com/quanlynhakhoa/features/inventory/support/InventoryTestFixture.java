package com.quanlynhakhoa.features.inventory.support;

import com.quanlynhakhoa.features.auth.service.AuthorizationService;
import com.quanlynhakhoa.features.auth.service.RolePermissionPolicy;
import com.quanlynhakhoa.features.inventory.service.InventoryItemService;
import com.quanlynhakhoa.features.inventory.service.InventoryOverviewService;
import com.quanlynhakhoa.features.inventory.service.StockInService;
import com.quanlynhakhoa.features.inventory.service.StockOutService;
import com.quanlynhakhoa.features.inventory.service.StockReturnService;
import com.quanlynhakhoa.features.inventory.service.StocktakeService;
import com.quanlynhakhoa.features.inventory.service.DisposalService;
import com.quanlynhakhoa.features.inventory.service.ControlledDrugService;
import com.quanlynhakhoa.features.inventory.service.EmergencyKitService;
import com.quanlynhakhoa.features.inventory.service.StockMonitorService;

/** TEST-ONLY: ghép các Service kho với repository trong bộ nhớ (unit test và bản xem trước). */
public final class InventoryTestFixture {
    public final InMemoryMedicineRepository medicines = new InMemoryMedicineRepository();
    public final InMemoryMedicalSupplyRepository supplies = new InMemoryMedicalSupplyRepository();
    public final InMemoryStockLotRepository lots = new InMemoryStockLotRepository();
    public final InMemorySupplierRepository suppliers = new InMemorySupplierRepository();
    public final InMemoryStockInReceiptRepository receipts = new InMemoryStockInReceiptRepository();
    public final InMemoryStockOutReceiptRepository stockOuts = new InMemoryStockOutReceiptRepository();
    public final InMemoryStockReturnReceiptRepository returns = new InMemoryStockReturnReceiptRepository();
    public final InMemoryStocktakeRepository stocktakes = new InMemoryStocktakeRepository();
    public final InMemoryDisposalRepository disposals = new InMemoryDisposalRepository();
    public final InMemoryEmergencyKitRepository kits = new InMemoryEmergencyKitRepository();
    public final InMemoryEmergencyKitLogRepository kitLogs = new InMemoryEmergencyKitLogRepository();
    public final InMemoryStockInLineRepository stockInLines = new InMemoryStockInLineRepository(receipts);
    public final AuthorizationService authorization = new AuthorizationService(new RolePermissionPolicy());
    public final InventoryItemService itemService =
            new InventoryItemService(authorization, medicines, supplies, lots);
    public final InventoryOverviewService overviewService =
            new InventoryOverviewService(authorization, medicines, supplies, lots, receipts, stockOuts, returns,
                    stocktakes, disposals);
    public final StockInService stockInService =
            new StockInService(authorization, receipts, suppliers, lots, medicines, supplies);
    public final StockOutService stockOutService =
            new StockOutService(authorization, stockOuts, lots, stockInLines, receipts, medicines, supplies);
    public final StockReturnService stockReturnService =
            new StockReturnService(authorization, returns, stockOuts, lots, receipts, medicines, supplies);
    public final StocktakeService stocktakeService =
            new StocktakeService(authorization, stocktakes, lots, receipts, medicines, supplies);
    public final DisposalService disposalService =
            new DisposalService(authorization, disposals, lots, stockInLines, receipts, medicines, supplies);
    public final ControlledDrugService controlledDrugService = new ControlledDrugService(authorization, medicines,
            lots, receipts, suppliers, stockOuts, returns, stocktakes, disposals);
    public final EmergencyKitService emergencyKitService = new EmergencyKitService(authorization, kits, kitLogs,
            lots, medicines, supplies, receipts, stockOutService);
    public final StockMonitorService stockMonitorService = new StockMonitorService(authorization, medicines, supplies,
            lots, receipts, suppliers, stockOuts, returns, stocktakes, disposals);
}
