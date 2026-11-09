package com.quanlynhakhoa.model.phieukham;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class DongChiTietDonThuoc {
    private final String maThuoc;
    private final String tenThuoc;
    private final String donViTinh;
    private final int soLuong;
    private final String lieuDung;
    private final String cachDung;
    private final int soNgayDung;
    private final boolean kiemSoatDacBiet;
    private final String hoatChat;

    public DongChiTietDonThuoc(String maThuoc, String tenThuoc, String donViTinh, int soLuong, String lieuDung, String cachDung, int soNgayDung, boolean kiemSoatDacBiet, String hoatChat) {
        this.maThuoc = maThuoc;
        this.tenThuoc = tenThuoc;
        this.donViTinh = donViTinh;
        this.soLuong = soLuong;
        this.lieuDung = lieuDung;
        this.cachDung = cachDung;
        this.soNgayDung = soNgayDung;
        this.kiemSoatDacBiet = kiemSoatDacBiet;
        this.hoatChat = hoatChat;
    }

    public String maThuoc() { return maThuoc; }
    public String tenThuoc() { return tenThuoc; }
    public String donViTinh() { return donViTinh; }
    public int soLuong() { return soLuong; }
    public String lieuDung() { return lieuDung; }
    public String cachDung() { return cachDung; }
    public int soNgayDung() { return soNgayDung; }
    public boolean kiemSoatDacBiet() { return kiemSoatDacBiet; }
    public String hoatChat() { return hoatChat; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongChiTietDonThuoc value)) return false;
        return java.util.Objects.equals(maThuoc, value.maThuoc)
                && java.util.Objects.equals(tenThuoc, value.tenThuoc)
                && java.util.Objects.equals(donViTinh, value.donViTinh)
                && soLuong == value.soLuong
                && java.util.Objects.equals(lieuDung, value.lieuDung)
                && java.util.Objects.equals(cachDung, value.cachDung)
                && soNgayDung == value.soNgayDung
                && kiemSoatDacBiet == value.kiemSoatDacBiet
                && java.util.Objects.equals(hoatChat, value.hoatChat);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maThuoc);
        result = 31 * result + java.util.Objects.hashCode(tenThuoc);
        result = 31 * result + java.util.Objects.hashCode(donViTinh);
        result = 31 * result + Integer.hashCode(soLuong);
        result = 31 * result + java.util.Objects.hashCode(lieuDung);
        result = 31 * result + java.util.Objects.hashCode(cachDung);
        result = 31 * result + Integer.hashCode(soNgayDung);
        result = 31 * result + Boolean.hashCode(kiemSoatDacBiet);
        result = 31 * result + java.util.Objects.hashCode(hoatChat);
        return result;
    }

    @Override
    public String toString() {
        return "ChiTietDonThuoc[" + "maThuoc=" + maThuoc + ", " + "tenThuoc=" + tenThuoc + ", " + "donViTinh=" + donViTinh + ", " + "soLuong=" + soLuong + ", " + "lieuDung=" + lieuDung + ", " + "cachDung=" + cachDung + ", " + "soNgayDung=" + soNgayDung + ", " + "kiemSoatDacBiet=" + kiemSoatDacBiet + ", " + "hoatChat=" + hoatChat + "]";
    }
}
