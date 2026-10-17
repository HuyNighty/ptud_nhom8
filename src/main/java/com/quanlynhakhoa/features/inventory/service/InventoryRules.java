package com.quanlynhakhoa.features.inventory.service;

import com.quanlynhakhoa.features.inventory.enums.ExpiryLevel;
import com.quanlynhakhoa.features.inventory.enums.StockLevel;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
}
