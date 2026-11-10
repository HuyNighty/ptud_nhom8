package com.quanlynhakhoa.model.phieukham;

import java.util.List;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class DuLieuCanLamSang {
    private final List<DichVuCanLamSangChon> dichVu;
    private final List<ThongTinChiDinhCanLamSang> chiDinh;

    public DuLieuCanLamSang(List<DichVuCanLamSangChon> dichVu, List<ThongTinChiDinhCanLamSang> chiDinh) {
        dichVu = List.copyOf(dichVu);
        chiDinh = List.copyOf(chiDinh);
        this.dichVu = dichVu;
        this.chiDinh = chiDinh;
    }

    public List<DichVuCanLamSangChon> dichVu() { return dichVu; }
    public List<ThongTinChiDinhCanLamSang> chiDinh() { return chiDinh; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DuLieuCanLamSang value)) return false;
        return java.util.Objects.equals(dichVu, value.dichVu)
                && java.util.Objects.equals(chiDinh, value.chiDinh);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(dichVu);
        result = 31 * result + java.util.Objects.hashCode(chiDinh);
        return result;
    }

    @Override
    public String toString() {
        return "DuLieuCanLamSang[" + "dichVu=" + dichVu + ", " + "chiDinh=" + chiDinh + "]";
    }
}
