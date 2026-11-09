package com.quanlynhakhoa.model.hoadon;


import com.quanlynhakhoa.nghiepvu.hoadon.entity.*;
import com.quanlynhakhoa.nghiepvu.hoadon.service.*;
import com.quanlynhakhoa.nghiepvu.hoadon.ui.*;
import java.awt.event.*;
import java.util.*;


/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class DuLieuThanhToanDaTai {
    private final DuLieuChiTietHoaDon invoice;
    private final YeuCauThanhToan pending;
    private final boolean cancellation;
    private final String owner;

    public DuLieuThanhToanDaTai(DuLieuChiTietHoaDon invoice, YeuCauThanhToan pending, boolean cancellation, String owner) {
        this.invoice = invoice;
        this.pending = pending;
        this.cancellation = cancellation;
        this.owner = owner;
    }

    public DuLieuChiTietHoaDon invoice() { return invoice; }
    public YeuCauThanhToan pending() { return pending; }
    public boolean cancellation() { return cancellation; }
    public String owner() { return owner; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DuLieuThanhToanDaTai value)) return false;
        return java.util.Objects.equals(invoice, value.invoice)
                && java.util.Objects.equals(pending, value.pending)
                && cancellation == value.cancellation
                && java.util.Objects.equals(owner, value.owner);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(invoice);
        result = 31 * result + java.util.Objects.hashCode(pending);
        result = 31 * result + Boolean.hashCode(cancellation);
        result = 31 * result + java.util.Objects.hashCode(owner);
        return result;
    }

    @Override
    public String toString() {
        return "Loaded[" + "invoice=" + invoice + ", " + "pending=" + pending + ", " + "cancellation=" + cancellation + ", " + "owner=" + owner + "]";
    }
}
