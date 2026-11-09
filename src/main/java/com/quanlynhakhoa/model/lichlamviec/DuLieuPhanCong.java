package com.quanlynhakhoa.model.lichlamviec;

import com.quanlynhakhoa.entity.CaLamViec;
import com.quanlynhakhoa.hethong.phanquyen.*;
import java.sql.*;
import java.util.*;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class DuLieuPhanCong {
    private final List<ThongTinPhanCong> rows;
    private final List<NhanVienChon> employees;
    private final List<CaLamViec> shifts;
    private final String message;

    public DuLieuPhanCong(List<ThongTinPhanCong> rows, List<NhanVienChon> employees, List<CaLamViec> shifts, String message) {
        rows=List.copyOf(rows);employees=List.copyOf(employees);shifts=List.copyOf(shifts);
        this.rows = rows;
        this.employees = employees;
        this.shifts = shifts;
        this.message = message;
    }

    public List<ThongTinPhanCong> rows() { return rows; }
    public List<NhanVienChon> employees() { return employees; }
    public List<CaLamViec> shifts() { return shifts; }
    public String message() { return message; }



    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DuLieuPhanCong value)) return false;
        return java.util.Objects.equals(rows, value.rows)
                && java.util.Objects.equals(employees, value.employees)
                && java.util.Objects.equals(shifts, value.shifts)
                && java.util.Objects.equals(message, value.message);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(rows);
        result = 31 * result + java.util.Objects.hashCode(employees);
        result = 31 * result + java.util.Objects.hashCode(shifts);
        result = 31 * result + java.util.Objects.hashCode(message);
        return result;
    }

    @Override
    public String toString() {
        return "DuLieu[" + "rows=" + rows + ", " + "employees=" + employees + ", " + "shifts=" + shifts + ", " + "message=" + message + "]";
    }
}
