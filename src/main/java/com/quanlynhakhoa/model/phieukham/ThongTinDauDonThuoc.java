package com.quanlynhakhoa.model.phieukham;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class ThongTinDauDonThuoc {
    private final String maDonThuoc;
    private final String loiDan;

    public ThongTinDauDonThuoc(String maDonThuoc, String loiDan) {
        this.maDonThuoc = maDonThuoc;
        this.loiDan = loiDan;
    }

    public String maDonThuoc() { return maDonThuoc; }
    public String loiDan() { return loiDan; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThongTinDauDonThuoc value)) return false;
        return java.util.Objects.equals(maDonThuoc, value.maDonThuoc)
                && java.util.Objects.equals(loiDan, value.loiDan);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maDonThuoc);
        result = 31 * result + java.util.Objects.hashCode(loiDan);
        return result;
    }

    @Override
    public String toString() {
        return "DonThuocHeader[" + "maDonThuoc=" + maDonThuoc + ", " + "loiDan=" + loiDan + "]";
    }
}
