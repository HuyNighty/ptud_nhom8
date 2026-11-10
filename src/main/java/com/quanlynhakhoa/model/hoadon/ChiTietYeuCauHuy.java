package com.quanlynhakhoa.model.hoadon;

import com.quanlynhakhoa.model.hoadon.ThongTinYeuCauHuyHoaDon;
import com.quanlynhakhoa.model.hoadon.DuLieuChiTietHoaDon;
import com.quanlynhakhoa.nghiepvu.hoadon.dao.*;
import com.quanlynhakhoa.hethong.phanquyen.*;
import java.sql.*;
import java.util.*;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class ChiTietYeuCauHuy {
    private final ThongTinYeuCauHuyHoaDon request;
    private final DuLieuChiTietHoaDon invoice;

    public ChiTietYeuCauHuy(ThongTinYeuCauHuyHoaDon request, DuLieuChiTietHoaDon invoice) {
        this.request = request;
        this.invoice = invoice;
    }

    public ThongTinYeuCauHuyHoaDon request() { return request; }
    public DuLieuChiTietHoaDon invoice() { return invoice; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ChiTietYeuCauHuy value)) return false;
        return java.util.Objects.equals(request, value.request)
                && java.util.Objects.equals(invoice, value.invoice);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(request);
        result = 31 * result + java.util.Objects.hashCode(invoice);
        return result;
    }

    @Override
    public String toString() {
        return "Detail[" + "request=" + request + ", " + "invoice=" + invoice + "]";
    }
}
