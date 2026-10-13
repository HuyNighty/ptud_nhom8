package vn.n08.nhakhoa.model;

/** Bảng Thuoc. */
public record Thuoc(
        String maThuoc,
        String tenThuoc,
        String donViTinh,
        boolean kiemSoatDacBiet,
        String hamLuong,
        String hoatChat,
        int dinhMucToiThieu,
        boolean daDatHang) {
    @Override public String toString() { return tenThuoc + (hamLuong == null ? "" : " " + hamLuong); }
}
