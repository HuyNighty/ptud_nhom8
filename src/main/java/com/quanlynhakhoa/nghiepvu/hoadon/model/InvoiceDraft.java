package com.quanlynhakhoa.nghiepvu.hoadon.model;

import java.math.BigDecimal;
import java.util.List;

/** In-memory review only. Never persisted as a NHAP status. */
public record InvoiceDraft(String patientId, String visitId, String revision, List<Line> lines) {
    public InvoiceDraft { lines = List.copyOf(lines); }
    public record Line(String serviceId, String serviceName, int quantity, BigDecimal unitPrice, boolean medicine) {
        public Line(String serviceId, String serviceName, int quantity, BigDecimal unitPrice) {
            this(serviceId,serviceName,quantity,unitPrice,false);
        }
        public BigDecimal amount() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
    }
    public BigDecimal total() {
        BigDecimal total = BigDecimal.ZERO;
        for (Line line : lines) total = total.add(line.amount());
        return total;
    }
}
