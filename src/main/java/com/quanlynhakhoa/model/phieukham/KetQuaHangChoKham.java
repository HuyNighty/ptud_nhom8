package com.quanlynhakhoa.model.phieukham;

import java.util.List;
import java.util.Objects;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class KetQuaHangChoKham {
    private final List<DongHangChoKham> rows;
    private final boolean truncated;

    public KetQuaHangChoKham(List<DongHangChoKham> rows, boolean truncated) {
        rows = List.copyOf(rows);
        this.rows = rows;
        this.truncated = truncated;
    }

    public List<DongHangChoKham> rows() { return rows; }
    public boolean truncated() { return truncated; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof KetQuaHangChoKham value)) return false;
        return java.util.Objects.equals(rows, value.rows)
                && truncated == value.truncated;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(rows);
        result = 31 * result + Boolean.hashCode(truncated);
        return result;
    }

    @Override
    public String toString() {
        return "Result[" + "rows=" + rows + ", " + "truncated=" + truncated + "]";
    }
}
