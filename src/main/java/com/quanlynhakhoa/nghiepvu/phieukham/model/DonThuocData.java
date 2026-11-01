package com.quanlynhakhoa.nghiepvu.phieukham.model;

import java.util.List;

public record DonThuocData(
        List<Thuoc> danhSachThuoc,
        List<ChiTietDonThuoc> chiTiet,
        String loiDan,
        boolean daKeDon) {

    public DonThuocData {
        danhSachThuoc = List.copyOf(danhSachThuoc);
        chiTiet = List.copyOf(chiTiet);
        loiDan = loiDan == null ? "" : loiDan;
    }
}
