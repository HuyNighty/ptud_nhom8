package com.quanlynhakhoa.billing.model;

import java.math.BigDecimal;
import java.util.List;

/** Stable row IDs are generated once per user confirmation and reused when checking an uncertain outcome. */
public record PaymentRequest(String invoiceId, BigDecimal amount, List<Part> parts, boolean transferConfirmed) {
    public PaymentRequest { parts = List.copyOf(parts); }
    public record Part(String id, BigDecimal amount, ThanhToan.PhuongThuc method,
                       String reference, BigDecimal customerTender) { }
}
