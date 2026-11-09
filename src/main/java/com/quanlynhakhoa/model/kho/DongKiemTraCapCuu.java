package com.quanlynhakhoa.model.kho;

import java.time.LocalDate;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class DongKiemTraCapCuu {
    private final String maMuc;
    private final int soLuongThucTe;
    private final LocalDate hanSuDung;

    public DongKiemTraCapCuu(String maMuc, int soLuongThucTe, LocalDate hanSuDung) {
        this.maMuc = maMuc;
        this.soLuongThucTe = soLuongThucTe;
        this.hanSuDung = hanSuDung;
    }

    public String itemId() { return maMuc; }
    public int actualQuantity() { return soLuongThucTe; }
    public LocalDate expiry() { return hanSuDung; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongKiemTraCapCuu value)) return false;
        return java.util.Objects.equals(maMuc, value.maMuc)
                && soLuongThucTe == value.soLuongThucTe
                && java.util.Objects.equals(hanSuDung, value.hanSuDung);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maMuc);
        result = 31 * result + Integer.hashCode(soLuongThucTe);
        result = 31 * result + java.util.Objects.hashCode(hanSuDung);
        return result;
    }

    @Override
    public String toString() {
        return "CheckLine[" + "itemId=" + maMuc + ", " + "actualQuantity=" + soLuongThucTe + ", " + "expiry=" + hanSuDung + "]";
    }
}
