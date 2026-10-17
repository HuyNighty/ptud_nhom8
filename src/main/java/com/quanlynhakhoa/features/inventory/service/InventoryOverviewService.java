package com.quanlynhakhoa.features.inventory.service;

import com.quanlynhakhoa.features.auth.enums.Permission;
import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.auth.service.AuthorizationService;
import com.quanlynhakhoa.features.inventory.dto.response.AttentionNoticeResponse;
import com.quanlynhakhoa.features.inventory.dto.response.ExpiringLotResponse;
import com.quanlynhakhoa.features.inventory.dto.response.InventoryActivityResponse;
import com.quanlynhakhoa.features.inventory.dto.response.InventoryOverviewResponse;
import com.quanlynhakhoa.features.inventory.dto.response.LowStockItemResponse;
import com.quanlynhakhoa.features.inventory.enums.ExpiryLevel;
import com.quanlynhakhoa.features.inventory.enums.NoticeSeverity;
import com.quanlynhakhoa.features.inventory.enums.StockLevel;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Tổng quan kho cho INVENTORY_MANAGER (và SYSTEM_ADMIN dùng chung).
 *
 * <p>Quy tắc phân loại tồn/hạn dùng là logic thật và giữ nguyên khi thay nguồn dữ
 * liệu. Nguồn dữ liệu hiện là {@link InventoryOverviewPrototypeData} (PROTOTYPE) cho
 * tới khi schema kho được duyệt.</p>
 */
@Service
public class InventoryOverviewService {

    /** Vùng đệm trên định mức để cảnh báo "Sắp hết" — 20%, CHỜ NHÓM CHỐT. */
    static final double LOW_STOCK_BUFFER_RATIO = 0.20;
    static final int URGENT_EXPIRY_DAYS = 7;
    static final int WATCH_EXPIRY_DAYS = 30;

    private final AuthorizationService authorizationService;

    public InventoryOverviewService(AuthorizationService authorizationService) {
        this.authorizationService = Objects.requireNonNull(authorizationService);
    }

    /**
     * @param authenticatedRoles Role lấy từ phiên đăng nhập hợp lệ, không lấy từ UI.
     */
    public InventoryOverviewResponse getOverview(Set<Role> authenticatedRoles) {
        return getOverview(authenticatedRoles, LocalDate.now());
    }

    InventoryOverviewResponse getOverview(Set<Role> authenticatedRoles, LocalDate today) {
        authorizationService.require(authenticatedRoles, Permission.INVENTORY_READ);
        Objects.requireNonNull(today, "today must not be null");

        List<LowStockItemResponse> lowStock = InventoryOverviewPrototypeData.stockItems().stream()
                .map(i -> new LowStockItemResponse(i.ma(), i.ten(), i.ton(), i.dinhMuc(),
                        classifyStock(i.ton(), i.dinhMuc())))
                .filter(i -> i.mucTon() != StockLevel.DU)
                .sorted(Comparator.comparing(LowStockItemResponse::mucTon)
                        .thenComparing(LowStockItemResponse::maMatHang))
                .toList();

        List<ExpiringLotResponse> lots = InventoryOverviewPrototypeData.lots(today).stream()
                .map(l -> new ExpiringLotResponse(l.maLo(), l.tenSanPham(), l.hanSuDung(),
                        l.soLuong(), l.donVi(), classifyExpiry(l.hanSuDung(), today)))
                .filter(l -> l.mucCanhBao() != ExpiryLevel.CON_HAN)
                .sorted(Comparator.comparing(ExpiringLotResponse::hanSuDung))
                .toList();

        int urgentLots = (int) lots.stream()
                .filter(l -> l.mucCanhBao() == ExpiryLevel.DA_HET_HAN
                        || l.mucCanhBao() == ExpiryLevel.CON_TOI_DA_7_NGAY)
                .count();

        int pending = InventoryOverviewPrototypeData.PHIEU_CHO_XU_LY;
        return new InventoryOverviewResponse(
                today,
                InventoryOverviewPrototypeData.TONG_MAT_HANG,
                lowStock.size(),
                urgentLots,
                pending,
                lowStock,
                lots,
                new InventoryActivityResponse(
                        InventoryOverviewPrototypeData.NHAP_HOM_NAY,
                        InventoryOverviewPrototypeData.XUAT_HOM_NAY,
                        InventoryOverviewPrototypeData.HOAN_TRA_HOM_NAY,
                        InventoryOverviewPrototypeData.HUY_HOM_NAY),
                buildNotices(lowStock, lots, pending),
                true);
    }

    static StockLevel classifyStock(int ton, int dinhMuc) {
        if (ton < 0 || dinhMuc < 0) {
            throw new IllegalArgumentException("Tồn và định mức không được âm");
        }
        if (ton < dinhMuc) {
            return StockLevel.THIEU;
        }
        double warningLimit = dinhMuc * (1 + LOW_STOCK_BUFFER_RATIO);
        return ton < warningLimit ? StockLevel.SAP_HET : StockLevel.DU;
    }

    static ExpiryLevel classifyExpiry(LocalDate hanSuDung, LocalDate today) {
        Objects.requireNonNull(hanSuDung, "hanSuDung must not be null");
        long daysLeft = ChronoUnit.DAYS.between(today, hanSuDung);
        if (daysLeft < 0) {
            return ExpiryLevel.DA_HET_HAN;
        }
        if (daysLeft <= URGENT_EXPIRY_DAYS) {
            return ExpiryLevel.CON_TOI_DA_7_NGAY;
        }
        if (daysLeft <= WATCH_EXPIRY_DAYS) {
            return ExpiryLevel.CON_TOI_DA_30_NGAY;
        }
        return ExpiryLevel.CON_HAN;
    }

    private static List<AttentionNoticeResponse> buildNotices(List<LowStockItemResponse> lowStock,
                                                              List<ExpiringLotResponse> lots,
                                                              int pendingVouchers) {
        List<AttentionNoticeResponse> notices = new ArrayList<>();
        if (lots.stream().anyMatch(l -> l.mucCanhBao() == ExpiryLevel.DA_HET_HAN)) {
            notices.add(new AttentionNoticeResponse(NoticeSeverity.CRITICAL,
                    "Có thuốc/vật tư hết hạn trong kho"));
        }
        if (lowStock.stream().anyMatch(i -> i.mucTon() == StockLevel.THIEU)) {
            notices.add(new AttentionNoticeResponse(NoticeSeverity.CRITICAL,
                    "Có vật tư dưới định mức cần đặt hàng"));
        }
        if (InventoryOverviewPrototypeData.CO_THUOC_KSDB_CAN_DOI_SOAT) {
            notices.add(new AttentionNoticeResponse(NoticeSeverity.WARNING,
                    "Có thuốc kiểm soát đặc biệt cần đối soát"));
        }
        if (pendingVouchers > 0) {
            notices.add(new AttentionNoticeResponse(NoticeSeverity.WARNING,
                    "Có " + pendingVouchers + " phiếu kho chờ xử lý"));
        }
        if (InventoryOverviewPrototypeData.CO_HOP_CAP_CUU_CAN_KIEM_TRA) {
            notices.add(new AttentionNoticeResponse(NoticeSeverity.WARNING,
                    "Hộp thuốc cấp cứu cần kiểm tra định kỳ"));
        }
        return notices;
    }
}
