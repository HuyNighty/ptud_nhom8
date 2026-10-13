package vn.n08.nhakhoa.model;

import java.math.BigDecimal;

/** Bảng DichVu — bảng giá dịch vụ. */
public record DichVu(
        String maDichVu,
        String tenDichVu,
        BigDecimal donGia,
        String moTa,
        boolean laCanLamSang,
        boolean dangApDung) {
    @Override public String toString() { return tenDichVu; }
}
