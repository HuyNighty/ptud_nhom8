package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.ChiTietDonThuoc;
import vn.n08.nhakhoa.model.DonThuoc;

import java.util.List;

/** Đơn thuốc kèm các dòng thuốc. */
public record DonThuocView(DonThuoc don, String tenBacSi, List<Dong> dong) {
    /** Một dòng thuốc trong đơn. */
    public record Dong(ChiTietDonThuoc ct, String tenThuoc, String donViTinh, boolean kiemSoatDacBiet) {}
}
