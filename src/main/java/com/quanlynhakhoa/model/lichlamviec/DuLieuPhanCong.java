package com.quanlynhakhoa.model.lichlamviec;

import com.quanlynhakhoa.entity.CaLamViec;
import java.sql.*;
import java.util.*;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class DuLieuPhanCong {
    private final List<ThongTinPhanCong> danhSach;
    private final List<NhanVienChon> nhanVien;
    private final List<CaLamViec> caLamViec;
    private final String thongBao;

    public DuLieuPhanCong(List<ThongTinPhanCong> danhSach, List<NhanVienChon> nhanVien, List<CaLamViec> caLamViec, String thongBao) {
        danhSach=List.copyOf(danhSach);nhanVien=List.copyOf(nhanVien);caLamViec=List.copyOf(caLamViec);
        this.danhSach = danhSach;
        this.nhanVien = nhanVien;
        this.caLamViec = caLamViec;
        this.thongBao = thongBao;
    }

    public List<ThongTinPhanCong> rows() { return danhSach; }
    public List<NhanVienChon> employees() { return nhanVien; }
    public List<CaLamViec> shifts() { return caLamViec; }
    public String message() { return thongBao; }



    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DuLieuPhanCong value)) return false;
        return java.util.Objects.equals(danhSach, value.danhSach)
                && java.util.Objects.equals(nhanVien, value.nhanVien)
                && java.util.Objects.equals(caLamViec, value.caLamViec)
                && java.util.Objects.equals(thongBao, value.thongBao);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(danhSach);
        result = 31 * result + java.util.Objects.hashCode(nhanVien);
        result = 31 * result + java.util.Objects.hashCode(caLamViec);
        result = 31 * result + java.util.Objects.hashCode(thongBao);
        return result;
    }

    @Override
    public String toString() {
        return "DuLieu[" + "rows=" + danhSach + ", " + "employees=" + nhanVien + ", " + "shifts=" + caLamViec + ", " + "message=" + thongBao + "]";
    }
}
