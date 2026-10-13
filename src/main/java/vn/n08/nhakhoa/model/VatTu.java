package vn.n08.nhakhoa.model;

/** Bảng VatTu. */
public record VatTu(
        String maVatTu,
        String tenVatTu,
        String donViTinh,
        String moTa,
        int dinhMucToiThieu,
        boolean daDatHang) {
    @Override public String toString() { return tenVatTu; }
}
