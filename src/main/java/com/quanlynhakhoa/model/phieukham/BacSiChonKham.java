package com.quanlynhakhoa.model.phieukham;

import java.util.Objects;

/** Dữ liệu lựa chọn bác sĩ trong Phiếu khám. */
public final class BacSiChonKham {
    private final String maNhanVien;
    private final String hoTen;

    public BacSiChonKham(String maNhanVien, String hoTen) {
        this.maNhanVien = maNhanVien == null ? "" : maNhanVien.trim();
        this.hoTen = hoTen == null ? "" : hoTen.trim();
    }

    public String maNhanVien() { return maNhanVien; }
    public String hoTen() { return hoTen; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BacSiChonKham bacSi)) return false;
        return Objects.equals(maNhanVien, bacSi.maNhanVien)
                && Objects.equals(hoTen, bacSi.hoTen);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hashCode(maNhanVien) + Objects.hashCode(hoTen);
    }

    @Override
    public String toString() {
        if (maNhanVien.isBlank()) return hoTen;
        return maNhanVien + " - BS. " + hoTen;
    }
}
