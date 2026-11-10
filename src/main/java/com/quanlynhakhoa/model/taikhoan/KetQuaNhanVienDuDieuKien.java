package com.quanlynhakhoa.model.taikhoan;






import java.util.List;
import java.util.Objects;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class KetQuaNhanVienDuDieuKien {
    private final List<NhanVienDuDieuKien> nhanVien;
    private final boolean biGioiHan;

    public KetQuaNhanVienDuDieuKien(List<NhanVienDuDieuKien> nhanVien, boolean biGioiHan) {
        nhanVien = List.copyOf(Objects.requireNonNull(nhanVien, "employees"));
        this.nhanVien = nhanVien;
        this.biGioiHan = biGioiHan;
    }

    public List<NhanVienDuDieuKien> employees() { return nhanVien; }
    public boolean truncated() { return biGioiHan; }



    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof KetQuaNhanVienDuDieuKien value)) return false;
        return java.util.Objects.equals(nhanVien, value.nhanVien)
                && biGioiHan == value.biGioiHan;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(nhanVien);
        result = 31 * result + Boolean.hashCode(biGioiHan);
        return result;
    }

    @Override
    public String toString() {
        return "Result[" + "employees=" + nhanVien + ", " + "truncated=" + biGioiHan + "]";
    }
}
