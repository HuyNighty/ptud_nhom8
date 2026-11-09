package com.quanlynhakhoa.model.nhatky;











import java.util.List;


/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class KetQuaNhatKy {
    private final List<BanGhiNhatKy> rows;
    private final boolean truncated;

    public KetQuaNhatKy(List<BanGhiNhatKy> rows, boolean truncated) {
        rows = List.copyOf(rows);
        this.rows = rows;
        this.truncated = truncated;
    }

    public List<BanGhiNhatKy> rows() { return rows; }
    public boolean truncated() { return truncated; }



    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof KetQuaNhatKy value)) return false;
        return java.util.Objects.equals(rows, value.rows)
                && truncated == value.truncated;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(this.rows);
        result = 31 * result + Boolean.hashCode(this.truncated);
        return result;
    }

    @Override
    public String toString() {
        return "Result[" + "rows=" + rows + ", " + "truncated=" + truncated + "]";
    }
}
