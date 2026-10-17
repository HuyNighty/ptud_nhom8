package com.quanlynhakhoa.features.inventory.service;

import com.quanlynhakhoa.features.auth.enums.Permission;
import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.auth.service.AuthorizationService;
import com.quanlynhakhoa.features.inventory.dto.response.AttentionNoticeResponse;
import com.quanlynhakhoa.features.inventory.dto.response.ExpiringLotResponse;
import com.quanlynhakhoa.features.inventory.dto.response.InventoryActivityResponse;
import com.quanlynhakhoa.features.inventory.dto.response.InventoryOverviewResponse;
import com.quanlynhakhoa.features.inventory.dto.response.LowStockItemResponse;
import com.quanlynhakhoa.features.inventory.entity.MedicalSupply;
import com.quanlynhakhoa.features.inventory.entity.Medicine;
import com.quanlynhakhoa.features.inventory.entity.StockLot;
import com.quanlynhakhoa.features.inventory.enums.ExpiryLevel;
import com.quanlynhakhoa.features.inventory.enums.NoticeSeverity;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.enums.StockLevel;
import com.quanlynhakhoa.features.inventory.repository.MedicalSupplyRepository;
import com.quanlynhakhoa.features.inventory.repository.MedicineRepository;
import com.quanlynhakhoa.features.inventory.repository.StockInReceiptRepository;
import com.quanlynhakhoa.features.inventory.repository.StockLotRepository;
import com.quanlynhakhoa.features.inventory.repository.StockOutReceiptRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tổng quan kho cho INVENTORY_MANAGER (SYSTEM_ADMIN dùng chung). Mọi số liệu lấy từ SQL Server:
 * danh mục, lô hàng (tồn, hạn dùng), phiếu nhập và phiếu xuất. Hoàn trả/hủy chưa có nên trả null.
 */
@Service
public class InventoryOverviewService {

    private final AuthorizationService authorizationService;
    private final MedicineRepository medicineRepository;
    private final MedicalSupplyRepository supplyRepository;
    private final StockLotRepository lotRepository;
    private final StockInReceiptRepository receiptRepository;
    private final StockOutReceiptRepository stockOutRepository;

    public InventoryOverviewService(AuthorizationService authorizationService,
                                    MedicineRepository medicineRepository,
                                    MedicalSupplyRepository supplyRepository,
                                    StockLotRepository lotRepository,
                                    StockInReceiptRepository receiptRepository,
                                    StockOutReceiptRepository stockOutRepository) {
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.medicineRepository = Objects.requireNonNull(medicineRepository);
        this.supplyRepository = Objects.requireNonNull(supplyRepository);
        this.lotRepository = Objects.requireNonNull(lotRepository);
        this.receiptRepository = Objects.requireNonNull(receiptRepository);
        this.stockOutRepository = Objects.requireNonNull(stockOutRepository);
    }

    /**
     * @param authenticatedRoles Role lấy từ phiên đăng nhập hợp lệ, không lấy từ UI.
     */
    @Transactional(readOnly = true)
    public InventoryOverviewResponse getOverview(Set<Role> authenticatedRoles) {
        return getOverview(authenticatedRoles, LocalDate.now());
    }

