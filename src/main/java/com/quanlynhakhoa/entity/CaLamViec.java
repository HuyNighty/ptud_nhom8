package com.quanlynhakhoa.entity;

import java.time.LocalTime;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class CaLamViec {
    private final String ma;
    private final String ten;
    private final LocalTime batDau;
    private final LocalTime ketThuc;

    public CaLamViec(String ma, String ten, LocalTime batDau, LocalTime ketThuc) {
        this.ma = ma;
        this.ten = ten;
        this.batDau = batDau;
        this.ketThuc = ketThuc;
    }

    public String id() { return ma; }
    public String ten() { return ten; }
    public LocalTime batDau() { return batDau; }
    public LocalTime ketThuc() { return ketThuc; }

    @Override public String toString() { return ten + " (" + batDau + "–" + ketThuc + ")"; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CaLamViec value)) return false;
        return java.util.Objects.equals(ma, value.ma)
                && java.util.Objects.equals(ten, value.ten)
                && java.util.Objects.equals(batDau, value.batDau)
                && java.util.Objects.equals(ketThuc, value.ketThuc);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(ma);
        result = 31 * result + java.util.Objects.hashCode(ten);
        result = 31 * result + java.util.Objects.hashCode(batDau);
        result = 31 * result + java.util.Objects.hashCode(ketThuc);
        return result;
    }

}
