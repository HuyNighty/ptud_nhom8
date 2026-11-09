package com.quanlynhakhoa.service.hoadon;

import com.quanlynhakhoa.enums.TrangThaiHoaDon;
import com.quanlynhakhoa.model.hoadon.TongHopCongNoBenhNhan;
import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import java.util.*;

/** Projection of existing invoice data; no separate receivables storage or invented due dates. */
public final class CongNoService {
    private CongNoService(){ }
    public static List<ThongTinHoaDon> outstanding(List<ThongTinHoaDon> invoices){
        List<ThongTinHoaDon> result=new ArrayList<>();
        for(ThongTinHoaDon h:invoices)if(h.trangThai()!=TrangThaiHoaDon.DA_HUY&&h.conNo().signum()>0)result.add(h);
        return List.copyOf(result);
    }
    public static List<TongHopCongNoBenhNhan> byPatient(List<ThongTinHoaDon> invoices){
        Map<String,List<ThongTinHoaDon>> grouped=new LinkedHashMap<>();
        for(ThongTinHoaDon h:outstanding(invoices)){List<ThongTinHoaDon> rows=grouped.get(h.maBenhNhan());if(rows==null){rows=new ArrayList<>();grouped.put(h.maBenhNhan(),rows);}rows.add(h);}
        List<TongHopCongNoBenhNhan> result=new ArrayList<>();
        for(var entry:grouped.entrySet()){
            java.math.BigDecimal total=java.math.BigDecimal.ZERO,paid=java.math.BigDecimal.ZERO,debt=java.math.BigDecimal.ZERO;
            for(ThongTinHoaDon h:entry.getValue()){total=total.add(h.tongTien());paid=paid.add(h.daThu());debt=debt.add(h.conNo());}
            result.add(new TongHopCongNoBenhNhan(entry.getKey(),entry.getValue().getFirst().tenBenhNhan(),total,paid,debt,entry.getValue()));
        }
        return List.copyOf(result);
    }
}
