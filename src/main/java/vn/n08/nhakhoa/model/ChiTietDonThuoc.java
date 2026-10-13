package vn.n08.nhakhoa.model;

/** Bảng ChiTietDonThuoc. */
public record ChiTietDonThuoc(
        String maDonThuoc,
        String maThuoc,
        int soLuong,
        String lieuDung,
        String cachDung,
        int soNgayDung) {}
