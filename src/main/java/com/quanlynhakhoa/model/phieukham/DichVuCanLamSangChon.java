package com.quanlynhakhoa.model.phieukham;

import java.math.BigDecimal;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class DichVuCanLamSangChon {
    private final String maDichVu;
    private final String tenDichVu;
    private final BigDecimal donGia;
    private final String moTa;

    public DichVuCanLamSangChon(String maDichVu, String tenDichVu, BigDecimal donGia, String moTa) {
        this.maDichVu = maDichVu;
        this.tenDichVu = tenDichVu;
        this.donGia = donGia;
        this.moTa = moTa;
    }

    public String maDichVu() { return maDichVu; }
    public String tenDichVu() { return tenDichVu; }
    public BigDecimal donGia() { return donGia; }
    public String moTa() { return moTa; }

    @Override
    public String toString() {
        return maDichVu + " - " + tenDichVu;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DichVuCanLamSangChon value)) return false;
        return java.util.Objects.equals(maDichVu, value.maDichVu)
                && java.util.Objects.equals(tenDichVu, value.tenDichVu)
                && java.util.Objects.equals(donGia, value.donGia)
                && java.util.Objects.equals(moTa, value.moTa);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maDichVu);
        result = 31 * result + java.util.Objects.hashCode(tenDichVu);
        result = 31 * result + java.util.Objects.hashCode(donGia);
        result = 31 * result + java.util.Objects.hashCode(moTa);
        return result;
    }

}
