package com.quanlynhakhoa.model.hoadon;





import java.awt.event.*;
import java.util.*;


/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class DuLieuThanhToanDaTai {
    private final DuLieuChiTietHoaDon hoaDon;
    private final YeuCauThanhToan dangCho;
    private final boolean yeuCauHuy;
    private final String chuSoHuu;

    public DuLieuThanhToanDaTai(DuLieuChiTietHoaDon hoaDon, YeuCauThanhToan dangCho, boolean yeuCauHuy, String chuSoHuu) {
        this.hoaDon = hoaDon;
        this.dangCho = dangCho;
        this.yeuCauHuy = yeuCauHuy;
        this.chuSoHuu = chuSoHuu;
    }

    public DuLieuChiTietHoaDon invoice() { return hoaDon; }
    public YeuCauThanhToan pending() { return dangCho; }
    public boolean cancellation() { return yeuCauHuy; }
    public String owner() { return chuSoHuu; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DuLieuThanhToanDaTai value)) return false;
        return java.util.Objects.equals(hoaDon, value.hoaDon)
                && java.util.Objects.equals(dangCho, value.dangCho)
                && yeuCauHuy == value.yeuCauHuy
                && java.util.Objects.equals(chuSoHuu, value.chuSoHuu);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(hoaDon);
        result = 31 * result + java.util.Objects.hashCode(dangCho);
        result = 31 * result + Boolean.hashCode(yeuCauHuy);
        result = 31 * result + java.util.Objects.hashCode(chuSoHuu);
        return result;
    }

    @Override
    public String toString() {
        return "Loaded[" + "invoice=" + hoaDon + ", " + "pending=" + dangCho + ", " + "cancellation=" + yeuCauHuy + ", " + "owner=" + chuSoHuu + "]";
    }
}
