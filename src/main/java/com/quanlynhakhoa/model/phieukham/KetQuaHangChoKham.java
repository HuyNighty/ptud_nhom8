package com.quanlynhakhoa.model.phieukham;

import java.util.List;
import java.util.Objects;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class KetQuaHangChoKham {
    private final List<DongHangChoKham> danhSach;
    private final boolean biGioiHan;

    public KetQuaHangChoKham(List<DongHangChoKham> danhSach, boolean biGioiHan) {
        danhSach = List.copyOf(danhSach);
        this.danhSach = danhSach;
        this.biGioiHan = biGioiHan;
    }

    public List<DongHangChoKham> rows() { return danhSach; }
    public boolean truncated() { return biGioiHan; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof KetQuaHangChoKham value)) return false;
        return java.util.Objects.equals(danhSach, value.danhSach)
                && biGioiHan == value.biGioiHan;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(danhSach);
        result = 31 * result + Boolean.hashCode(biGioiHan);
        return result;
    }

    @Override
    public String toString() {
        return "Result[" + "rows=" + danhSach + ", " + "truncated=" + biGioiHan + "]";
    }
}
