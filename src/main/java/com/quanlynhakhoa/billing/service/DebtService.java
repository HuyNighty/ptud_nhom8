package com.quanlynhakhoa.billing.service;

import com.quanlynhakhoa.billing.model.*;
import java.util.*;

/** Projection of existing invoice data; no separate receivables storage or invented due dates. */
public final class DebtService {
    private DebtService(){ }
    public static List<HoaDon> outstanding(List<HoaDon> invoices){
        List<HoaDon> result=new ArrayList<>();
        for(HoaDon h:invoices)if(h.trangThai()!=TrangThaiHoaDon.DA_HUY&&h.conNo().signum()>0)result.add(h);
        return List.copyOf(result);
    }
}
