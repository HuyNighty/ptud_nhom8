package com.quanlynhakhoa.model.hoadon;



import com.quanlynhakhoa.model.hoadon.DuLieuChiTietHoaDon;
import com.quanlynhakhoa.nghiepvu.hoadon.dao.*;
import com.quanlynhakhoa.nghiepvu.hoadon.entity.*;
import com.quanlynhakhoa.hethong.phanquyen.*;
import java.sql.*;
import java.util.*;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class XemTruocHuyHoaDon {
    private final DuLieuChiTietHoaDon invoice;
    private final boolean pending;

    public XemTruocHuyHoaDon(DuLieuChiTietHoaDon invoice, boolean pending) {
        this.invoice = invoice;
        this.pending = pending;
    }

    public DuLieuChiTietHoaDon invoice() { return invoice; }
    public boolean pending() { return pending; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof XemTruocHuyHoaDon value)) return false;
        return java.util.Objects.equals(invoice, value.invoice)
                && pending == value.pending;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(invoice);
        result = 31 * result + Boolean.hashCode(pending);
        return result;
    }

    @Override
    public String toString() {
        return "Preview[" + "invoice=" + invoice + ", " + "pending=" + pending + "]";
    }
}
