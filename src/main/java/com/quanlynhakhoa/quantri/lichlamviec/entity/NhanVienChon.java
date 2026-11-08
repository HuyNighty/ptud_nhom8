package com.quanlynhakhoa.quantri.lichlamviec.entity;
public record NhanVienChon(String id, String ten) {
    @Override public String toString() { return id + " — " + ten; }
}
