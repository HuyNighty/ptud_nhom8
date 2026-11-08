package com.quanlynhakhoa.nghiepvu.hoadon.entity;

import java.util.List;

public record InvoiceDetails(Invoice hoaDon, List<InvoiceLine> chiTiet, List<Payment> thanhToan,
                             List<InvoiceCancellationRequest> cancellationHistory,
                             List<PrescribedMedicine> prescribedMedicines) {
    public InvoiceDetails(Invoice hoaDon, List<InvoiceLine> chiTiet, List<Payment> thanhToan) {
        this(hoaDon, chiTiet, thanhToan, List.of());
    }
    public InvoiceDetails(Invoice hoaDon, List<InvoiceLine> chiTiet, List<Payment> thanhToan,
                          List<InvoiceCancellationRequest> cancellationHistory) {
        this(hoaDon, chiTiet, thanhToan, cancellationHistory, List.of());
    }
    public InvoiceDetails { chiTiet = List.copyOf(chiTiet); thanhToan = List.copyOf(thanhToan); cancellationHistory = List.copyOf(cancellationHistory); prescribedMedicines = List.copyOf(prescribedMedicines); }
}
