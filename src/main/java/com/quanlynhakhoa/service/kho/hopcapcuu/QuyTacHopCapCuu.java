package com.quanlynhakhoa.service.kho.hopcapcuu;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Pure business rules for first-aid kit inspections; no SQL or Swing dependencies. */
public final class QuyTacHopCapCuu {
    private QuyTacHopCapCuu() {}

    public static boolean itemPasses(int minimumQuantity, int actualQuantity,
                                     LocalDate configuredNearestExpiry, LocalDate actualExpiry,
                                     LocalDate today, int minimumExpiryDays) {
        if (minimumQuantity < 1 || actualQuantity < 0 || today == null || minimumExpiryDays < 1) {
            throw new IllegalArgumentException("Thông số kiểm tra hộp cấp cứu không hợp lệ.");
        }
        if (actualQuantity < minimumQuantity) return false;
        boolean expiryRequired = configuredNearestExpiry != null || actualExpiry != null;
        if (!expiryRequired) return true;
        return actualExpiry != null && actualExpiry.isAfter(today.plusDays(minimumExpiryDays));
    }

    /** A missing inspection is due immediately; the boundary matches the documented > interval rule. */
    public static boolean isOverdue(LocalDate lastInspection, LocalDate today, int intervalDays) {
        if (today == null || intervalDays < 1) {
            throw new IllegalArgumentException("Chu kỳ kiểm tra hộp cấp cứu không hợp lệ.");
        }
        return lastInspection == null || ChronoUnit.DAYS.between(lastInspection, today) > intervalDays;
    }
}
