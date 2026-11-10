package com.quanlynhakhoa.model.kho;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class DongYeuCauCungUng {
    private final String loaiHang;
    private final String maHang;
    private final int soLuong;

    public DongYeuCauCungUng(String loaiHang, String maHang, int soLuong) {
        this.loaiHang = loaiHang;
        this.maHang = maHang;
        this.soLuong = soLuong;
    }

    public String itemType() { return loaiHang; }
    public String itemCode() { return maHang; }
    public int quantity() { return soLuong; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongYeuCauCungUng value)) return false;
        return java.util.Objects.equals(loaiHang, value.loaiHang)
                && java.util.Objects.equals(maHang, value.maHang)
                && soLuong == value.soLuong;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(loaiHang);
        result = 31 * result + java.util.Objects.hashCode(maHang);
        result = 31 * result + Integer.hashCode(soLuong);
        return result;
    }

    @Override
    public String toString() {
        return "RequestLine[" + "itemType=" + loaiHang + ", " + "itemCode=" + maHang + ", " + "quantity=" + soLuong + "]";
    }
}
