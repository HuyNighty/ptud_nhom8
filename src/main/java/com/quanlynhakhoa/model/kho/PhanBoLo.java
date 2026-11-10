package com.quanlynhakhoa.model.kho;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class PhanBoLo {
    private final int soDong;
    private final String maLo;

    public PhanBoLo(int soDong, String maLo) {
        this.soDong = soDong;
        this.maLo = maLo;
    }

    public int lineNo() { return soDong; }
    public String lotCode() { return maLo; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PhanBoLo value)) return false;
        return soDong == value.soDong
                && java.util.Objects.equals(maLo, value.maLo);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Integer.hashCode(soDong);
        result = 31 * result + java.util.Objects.hashCode(maLo);
        return result;
    }

    @Override
    public String toString() {
        return "Allocation[" + "lineNo=" + soDong + ", " + "lotCode=" + maLo + "]";
    }
}
