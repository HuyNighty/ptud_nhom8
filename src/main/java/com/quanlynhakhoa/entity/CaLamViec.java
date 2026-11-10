package com.quanlynhakhoa.entity;

import java.time.LocalTime;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class CaLamViec {
    private final String id;
    private final String ten;
    private final LocalTime batDau;
    private final LocalTime ketThuc;

    public CaLamViec(String id, String ten, LocalTime batDau, LocalTime ketThuc) {
        this.id = id;
        this.ten = ten;
        this.batDau = batDau;
        this.ketThuc = ketThuc;
    }

    public String id() { return id; }
    public String ten() { return ten; }
    public LocalTime batDau() { return batDau; }
    public LocalTime ketThuc() { return ketThuc; }

    @Override public String toString() { return ten + " (" + batDau + "–" + ketThuc + ")"; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CaLamViec value)) return false;
        return java.util.Objects.equals(id, value.id)
                && java.util.Objects.equals(ten, value.ten)
                && java.util.Objects.equals(batDau, value.batDau)
                && java.util.Objects.equals(ketThuc, value.ketThuc);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(id);
        result = 31 * result + java.util.Objects.hashCode(ten);
        result = 31 * result + java.util.Objects.hashCode(batDau);
        result = 31 * result + java.util.Objects.hashCode(ketThuc);
        return result;
    }

}
