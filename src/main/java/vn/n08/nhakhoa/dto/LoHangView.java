package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.LoHang;

/** Lô hàng kèm tên thuốc/vật tư, đơn vị, nhà cung cấp. */
public record LoHangView(LoHang lo, String tenHang, String donViTinh, String tenNhaCungCap, boolean kiemSoatDacBiet) {
    @Override public String toString() { return lo.maLo() + " · lô " + lo.soLo() + " · " + tenHang + " (tồn " + lo.soLuongTon() + ")"; }
}
