package com.quanlynhakhoa.model.nhanvien;









import java.util.List;


/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class KetQuaDanhSachNhanVien {
    private final List<DongDanhSachNhanVien> rows;
    private final boolean truncated;

    public KetQuaDanhSachNhanVien(List<DongDanhSachNhanVien> rows, boolean truncated) {
        rows = List.copyOf(rows);
        this.rows = rows;
        this.truncated = truncated;
    }

    public List<DongDanhSachNhanVien> rows() { return rows; }
    public boolean truncated() { return truncated; }



    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof KetQuaDanhSachNhanVien value)) return false;
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
