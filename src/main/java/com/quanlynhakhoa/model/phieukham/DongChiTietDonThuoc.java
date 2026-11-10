package com.quanlynhakhoa.model.phieukham;

import com.quanlynhakhoa.entity.ChiTietDonThuoc;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class DongChiTietDonThuoc {
    private final ChiTietDonThuoc chiTietDonThuoc;
    private final String tenThuoc;
    private final String donViTinh;
    private final boolean kiemSoatDacBiet;
    private final String hoatChat;

    public DongChiTietDonThuoc(String maThuoc, String tenThuoc, String donViTinh, int soLuong, String lieuDung, String cachDung, int soNgayDung, boolean kiemSoatDacBiet, String hoatChat) {
        this(null, maThuoc, tenThuoc, donViTinh, soLuong, lieuDung, cachDung, soNgayDung, kiemSoatDacBiet, hoatChat);
    }

    public DongChiTietDonThuoc(String maDonThuoc, String maThuoc, String tenThuoc, String donViTinh, int soLuong, String lieuDung, String cachDung, int soNgayDung, boolean kiemSoatDacBiet, String hoatChat) {
        this.chiTietDonThuoc = new ChiTietDonThuoc(maDonThuoc, maThuoc, soLuong, lieuDung, cachDung, soNgayDung);
        this.tenThuoc = tenThuoc;
        this.donViTinh = donViTinh;

        this.kiemSoatDacBiet = kiemSoatDacBiet;
        this.hoatChat = hoatChat;
    }

    public String maThuoc() { return chiTietDonThuoc.getMaThuoc(); }
    public String tenThuoc() { return tenThuoc; }
    public String donViTinh() { return donViTinh; }
    public int soLuong() { return chiTietDonThuoc.getSoLuong(); }
    public String lieuDung() { return chiTietDonThuoc.getLieuDung(); }
    public String cachDung() { return chiTietDonThuoc.getCachDung(); }
    public int soNgayDung() { return chiTietDonThuoc.getSoNgayDung(); }
    public boolean kiemSoatDacBiet() { return kiemSoatDacBiet; }
    public String hoatChat() { return hoatChat; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongChiTietDonThuoc value)) return false;
        return java.util.Objects.equals(chiTietDonThuoc.getMaThuoc(), value.chiTietDonThuoc.getMaThuoc())
                && java.util.Objects.equals(tenThuoc, value.tenThuoc)
                && java.util.Objects.equals(donViTinh, value.donViTinh)
                && chiTietDonThuoc.getSoLuong() == value.chiTietDonThuoc.getSoLuong()
                && java.util.Objects.equals(chiTietDonThuoc.getLieuDung(), value.chiTietDonThuoc.getLieuDung())
                && java.util.Objects.equals(chiTietDonThuoc.getCachDung(), value.chiTietDonThuoc.getCachDung())
                && chiTietDonThuoc.getSoNgayDung() == value.chiTietDonThuoc.getSoNgayDung()
                && kiemSoatDacBiet == value.kiemSoatDacBiet
                && java.util.Objects.equals(hoatChat, value.hoatChat);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(chiTietDonThuoc.getMaThuoc());
        result = 31 * result + java.util.Objects.hashCode(tenThuoc);
        result = 31 * result + java.util.Objects.hashCode(donViTinh);
        result = 31 * result + Integer.hashCode(chiTietDonThuoc.getSoLuong());
        result = 31 * result + java.util.Objects.hashCode(chiTietDonThuoc.getLieuDung());
        result = 31 * result + java.util.Objects.hashCode(chiTietDonThuoc.getCachDung());
        result = 31 * result + Integer.hashCode(chiTietDonThuoc.getSoNgayDung());
        result = 31 * result + Boolean.hashCode(kiemSoatDacBiet);
        result = 31 * result + java.util.Objects.hashCode(hoatChat);
        return result;
    }

    @Override
    public String toString() {
        return "ChiTietDonThuoc[" + "maThuoc=" + chiTietDonThuoc.getMaThuoc() + ", " + "tenThuoc=" + tenThuoc + ", " + "donViTinh=" + donViTinh + ", " + "soLuong=" + chiTietDonThuoc.getSoLuong() + ", " + "lieuDung=" + chiTietDonThuoc.getLieuDung() + ", " + "cachDung=" + chiTietDonThuoc.getCachDung() + ", " + "soNgayDung=" + chiTietDonThuoc.getSoNgayDung() + ", " + "kiemSoatDacBiet=" + kiemSoatDacBiet + ", " + "hoatChat=" + hoatChat + "]";
    }

    public ChiTietDonThuoc getChiTietDonThuoc() { return chiTietDonThuoc; }
}
