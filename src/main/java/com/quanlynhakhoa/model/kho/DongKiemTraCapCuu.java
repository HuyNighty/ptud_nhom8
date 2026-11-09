package com.quanlynhakhoa.model.kho;

import java.time.LocalDate;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class DongKiemTraCapCuu {
    private final String itemId;
    private final int actualQuantity;
    private final LocalDate expiry;

    public DongKiemTraCapCuu(String itemId, int actualQuantity, LocalDate expiry) {
        this.itemId = itemId;
        this.actualQuantity = actualQuantity;
        this.expiry = expiry;
    }

    public String itemId() { return itemId; }
    public int actualQuantity() { return actualQuantity; }
    public LocalDate expiry() { return expiry; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongKiemTraCapCuu value)) return false;
        return java.util.Objects.equals(itemId, value.itemId)
                && actualQuantity == value.actualQuantity
                && java.util.Objects.equals(expiry, value.expiry);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(itemId);
        result = 31 * result + Integer.hashCode(actualQuantity);
        result = 31 * result + java.util.Objects.hashCode(expiry);
        return result;
    }

    @Override
    public String toString() {
        return "CheckLine[" + "itemId=" + itemId + ", " + "actualQuantity=" + actualQuantity + ", " + "expiry=" + expiry + "]";
    }
}
