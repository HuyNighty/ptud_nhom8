package com.quanlynhakhoa.model.taikhoan;










import java.util.List;


/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class KetQuaDanhSachTaiKhoan {
    private final List<DongDanhSachTaiKhoan> rows;
    private final boolean truncated;

    public KetQuaDanhSachTaiKhoan(List<DongDanhSachTaiKhoan> rows, boolean truncated) {
        rows = List.copyOf(rows);
        this.rows = rows;
        this.truncated = truncated;
    }

    public List<DongDanhSachTaiKhoan> rows() { return rows; }
    public boolean truncated() { return truncated; }



    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof KetQuaDanhSachTaiKhoan value)) return false;
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
