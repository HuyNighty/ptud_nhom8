package com.quanlynhakhoa.quantri.lichlamviec.model;
public record NhanVienChon(String id, String ten) {
    @Override public String toString() { return id + " — " + ten; }
}
