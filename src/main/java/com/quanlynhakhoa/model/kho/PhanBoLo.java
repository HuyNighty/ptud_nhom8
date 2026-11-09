package com.quanlynhakhoa.model.kho;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class PhanBoLo {
    private final int lineNo;
    private final String lotCode;

    public PhanBoLo(int lineNo, String lotCode) {
        this.lineNo = lineNo;
        this.lotCode = lotCode;
    }

    public int lineNo() { return lineNo; }
    public String lotCode() { return lotCode; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PhanBoLo value)) return false;
        return lineNo == value.lineNo
                && java.util.Objects.equals(lotCode, value.lotCode);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Integer.hashCode(lineNo);
        result = 31 * result + java.util.Objects.hashCode(lotCode);
        return result;
    }

    @Override
    public String toString() {
        return "Allocation[" + "lineNo=" + lineNo + ", " + "lotCode=" + lotCode + "]";
    }
}
