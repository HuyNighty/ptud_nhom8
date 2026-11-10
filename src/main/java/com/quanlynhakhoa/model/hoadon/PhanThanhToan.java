package com.quanlynhakhoa.model.hoadon;

import com.quanlynhakhoa.enums.PhuongThucThanhToan;
import java.math.BigDecimal;


/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class PhanThanhToan {
    private final String id;
    private final BigDecimal amount;
    private final PhuongThucThanhToan method;
    private final String reference;
    private final BigDecimal customerTender;

    public PhanThanhToan(String id, BigDecimal amount, PhuongThucThanhToan method, String reference, BigDecimal customerTender) {
        this.id = id;
        this.amount = amount;
        this.method = method;
        this.reference = reference;
        this.customerTender = customerTender;
    }

    public String id() { return id; }
    public BigDecimal amount() { return amount; }
    public PhuongThucThanhToan method() { return method; }
    public String reference() { return reference; }
    public BigDecimal customerTender() { return customerTender; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PhanThanhToan value)) return false;
        return java.util.Objects.equals(id, value.id)
                && java.util.Objects.equals(amount, value.amount)
                && java.util.Objects.equals(method, value.method)
                && java.util.Objects.equals(reference, value.reference)
                && java.util.Objects.equals(customerTender, value.customerTender);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(id);
        result = 31 * result + java.util.Objects.hashCode(amount);
        result = 31 * result + java.util.Objects.hashCode(method);
        result = 31 * result + java.util.Objects.hashCode(reference);
        result = 31 * result + java.util.Objects.hashCode(customerTender);
        return result;
    }

    @Override
    public String toString() {
        return "Part[" + "id=" + id + ", " + "amount=" + amount + ", " + "method=" + method + ", " + "reference=" + reference + ", " + "customerTender=" + customerTender + "]";
    }
}
