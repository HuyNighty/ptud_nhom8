package vn.n08.nhakhoa.model;

/** [Bổ sung] Dòng điều chỉnh theo lô. */
public record ChiTietDieuChinhKho(
        String maPhieu,
        String maLo,
        int soLuongHeThong,
        int soLuongThucTe) {
    public int chenhLech() { return soLuongThucTe - soLuongHeThong; }
}
