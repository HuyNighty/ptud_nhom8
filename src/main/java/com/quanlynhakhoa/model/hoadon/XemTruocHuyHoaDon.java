package com.quanlynhakhoa.model.hoadon;



import com.quanlynhakhoa.model.hoadon.DuLieuChiTietHoaDon;
import java.sql.*;
import java.util.*;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class XemTruocHuyHoaDon {
    private final DuLieuChiTietHoaDon hoaDon;
    private final boolean dangCho;

    public XemTruocHuyHoaDon(DuLieuChiTietHoaDon hoaDon, boolean dangCho) {
        this.hoaDon = hoaDon;
        this.dangCho = dangCho;
    }

    public DuLieuChiTietHoaDon invoice() { return hoaDon; }
    public boolean pending() { return dangCho; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof XemTruocHuyHoaDon value)) return false;
        return java.util.Objects.equals(hoaDon, value.hoaDon)
                && dangCho == value.dangCho;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(hoaDon);
        result = 31 * result + Boolean.hashCode(dangCho);
        return result;
    }

    @Override
    public String toString() {
        return "Preview[" + "invoice=" + hoaDon + ", " + "pending=" + dangCho + "]";
    }
}
