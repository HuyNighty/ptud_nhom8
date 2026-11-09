package com.quanlynhakhoa.model.phieukham;

import java.util.Objects;

/** Dữ liệu lựa chọn thuốc để kê đơn, tách biệt với danh mục thuốc Kho. */
public final class ThuocChonKeDon {
    private final String maThuoc;
    private final String tenThuoc;
    private final String donViTinh;
    private final boolean kiemSoatDacBiet;
    private final String hamLuong;
    private final String hoatChat;

    public ThuocChonKeDon(String maThuoc, String tenThuoc, String donViTinh,
                          boolean kiemSoatDacBiet, String hamLuong, String hoatChat) {
        this.maThuoc = maThuoc;
        this.tenThuoc = tenThuoc;
        this.donViTinh = donViTinh;
        this.kiemSoatDacBiet = kiemSoatDacBiet;
        this.hamLuong = hamLuong;
        this.hoatChat = hoatChat;
    }

    public String maThuoc() { return maThuoc; }
    public String tenThuoc() { return tenThuoc; }
    public String donViTinh() { return donViTinh; }
    public boolean kiemSoatDacBiet() { return kiemSoatDacBiet; }
    public String hamLuong() { return hamLuong; }
    public String hoatChat() { return hoatChat; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThuocChonKeDon thuoc)) return false;
        return kiemSoatDacBiet == thuoc.kiemSoatDacBiet
                && Objects.equals(maThuoc, thuoc.maThuoc)
                && Objects.equals(tenThuoc, thuoc.tenThuoc)
                && Objects.equals(donViTinh, thuoc.donViTinh)
                && Objects.equals(hamLuong, thuoc.hamLuong)
                && Objects.equals(hoatChat, thuoc.hoatChat);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(maThuoc);
        result = 31 * result + Objects.hashCode(tenThuoc);
        result = 31 * result + Objects.hashCode(donViTinh);
        result = 31 * result + Boolean.hashCode(kiemSoatDacBiet);
        result = 31 * result + Objects.hashCode(hamLuong);
        return 31 * result + Objects.hashCode(hoatChat);
    }

    @Override
    public String toString() {
        String strength = hamLuong == null || hamLuong.isBlank()
                ? "" : " (" + hamLuong.trim() + ")";
        return tenThuoc + strength + " - " + donViTinh;
    }
}
