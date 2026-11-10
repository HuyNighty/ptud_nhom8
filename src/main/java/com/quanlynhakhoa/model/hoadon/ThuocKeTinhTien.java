package com.quanlynhakhoa.model.hoadon;

import java.math.BigDecimal;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class ThuocKeTinhTien {
    private final String maThuoc;
    private final String ten;
    private final String donViTinh;
    private final int soLuong;
    private final BigDecimal donGia;

    public ThuocKeTinhTien(String maThuoc, String ten, String donViTinh, int soLuong, BigDecimal donGia) {
        this.maThuoc = maThuoc;
        this.ten = ten;
        this.donViTinh = donViTinh;
        this.soLuong = soLuong;
        this.donGia = donGia;
    }

    public String medicineId() { return maThuoc; }
    public String name() { return ten; }
    public String unit() { return donViTinh; }
    public int quantity() { return soLuong; }
    public BigDecimal unitPrice() { return donGia; }

    public BigDecimal total() {
        return donGia == null ? null : donGia.multiply(BigDecimal.valueOf(soLuong));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThuocKeTinhTien value)) return false;
        return java.util.Objects.equals(maThuoc, value.maThuoc)
                && java.util.Objects.equals(ten, value.ten)
                && java.util.Objects.equals(donViTinh, value.donViTinh)
                && soLuong == value.soLuong
                && java.util.Objects.equals(donGia, value.donGia);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maThuoc);
        result = 31 * result + java.util.Objects.hashCode(ten);
        result = 31 * result + java.util.Objects.hashCode(donViTinh);
        result = 31 * result + Integer.hashCode(soLuong);
        result = 31 * result + java.util.Objects.hashCode(donGia);
        return result;
    }

    @Override
    public String toString() {
        return "PrescribedMedicine[" + "medicineId=" + maThuoc + ", " + "name=" + ten + ", " + "unit=" + donViTinh + ", " + "quantity=" + soLuong + ", " + "unitPrice=" + donGia + "]";
    }
}
