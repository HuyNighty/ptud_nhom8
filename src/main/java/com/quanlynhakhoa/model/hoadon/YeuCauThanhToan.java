package com.quanlynhakhoa.model.hoadon;

import java.math.BigDecimal;
import java.util.List;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class YeuCauThanhToan {
    private final String invoiceId;
    private final BigDecimal amount;
    private final List<PhanThanhToan> parts;
    private final boolean transferConfirmed;

    public YeuCauThanhToan(String invoiceId, BigDecimal amount, List<PhanThanhToan> parts, boolean transferConfirmed) {
        parts = List.copyOf(parts);
        this.invoiceId = invoiceId;
        this.amount = amount;
        this.parts = parts;
        this.transferConfirmed = transferConfirmed;
    }

    public String invoiceId() { return invoiceId; }
    public BigDecimal amount() { return amount; }
    public List<PhanThanhToan> parts() { return parts; }
    public boolean transferConfirmed() { return transferConfirmed; }




    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof YeuCauThanhToan value)) return false;
        return java.util.Objects.equals(invoiceId, value.invoiceId)
                && java.util.Objects.equals(amount, value.amount)
                && java.util.Objects.equals(parts, value.parts)
                && transferConfirmed == value.transferConfirmed;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(invoiceId);
        result = 31 * result + java.util.Objects.hashCode(amount);
        result = 31 * result + java.util.Objects.hashCode(parts);
        result = 31 * result + Boolean.hashCode(transferConfirmed);
        return result;
    }

    @Override
    public String toString() {
        return "PaymentRequest[" + "invoiceId=" + invoiceId + ", " + "amount=" + amount + ", " + "parts=" + parts + ", " + "transferConfirmed=" + transferConfirmed + "]";
    }
}
