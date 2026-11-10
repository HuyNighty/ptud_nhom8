package com.quanlynhakhoa.nghiepvu.hoadon.service;

import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import com.quanlynhakhoa.nghiepvu.hoadon.entity.*;
import java.util.*;

/** Projection of existing invoice data; no separate receivables storage or invented due dates. */
public final class DebtService {
    private DebtService(){ }
    public static List<ThongTinHoaDon> outstanding(List<ThongTinHoaDon> invoices){
        List<ThongTinHoaDon> result=new ArrayList<>();
        for(ThongTinHoaDon h:invoices)if(h.trangThai()!=InvoiceStatus.DA_HUY&&h.conNo().signum()>0)result.add(h);
        return List.copyOf(result);
    }
    public static List<PatientDebt> byPatient(List<ThongTinHoaDon> invoices){
        Map<String,List<ThongTinHoaDon>> grouped=new LinkedHashMap<>();
        for(ThongTinHoaDon h:outstanding(invoices)){List<ThongTinHoaDon> rows=grouped.get(h.maBenhNhan());if(rows==null){rows=new ArrayList<>();grouped.put(h.maBenhNhan(),rows);}rows.add(h);}
        List<PatientDebt> result=new ArrayList<>();
        for(var entry:grouped.entrySet()){
            java.math.BigDecimal total=java.math.BigDecimal.ZERO,paid=java.math.BigDecimal.ZERO,debt=java.math.BigDecimal.ZERO;
            for(ThongTinHoaDon h:entry.getValue()){total=total.add(h.tongTien());paid=paid.add(h.daThu());debt=debt.add(h.conNo());}
            result.add(new PatientDebt(entry.getKey(),entry.getValue().getFirst().tenBenhNhan(),total,paid,debt,entry.getValue()));
        }
        return List.copyOf(result);
    }
}
