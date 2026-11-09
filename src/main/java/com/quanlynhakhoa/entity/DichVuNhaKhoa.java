package com.quanlynhakhoa.entity;

import java.math.BigDecimal;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class DichVuNhaKhoa {
    private final String maDichVu;
    private final String tenDichVu;
    private final BigDecimal donGia;
    private final String moTa;
    private final boolean laCanLamSang;
    private final boolean dangApDung;

    public DichVuNhaKhoa(String maDichVu, String tenDichVu, BigDecimal donGia, String moTa, boolean laCanLamSang, boolean dangApDung) {
        this.maDichVu = maDichVu;
        this.tenDichVu = tenDichVu;
        this.donGia = donGia;
        this.moTa = moTa;
        this.laCanLamSang = laCanLamSang;
        this.dangApDung = dangApDung;
    }

    public String maDichVu() { return maDichVu; }
    public String tenDichVu() { return tenDichVu; }
    public BigDecimal donGia() { return donGia; }
    public String moTa() { return moTa; }
    public boolean laCanLamSang() { return laCanLamSang; }
    public boolean dangApDung() { return dangApDung; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DichVuNhaKhoa value)) return false;
        return java.util.Objects.equals(maDichVu, value.maDichVu)
                && java.util.Objects.equals(tenDichVu, value.tenDichVu)
                && java.util.Objects.equals(donGia, value.donGia)
                && java.util.Objects.equals(moTa, value.moTa)
                && laCanLamSang == value.laCanLamSang
                && dangApDung == value.dangApDung;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maDichVu);
        result = 31 * result + java.util.Objects.hashCode(tenDichVu);
        result = 31 * result + java.util.Objects.hashCode(donGia);
        result = 31 * result + java.util.Objects.hashCode(moTa);
        result = 31 * result + Boolean.hashCode(laCanLamSang);
        result = 31 * result + Boolean.hashCode(dangApDung);
        return result;
    }

    @Override
    public String toString() {
        return "DentalService[" + "maDichVu=" + maDichVu + ", " + "tenDichVu=" + tenDichVu + ", " + "donGia=" + donGia + ", " + "moTa=" + moTa + ", " + "laCanLamSang=" + laCanLamSang + ", " + "dangApDung=" + dangApDung + "]";
    }
}
