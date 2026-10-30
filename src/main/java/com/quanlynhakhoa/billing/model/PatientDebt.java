package com.quanlynhakhoa.billing.model;

import java.math.BigDecimal;
import java.util.List;

public record PatientDebt(String patientId,String name,BigDecimal total,BigDecimal collected,BigDecimal debt,List<HoaDon> invoices) {
    public PatientDebt{invoices=List.copyOf(invoices);}
}
