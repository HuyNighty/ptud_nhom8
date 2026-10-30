package com.quanlynhakhoa.billing.model;

import java.math.BigDecimal;
import java.util.List;

/** In-memory review only. Never persisted as a NHAP status. */
public record InvoiceDraft(String patientId, String visitId, String revision, List<Line> lines) {
    public InvoiceDraft { lines = List.copyOf(lines); }
    public record Line(String serviceId, String serviceName, int quantity, BigDecimal unitPrice) {
        public BigDecimal amount() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
    }
    public BigDecimal total() {
        BigDecimal total = BigDecimal.ZERO;
        for (Line line : lines) total = total.add(line.amount());
        return total;
    }
}
