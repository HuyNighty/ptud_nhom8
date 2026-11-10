package com.quanlynhakhoa.model.taikhoan;










import java.util.List;


/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class KetQuaDanhSachTaiKhoan {
    private final List<DongDanhSachTaiKhoan> danhSach;
    private final boolean biGioiHan;

    public KetQuaDanhSachTaiKhoan(List<DongDanhSachTaiKhoan> danhSach, boolean biGioiHan) {
        danhSach = List.copyOf(danhSach);
        this.danhSach = danhSach;
        this.biGioiHan = biGioiHan;
    }

    public List<DongDanhSachTaiKhoan> rows() { return danhSach; }
    public boolean truncated() { return biGioiHan; }



    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof KetQuaDanhSachTaiKhoan value)) return false;
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
