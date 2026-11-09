package com.quanlynhakhoa.model.hoadon;

import java.math.BigDecimal;


/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class DongBanNhapHoaDon {
    private final String maDichVu;
    private final String tenDichVu;
    private final int soLuong;
    private final BigDecimal donGia;
    private final boolean laThuoc;

    public DongBanNhapHoaDon(String maDichVu, String tenDichVu, int soLuong, BigDecimal donGia, boolean laThuoc) {
        this.maDichVu = maDichVu;
        this.tenDichVu = tenDichVu;
        this.soLuong = soLuong;
        this.donGia = donGia;
        this.laThuoc = laThuoc;
    }

    public String serviceId() { return maDichVu; }
    public String serviceName() { return tenDichVu; }
    public int quantity() { return soLuong; }
    public BigDecimal unitPrice() { return donGia; }
    public boolean medicine() { return laThuoc; }

        public DongBanNhapHoaDon(String maDichVu, String tenDichVu, int soLuong, BigDecimal donGia) {
            this(maDichVu,tenDichVu,soLuong,donGia,false);
        }
        public BigDecimal amount() { return donGia.multiply(BigDecimal.valueOf(soLuong)); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongBanNhapHoaDon value)) return false;
        return java.util.Objects.equals(maDichVu, value.maDichVu)
                && java.util.Objects.equals(tenDichVu, value.tenDichVu)
                && soLuong == value.soLuong
                && java.util.Objects.equals(donGia, value.donGia)
                && laThuoc == value.laThuoc;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maDichVu);
        result = 31 * result + java.util.Objects.hashCode(tenDichVu);
        result = 31 * result + Integer.hashCode(soLuong);
        result = 31 * result + java.util.Objects.hashCode(donGia);
        result = 31 * result + Boolean.hashCode(laThuoc);
        return result;
    }

    @Override
    public String toString() {
        return "Line[" + "serviceId=" + maDichVu + ", " + "serviceName=" + tenDichVu + ", " + "quantity=" + soLuong + ", " + "unitPrice=" + donGia + ", " + "medicine=" + laThuoc + "]";
    }
}
