package com.quanlynhakhoa.model.taikhoan;






import java.util.List;
import java.util.Objects;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class KetQuaNhanVienDuDieuKien {
    private final List<NhanVienDuDieuKien> employees;
    private final boolean truncated;

    public KetQuaNhanVienDuDieuKien(List<NhanVienDuDieuKien> employees, boolean truncated) {
        employees = List.copyOf(Objects.requireNonNull(employees, "employees"));
        this.employees = employees;
        this.truncated = truncated;
    }

    public List<NhanVienDuDieuKien> employees() { return employees; }
    public boolean truncated() { return truncated; }



    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof KetQuaNhanVienDuDieuKien value)) return false;
        return java.util.Objects.equals(employees, value.employees)
                && truncated == value.truncated;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(employees);
        result = 31 * result + Boolean.hashCode(truncated);
        return result;
    }

    @Override
    public String toString() {
        return "Result[" + "employees=" + employees + ", " + "truncated=" + truncated + "]";
    }
}
