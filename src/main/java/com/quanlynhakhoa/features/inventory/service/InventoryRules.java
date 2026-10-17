package com.quanlynhakhoa.features.inventory.service;

import com.quanlynhakhoa.features.inventory.enums.ExpiryLevel;
import com.quanlynhakhoa.features.inventory.enums.StockLevel;
import com.quanlynhakhoa.features.inventory.entity.StockLot;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Quy tắc phân loại tồn và hạn dùng, dùng chung cho các màn kho. */
final class InventoryRules {

    /** Vùng đệm trên định mức để cảnh báo "Sắp hết" — 20%, CHỜ NHÓM CHỐT. */
    static final double LOW_STOCK_BUFFER_RATIO = 0.20;
    static final int URGENT_EXPIRY_DAYS = 7;
    static final int WATCH_EXPIRY_DAYS = 30;

    private InventoryRules() {
    }

    static StockLevel classifyStock(int ton, int dinhMuc) {
        if (ton < 0 || dinhMuc < 0) {
            throw new IllegalArgumentException("Tồn và định mức không được âm");
        }
        if (ton == 0) {
            return StockLevel.HET_HANG;
        }
        if (ton < dinhMuc) {
            return StockLevel.THIEU;
        }
        double warningLimit = dinhMuc * (1 + LOW_STOCK_BUFFER_RATIO);
        return ton < warningLimit ? StockLevel.SAP_HET : StockLevel.DU;
    }

    static ExpiryLevel classifyExpiry(LocalDate hanSuDung, LocalDate today) {
        if (hanSuDung == null) {
            return ExpiryLevel.CON_HAN;
        }
        Objects.requireNonNull(today, "today must not be null");
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

    /** Lô còn dùng được (tính vào tồn khả dụng): chưa hết hạn hoặc không có hạn. */
    static boolean usable(LocalDate hanSuDung, LocalDate today) {
        return hanSuDung == null || !hanSuDung.isBefore(today);
    }

    /** Tồn khả dụng và hạn gần nhất của một mặt hàng. */
    record StockSnapshot(int tonKho, LocalDate hanGanNhat) {
        static final StockSnapshot EMPTY = new StockSnapshot(0, null);
    }

    /**
     * Gộp các lô còn tồn theo mã mặt hàng: tồn khả dụng = tổng tồn các lô chưa hết hạn,
     * hạn gần nhất = hạn sớm nhất trong các lô đó.
     */
    static Map<String, StockSnapshot> stockByItem(List<StockLot> lots, LocalDate today) {
        Map<String, StockSnapshot> result = new HashMap<>();
        for (StockLot lot : lots) {
            if (lot.getSoLuongTon() <= 0 || !usable(lot.getHanSuDung(), today)) {
                continue;
            }
            StockSnapshot current = result.getOrDefault(lot.maMatHang(), StockSnapshot.EMPTY);
            LocalDate nearest = current.hanGanNhat();
            if (lot.getHanSuDung() != null && (nearest == null || lot.getHanSuDung().isBefore(nearest))) {
                nearest = lot.getHanSuDung();
            }
            result.put(lot.maMatHang(), new StockSnapshot(current.tonKho() + lot.getSoLuongTon(), nearest));
        }
        return result;
    }
}
