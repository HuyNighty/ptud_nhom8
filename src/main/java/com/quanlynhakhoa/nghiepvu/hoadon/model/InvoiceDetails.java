package com.quanlynhakhoa.nghiepvu.hoadon.model;

import java.util.List;

public record InvoiceDetails(Invoice hoaDon, List<InvoiceLine> chiTiet, List<Payment> thanhToan,
                             List<InvoiceCancellationRequest> cancellationHistory) {
    public InvoiceDetails(Invoice hoaDon, List<InvoiceLine> chiTiet, List<Payment> thanhToan) {
        this(hoaDon, chiTiet, thanhToan, List.of());
    }
    public InvoiceDetails { chiTiet = List.copyOf(chiTiet); thanhToan = List.copyOf(thanhToan); cancellationHistory = List.copyOf(cancellationHistory); }
}
