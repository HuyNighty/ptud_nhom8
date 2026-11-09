package com.quanlynhakhoa.model.hoadon;

import com.quanlynhakhoa.model.hoadon.ThongTinYeuCauHuyHoaDon;
import com.quanlynhakhoa.model.hoadon.DuLieuChiTietHoaDon;
import java.sql.*;
import java.util.*;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class ChiTietYeuCauHuy {
    private final ThongTinYeuCauHuyHoaDon yeuCau;
    private final DuLieuChiTietHoaDon hoaDon;

    public ChiTietYeuCauHuy(ThongTinYeuCauHuyHoaDon yeuCau, DuLieuChiTietHoaDon hoaDon) {
        this.yeuCau = yeuCau;
        this.hoaDon = hoaDon;
    }

    public ThongTinYeuCauHuyHoaDon request() { return yeuCau; }
    public DuLieuChiTietHoaDon invoice() { return hoaDon; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ChiTietYeuCauHuy value)) return false;
        return java.util.Objects.equals(yeuCau, value.yeuCau)
                && java.util.Objects.equals(hoaDon, value.hoaDon);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(yeuCau);
        result = 31 * result + java.util.Objects.hashCode(hoaDon);
        return result;
    }

    @Override
    public String toString() {
        return "Detail[" + "request=" + yeuCau + ", " + "invoice=" + hoaDon + "]";
    }
}