    @Transactional(readOnly = true)
    InventoryOverviewResponse getOverview(Set<Role> authenticatedRoles, LocalDate today) {
        authorizationService.require(authenticatedRoles, Permission.INVENTORY_READ);
        List<Medicine> medicines = medicineRepository.search("");
        List<MedicalSupply> supplies = supplyRepository.search("");
        List<StockLot> lotsInStock = lotRepository.findBySoLuongTonGreaterThan(0);
        Map<String, InventoryRules.StockSnapshot> stock = InventoryRules.stockByItem(lotsInStock, today);
        Map<String, String[]> names = new HashMap<>();

        List<LowStockItemResponse> lowStock = new ArrayList<>();
        long soKiemSoat = 0;
        for (Medicine m : medicines) {
            names.put(m.getMaThuoc(), new String[] {m.getTenThuoc(), m.getDonViTinh()});
            addIfLow(lowStock, m.getMaThuoc(), m.getTenThuoc(), m.getDinhMucToiThieu(), stock);
            if (m.isKiemSoatDacBiet()) {
                soKiemSoat++;
            }
        }
        for (MedicalSupply v : supplies) {
            names.put(v.getMaVatTu(), new String[] {v.getTenVatTu(), v.getDonViTinh()});
            addIfLow(lowStock, v.getMaVatTu(), v.getTenVatTu(), v.getDinhMucToiThieu(), stock);
        }
        lowStock.sort(Comparator.comparing(LowStockItemResponse::mucTon).thenComparing(LowStockItemResponse::maMatHang));

        List<ExpiringLotResponse> lots = new ArrayList<>();
        for (StockLot lot : lotsInStock) {
            ExpiryLevel level = InventoryRules.classifyExpiry(lot.getHanSuDung(), today);
            if (level == ExpiryLevel.CON_HAN) {
                continue;
            }
            String[] info = names.getOrDefault(lot.maMatHang(), new String[] {lot.maMatHang(), ""});
            lots.add(new ExpiringLotResponse(lot.getSoLo(), info[0], lot.getHanSuDung(), lot.getSoLuongTon(),
                    info[1], level));
        }
        lots.sort(Comparator.comparing(ExpiringLotResponse::hanSuDung));
        long expired = lots.stream().filter(l -> l.mucCanhBao() == ExpiryLevel.DA_HET_HAN).count();
        int urgent = (int) lots.stream().filter(l -> l.mucCanhBao() == ExpiryLevel.DA_HET_HAN
                || l.mucCanhBao() == ExpiryLevel.CON_TOI_DA_7_NGAY).count();

        long pendingIn = receiptRepository.countByTrangThai(VoucherStatus.CHO_XU_LY);
        long pendingOut = stockOutRepository.countByTrangThai(VoucherStatus.CHO_XU_LY);
        long pending = pendingIn + pendingOut;
        java.time.LocalDateTime dayStart = today.atStartOfDay();
        java.time.LocalDateTime dayEnd = today.plusDays(1).atStartOfDay().minusSeconds(1);
        int receivedToday = (int) receiptRepository.countByThoiDiemNhapKhoBetween(dayStart, dayEnd);
        int issuedToday = (int) stockOutRepository.countByThoiDiemXuatKhoBetween(dayStart, dayEnd);

        List<AttentionNoticeResponse> notices = new ArrayList<>();
        if (medicines.isEmpty() && supplies.isEmpty()) {
            notices.add(new AttentionNoticeResponse(NoticeSeverity.WARNING,
                    "Danh mục chưa có mặt hàng nào — thêm tại mục Vật tư & thuốc"));
        }
        if (expired > 0) {
            notices.add(new AttentionNoticeResponse(NoticeSeverity.CRITICAL,
                    "Có " + expired + " lô đã hết hạn còn trong kho"));
        }
        long belowMin = lowStock.stream().filter(i -> i.soLuongTon() < i.dinhMucToiThieu()).count();
        if (belowMin > 0) {
            notices.add(new AttentionNoticeResponse(NoticeSeverity.CRITICAL,
                    "Có " + belowMin + " mặt hàng dưới định mức cần đặt hàng"));
        }
        if (soKiemSoat > 0) {
            notices.add(new AttentionNoticeResponse(NoticeSeverity.WARNING,
                    "Có " + soKiemSoat + " thuốc kiểm soát đặc biệt cần theo dõi sổ KSĐB"));
        }
        if (pendingIn > 0) {
            notices.add(new AttentionNoticeResponse(NoticeSeverity.WARNING,
                    "Có " + pendingIn + " phiếu nhập đang chờ nhập kho"));
        }
        if (pendingOut > 0) {
            notices.add(new AttentionNoticeResponse(NoticeSeverity.WARNING,
                    "Có " + pendingOut + " phiếu xuất đang chờ xuất kho"));
        }

        return new InventoryOverviewResponse(
                today,
                medicines.size() + supplies.size(),
                medicines.size(),
                supplies.size(),
                soKiemSoat,
                true,
                lowStock.size(),
                urgent,
                (int) pending,
                lowStock,
                lots,
                new InventoryActivityResponse(receivedToday, issuedToday, null, null),
                notices);
    }

    private static void addIfLow(List<LowStockItemResponse> target, String ma, String ten, int dinhMuc,
                                 Map<String, InventoryRules.StockSnapshot> stock) {
        int ton = stock.getOrDefault(ma, InventoryRules.StockSnapshot.EMPTY).tonKho();
        StockLevel level = InventoryRules.classifyStock(ton, dinhMuc);
        if (level != StockLevel.DU) {
            target.add(new LowStockItemResponse(ma, ten, ton, dinhMuc, level));
        }
    }
}
